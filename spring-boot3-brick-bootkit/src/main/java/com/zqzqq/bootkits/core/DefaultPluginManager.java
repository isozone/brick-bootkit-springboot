/**
 * Copyright [2019-Present] [starBlues]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.zqzqq.bootkits.core;

import com.zqzqq.bootkits.core.checker.ComposePluginLauncherChecker;
import com.zqzqq.bootkits.core.checker.DefaultPluginLauncherChecker;
import com.zqzqq.bootkits.core.checker.DependencyPluginLauncherChecker;
import com.zqzqq.bootkits.core.checker.PluginBasicChecker;
import com.zqzqq.bootkits.core.descriptor.DefaultInsidePluginDescriptor;
import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.core.descriptor.PluginDescriptor;
import com.zqzqq.bootkits.core.descriptor.PluginDescriptorLoader;
import com.zqzqq.bootkits.core.exception.PluginDisabledException;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.core.lock.ClusterLock;
import com.zqzqq.bootkits.core.lock.ClusterLockProvider;
import com.zqzqq.bootkits.core.lock.FileClusterLockProvider;
import com.zqzqq.bootkits.core.lock.NoOpClusterLockProvider;
import com.zqzqq.bootkits.core.lock.PluginLockManager;
import com.zqzqq.bootkits.core.resource.DefaultResourceManager;
import com.zqzqq.bootkits.core.resource.ResourceManager;
import com.zqzqq.bootkits.core.scanner.ComposePathResolve;
import com.zqzqq.bootkits.core.scanner.DevPathResolve;
import com.zqzqq.bootkits.core.scanner.PathResolve;
import com.zqzqq.bootkits.core.scanner.ProdPathResolve;
import com.zqzqq.bootkits.core.state.EnhancedPluginState;
import com.zqzqq.bootkits.core.version.VersionUtils;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.integration.listener.DefaultPluginListenerFactory;
import com.zqzqq.bootkits.integration.listener.PluginListenerFactory;
import com.zqzqq.bootkits.utils.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class DefaultPluginManager implements PluginManager{

    private final Logger log = LoggerFactory.getLogger(DefaultPluginManager.class);
    private static final String GLOBAL_CLUSTER_LOCK_KEY = "plugin:lifecycle:global";
    private final ResourceManager resourceManager = new DefaultResourceManager();
    private final PluginLockManager lockManager = new PluginLockManager();
    private final ClusterLockProvider clusterLockProvider;
    private final Duration clusterLockTimeout;
    private final Path clusterSharedRoot;
    
    // 使用锁管理器的示例方法
    private void withPluginLock(String pluginId, Runnable action) {
        lockManager.executeWithPluginLock(pluginId,
                () -> withClusterLock(getPluginClusterLockKey(pluginId), action));
    }
    
    private void withGlobalWriteLock(Runnable action) {
        lockManager.executeWithGlobalWriteLock(() -> withClusterLock(GLOBAL_CLUSTER_LOCK_KEY, action));
    }
    
    private void withGlobalReadLock(Runnable action) {
        lockManager.executeWithGlobalReadLock(action);
    }
    
    private <T> T withPluginLock(String pluginId, java.util.function.Supplier<T> action) {
        return lockManager.executeWithPluginLock(pluginId,
                () -> withClusterLock(getPluginClusterLockKey(pluginId), action));
    }
    
    private <T> T withGlobalWriteLock(java.util.function.Supplier<T> action) {
        return lockManager.executeWithGlobalWriteLock(() -> withClusterLock(GLOBAL_CLUSTER_LOCK_KEY, action));
    }
    
    private <T> T withGlobalReadLock(java.util.function.Supplier<T> action) {
        return lockManager.executeWithGlobalReadLock(action);
    }


    private final RealizeProvider provider;
    private final IntegrationConfiguration configuration;
    private final List<String> pluginRootDirs;

    private final PathResolve pathResolve;
    private final PluginBasicChecker basicChecker;

    protected final ComposePluginLauncherChecker launcherChecker;

    private final AtomicBoolean loaded = new AtomicBoolean(false);

    private final Map<String, PluginInsideInfo> startedPlugins = new ConcurrentHashMap<>();
    private final Map<String, PluginInsideInfo> resolvedPlugins = new ConcurrentHashMap<>();

    protected PluginListenerFactory pluginListenerFactory;


    private List<String> sortedPluginIds;

    public DefaultPluginManager(RealizeProvider realizeProvider, IntegrationConfiguration configuration) {
        this(realizeProvider, configuration, null);
    }

    public DefaultPluginManager(RealizeProvider realizeProvider,
                                IntegrationConfiguration configuration,
                                ClusterLockProvider externalClusterLockProvider) {
        this.provider = Assert.isNotNull(realizeProvider, "参数 realizeProvider 不能为空");
        this.configuration = Assert.isNotNull(configuration, "参数 configuration 不能为空");
        this.pluginRootDirs = resolvePath(String.join(",", configuration.pluginPath()));
        this.clusterSharedRoot = resolveClusterSharedRoot();
        long timeoutMillis = Optional.ofNullable(configuration.clusterLockTimeoutMs()).orElse(30000L);
        this.clusterLockTimeout = Duration.ofMillis(Math.max(1000L, timeoutMillis));
        if (externalClusterLockProvider != null) {
            this.clusterLockProvider = externalClusterLockProvider;
            log.info("Use custom cluster lock provider: {}", externalClusterLockProvider.getClass().getName());
        } else {
            this.clusterLockProvider = createClusterLockProvider();
        }
        this.pathResolve = getComposePathResolve();
        this.basicChecker = realizeProvider.getPluginBasicChecker();
        this.launcherChecker = getComposeLauncherChecker(realizeProvider);
        setSortedPluginIds(configuration.sortInitPluginIds());
    }

    protected ComposePluginLauncherChecker getComposeLauncherChecker(RealizeProvider realizeProvider){
        ComposePluginLauncherChecker checker = new ComposePluginLauncherChecker();
        checker.add(new DefaultPluginLauncherChecker(realizeProvider, configuration));
        checker.add(new DependencyPluginLauncherChecker(this));
        return checker;
    }

    protected ComposePathResolve getComposePathResolve(){
        return new ComposePathResolve(new DevPathResolve(), new ProdPathResolve());
    }

    public void setSortedPluginIds(List<String> sortedPluginIds) {
        this.sortedPluginIds = sortedPluginIds;
    }

    @Override
    public List<String> getPluginsRoots() {
        return new ArrayList<>(pluginRootDirs);
    }

    @Override
    public String getDefaultPluginRoot() {
        if(pluginRootDirs == null){
            return null;
        }
        return pluginRootDirs.stream().findFirst().orElseThrow(()-> new PluginException("插件根路径未配置"));
    }

    @Override
    public synchronized List<PluginInfo> loadPlugins() {
        if(loaded.get()){
            throw new PluginException("不能重复调用: loadPlugins");
        }
        try {
            pluginListenerFactory = createPluginListenerFactory();
            if(ObjectUtils.isEmpty(pluginRootDirs)){
                log.warn("插件根目录为空, 无法加载插件.");
                return Collections.emptyList();
            }
            List<Path> scanPluginPaths = provider.getPluginScanner().scan(pluginRootDirs);
            if(ObjectUtils.isEmpty(scanPluginPaths)){
                printOfNotFoundPlugins();
                return Collections.emptyList();
            }
            Map<String, PluginInfo> pluginInfoMap = new LinkedHashMap<>(scanPluginPaths.size());
            List<Path> failedPaths = new ArrayList<>();
            for (Path path : scanPluginPaths) {
                try {
                    PluginDescriptor pluginDescriptor = provider.getPluginDescriptorLoader().load(path);
                    if (pluginDescriptor == null) {
                        log.debug("跳过非插件目录: {}", path);
                        continue;
                    }
                    PluginInsideInfo pluginInsideInfo =
                            createPluginInsideInfo((InsidePluginDescriptor) pluginDescriptor, EnhancedPluginState.LOADED);
                    PluginInfo pluginInfo = createPluginSnapshot(pluginInsideInfo);
                    pluginInfoMap.put(pluginInfo.getPluginId(), pluginInfo);
                    resolvedPlugins.put(pluginInfo.getPluginId(), pluginInsideInfo);
                } catch (Exception e) {
                    log.error("加载插件失败: " + path, e);
                    failedPaths.add(path);
                }
            }
            if(pluginInfoMap.isEmpty() && !failedPaths.isEmpty()){
                return Collections.emptyList();
            }
            List<PluginInfo> pluginInfos = new ArrayList<>(pluginInfoMap.values());
            if(sortedPluginIds != null && !sortedPluginIds.isEmpty()){
                Map<String, Integer> sortOrder = new HashMap<>(sortedPluginIds.size());
                for (int i = 0; i < sortedPluginIds.size(); i++) {
                    sortOrder.put(sortedPluginIds.get(i), i);
                }
                pluginInfos.sort(Comparator
                        .comparingInt((PluginInfo pluginInfo) -> sortOrder.getOrDefault(pluginInfo.getPluginId(), Integer.MAX_VALUE))
                        .thenComparing(PluginInfo::getPluginId));
            }
            if (!failedPaths.isEmpty()) {
                log.warn("部分插件加载失败, successCount={}, failedPaths={}", pluginInfos.size(), failedPaths);
            }
            loaded.set(!pluginInfos.isEmpty());
            return pluginInfos;
        } catch (Exception e) {
            throw new PluginException("加载插件失败", e);
        }
    }

    @Override
    public PluginInfo install(Path path) throws PluginException {
        return install(path, false);
    }

    /**
     * 安装插件（支持解压参数）
     * @param path 插件路径
     * @param unpackPlugin 是否解压插件
     * @return 插件信息
     * @throws PluginException 插件异常
     */
    public PluginInfo install(Path path, boolean unpackPlugin) throws PluginException {
        return withGlobalWriteLock(() -> {
            try {
                // 校验插件路径
                basicChecker.checkPath(path);
                
                // 如果需要解压，先解压插件
                Path actualPath = path;
                if (unpackPlugin && path.toString().endsWith(".zip")) {
                    // 临时解压目录逻辑可以在这里实现
                    actualPath = path; // 目前先使用原路径
                }
                
                // 加载插件描述符
                PluginDescriptor pluginDescriptor = provider.getPluginDescriptorLoader().load(actualPath);
                String pluginId = pluginDescriptor.getPluginId();
                String newVersion = pluginDescriptor.getPluginVersion();
                
                // 检查插件是否已存在
                PluginInfo existingPlugin = getPlugin(pluginId);
                if (existingPlugin != null) {
                    String oldVersion = existingPlugin.getPluginDescriptor().getPluginVersion();
                    log.info("发现已存在的同名插件: {}, 当前版本: {}, 新版本: {}", pluginId, oldVersion, newVersion);
                    
                    // 比较版本号：新版本必须大于旧版本
                    if (!isVersionGreaterThan(newVersion, oldVersion)) {
                        throw new PluginException(String.format(
                                "安装失败：新版本号 %s 必须大于旧版本号 %s", newVersion, oldVersion));
                    }
                    
                    // 如果旧插件正在运行，先停止
                    PluginInsideInfo existingInsideInfo = getPluginInsideInfo(pluginId);
                    if (existingInsideInfo != null && existingInsideInfo.getPluginState() == EnhancedPluginState.STARTED) {
                        log.info("停止旧插件: {}", pluginId);
                        stop(pluginId);
                    }
                    
                    // 从已解析列表中移除旧插件
                    resolvedPlugins.remove(pluginId);
                    log.info("旧插件已卸载: {}", pluginId);
                }
                
                // 复制插件到插件目录
                Path targetPath = Paths.get(getDefaultPluginRoot(), actualPath.getFileName().toString());
                FileUtils.copyFile(actualPath.toFile(), targetPath.toFile());
                
                // 创建插件信息
                PluginInsideInfo pluginInsideInfo =
                        createPluginInsideInfo((InsidePluginDescriptor) pluginDescriptor, EnhancedPluginState.LOADED);
                
                // 更新插件路径为复制后的目标路径（解决临时文件被删除后路径失效问题）
                if (pluginInsideInfo.getPluginDescriptor() instanceof DefaultInsidePluginDescriptor) {
                    ((DefaultInsidePluginDescriptor) pluginInsideInfo.getPluginDescriptor()).setPluginPath(targetPath);
                }
                try {
                    beforeInstall(pluginInsideInfo);
                } catch (Exception e) {
                    try {
                        if (Files.exists(targetPath)) {
                            FileUtils.forceDelete(targetPath.toFile());
                        }
                    } catch (Exception deleteError) {
                        log.warn("Install rollback delete plugin file failed: {}", targetPath, deleteError);
                    }
                    throw e;
                }
                
                // 将插件添加到已解析插件列表
                resolvedPlugins.put(pluginId, pluginInsideInfo);
                
                log.info("插件安装成功: {}, 版本: {}", pluginId, newVersion);
                return createPluginSnapshot(pluginInsideInfo);
            } catch (IOException e) {
                throw new PluginException("插件安装失败", e);
            } catch (Exception e) {
                throw new PluginException("Install plugin failed", e);
            }
        });
    }

    /**
     * 比较版本号：判断新版本是否大于旧版本
     * 支持语义化版本号 (x.y.z) 和简单的数字版本号
     * 
     * @param newVersion 新版本号
     * @param oldVersion 旧版本号
     * @return true 如果新版本大于旧版本
     */
    private boolean isVersionGreaterThan(String newVersion, String oldVersion) {
        if (ObjectUtils.isEmpty(newVersion) || ObjectUtils.isEmpty(oldVersion)) {
            return false;
        }
        return VersionUtils.compareVersions(newVersion, oldVersion) > 0;
    }

    @Override
    public void start(String pluginId) throws PluginException {
        withPluginLock(pluginId, () -> {
            try {
                PluginInsideInfo pluginInsideInfo = getPluginInsideInfo(pluginId);
                if (pluginInsideInfo == null) {
                    throw new PluginException("插件不存在: " + pluginId);
                }
                
                if (pluginInsideInfo.getPluginState() == EnhancedPluginState.STARTED) {
                    log.warn("插件已启动: {}", pluginId);
                    return;
                }
                
                start(pluginInsideInfo);
                startedPlugins.put(pluginId, pluginInsideInfo);
                log.info("插件启动成功: {}, startedPlugins 大小: {}", pluginId, startedPlugins.size());
            } catch (Exception e) {
                throw new PluginException("启动插件失败: " + pluginId, e);
            }
        });
    }

    @Override
    public void stop(String pluginId) throws PluginException {
        withPluginLock(pluginId, () -> {
            try {
                PluginInsideInfo pluginInsideInfo = startedPlugins.get(pluginId);
                if (pluginInsideInfo == null) {
                    throw new PluginException("插件未启动: " + pluginId);
                }
                
                stop(pluginInsideInfo, PluginCloseType.STOP);
                startedPlugins.remove(pluginId);
                log.info("插件停止成功: {}", pluginId);
            } catch (Exception e) {
                throw new PluginException("停止插件失败: " + pluginId, e);
            }
        });
    }

    /**
     * 加载插件（与安装类似，但不添加到已解析列表）
     * @param pluginPath 插件路径
     * @param unpackPlugin 是否解压插件
     * @return 插件信息
     * @throws PluginException 插件异常
     */
    public PluginInfo load(Path pluginPath, boolean unpackPlugin) throws PluginException {
        try {
            // 校验插件路径
            basicChecker.checkPath(pluginPath);
            
            // 如果需要解压，先解压插件
            Path actualPath = pluginPath;
            if (unpackPlugin && pluginPath.toString().endsWith(".zip")) {
                // 临时解压目录逻辑可以在这里实现
                actualPath = pluginPath; // 目前先使用原路径
            }
            
            // 加载插件描述符
            PluginDescriptor pluginDescriptor = provider.getPluginDescriptorLoader().load(actualPath);
            
            // 创建插件信息
            PluginInsideInfo pluginInsideInfo =
                    createPluginInsideInfo((InsidePluginDescriptor) pluginDescriptor, EnhancedPluginState.LOADED);
            
            log.info("插件加载成功: {}", pluginDescriptor.getPluginId());
            return createPluginSnapshot(pluginInsideInfo);
        } catch (Exception e) {
            log.error("加载插件失败: {} - {}", pluginPath, e.getMessage(), e);
            throw new PluginException("加载插件失败: " + pluginPath, e);
        }
    }

    /**
     * 卸载插件（停止但不完全移除）
     * @param pluginId 插件id
     * @throws PluginException 插件异常
     */
    public void unLoad(String pluginId) throws PluginException {
        withPluginLock(pluginId, () -> {
            try {
                // 停止插件
                if (startedPlugins.containsKey(pluginId)) {
                    PluginInsideInfo pluginInsideInfo = startedPlugins.get(pluginId);
                    stop(pluginInsideInfo, PluginCloseType.STOP);
                    startedPlugins.remove(pluginId);
                }
                
                // 从已解析列表中移除
                resolvedPlugins.remove(pluginId);
                
                log.info("插件卸载成功: {}", pluginId);
            } catch (Exception e) {
                throw new PluginException("卸载插件失败: " + pluginId, e);
            }
        });
    }

    /**
     * 升级插件
     * @param pluginPath 新插件路径
     * @param unpackPlugin 是否解压插件
     * @return 插件信息
     * @throws PluginException 插件异常
     */
    public PluginInfo upgrade(Path pluginPath, boolean unpackPlugin) throws PluginException {
        return install(pluginPath, unpackPlugin);
    }

    /**
     * 扫描解析插件
     * @param pluginPath 插件路径
     * @return 插件信息
     * @throws PluginException 插件异常
     */
    public PluginInfo scanParse(Path pluginPath) throws PluginException {
        return parse(pluginPath);
    }

    @Override
    public void uninstall(String pluginId) throws PluginException {
        withGlobalWriteLock(() -> {
            try {
                // 从 startedPlugins 或 resolvedPlugins 中获取插件信息
                PluginInsideInfo pluginInsideInfo = startedPlugins.get(pluginId);
                if (pluginInsideInfo == null) {
                    pluginInsideInfo = resolvedPlugins.get(pluginId);
                }
                
                if (pluginInsideInfo == null) {
                    throw new PluginException("插件不存在: " + pluginId);
                }
                
                // 只在插件已启动时才调用 stop()，如果插件已经停止则跳过
                if (startedPlugins.containsKey(pluginId)) {
                    stop(pluginInsideInfo, PluginCloseType.UNINSTALL);
                }
                
                // 从两个 Map 中移除插件
                startedPlugins.remove(pluginId);
                beforeUninstall(pluginInsideInfo);
                resolvedPlugins.remove(pluginId);

                // 删除插件文件
                Path pluginPath = Path.of(pluginInsideInfo.getPluginDescriptor().getPluginPath());
                File pluginFile = pluginPath.toFile();
                if (pluginFile.exists()) {
                    if (pluginFile.isDirectory()) {
                        // 如果是目录,使用 deleteDirectory
                        FileUtils.deleteDirectory(pluginFile);
                        log.info("插件目录已删除: {}", pluginPath);
                    } else {
                        // 如果是文件(如 .jar 文件),直接删除
                        Files.delete(pluginPath);
                        log.info("插件文件已删除: {}", pluginPath);
                    }
                }
                
                log.info("插件卸载成功: {}", pluginId);
            } catch (IOException e) {
                throw new PluginException("卸载插件失败: " + pluginId, e);
            } catch (Exception e) {
                throw new PluginException("卸载插件失败: " + pluginId, e);
            }
        });
    }

    @Override
    public PluginInfo getPlugin(String pluginId) {
        return withGlobalReadLock(() -> {
            PluginInsideInfo insideInfo = startedPlugins.get(pluginId);
            if (insideInfo == null) {
                insideInfo = resolvedPlugins.get(pluginId);
            }
            return insideInfo == null ? null : createPluginSnapshot(insideInfo);
        });
    }

    @Override
    public List<PluginInfo> getPlugins() {
        return withGlobalReadLock(() -> {
            // 使用 LinkedHashMap 保持插入顺序并去重
            Map<String, PluginInfo> pluginMap = new LinkedHashMap<>();
            
            // 优先添加已启动的插件
            startedPlugins.values().forEach(info -> {
                pluginMap.put(info.getPluginId(), createPluginSnapshot(info));
            });
            
            // 添加已解析但未启动的插件（不覆盖已启动的插件）
            resolvedPlugins.values().forEach(info -> {
                if (!pluginMap.containsKey(info.getPluginId())) {
                    pluginMap.put(info.getPluginId(), createPluginSnapshot(info));
                }
            });
            return new ArrayList<>(pluginMap.values());
        });
    }

    @Override
    public List<PluginInfo> getStartedPlugins() {
        return withGlobalReadLock(() -> {
            List<PluginInfo> plugins = new ArrayList<>();
            startedPlugins.values().forEach(info -> plugins.add(createPluginSnapshot(info)));
            return plugins;
        });
    }

    @Override
    public List<PluginInfo> getResolvedPlugins() {
        return withGlobalReadLock(() -> {
            List<PluginInfo> plugins = new ArrayList<>();
            resolvedPlugins.values().forEach(info -> plugins.add(createPluginSnapshot(info)));
            return plugins;
        });
    }

    /**
     * 获取插件信息（getPlugin的别名方法）
     * @param pluginId 插件id
     * @return 插件信息
     */
    public PluginInfo getPluginInfo(String pluginId) {
        return getPlugin(pluginId);
    }

    /**
     * 获取所有插件信息列表（别名方法）
     * @return 插件信息列表
     */
    public List<PluginInsideInfo> getPluginInfos() {
        return withGlobalReadLock(() -> {
            List<PluginInsideInfo> plugins = new ArrayList<>();
            startedPlugins.values().forEach(plugins::add);
            resolvedPlugins.values().forEach(plugins::add);
            return plugins;
        });
    }

    @Override
    public void reload(String pluginId) throws PluginException {
        withPluginLock(pluginId, () -> {
            try {
                boolean wasStarted = startedPlugins.containsKey(pluginId);
                log.info("Reload plugin: {}, previous started: {}", pluginId, wasStarted ? "STARTED" : "STOPPED");
                
                // 先停止插件（如果已启动）
                if (wasStarted) {
                    log.info("停止插件: {}", pluginId);
                    stop(pluginId);
                    log.info("插件已停止: {}, startedPlugins 大小: {}", pluginId, startedPlugins.size());
                }
                
                PluginInsideInfo pluginInsideInfo = resolvedPlugins.get(pluginId);
                if (pluginInsideInfo == null) {
                    throw new PluginException("插件不存在: " + pluginId);
                }
                
                EnhancedPluginState previousState = pluginInsideInfo.getPluginState();

                // 重新加载插件
                Path pluginPath = Path.of(pluginInsideInfo.getPluginDescriptor().getPluginPath());
                PluginDescriptor pluginDescriptor = provider.getPluginDescriptorLoader().load(pluginPath);
                EnhancedPluginState restoredState = wasStarted
                        ? EnhancedPluginState.STOPPED
                        : (previousState != null ? previousState : EnhancedPluginState.LOADED);
                PluginInsideInfo newPluginInsideInfo =
                        createPluginInsideInfo((InsidePluginDescriptor) pluginDescriptor, restoredState);
                newPluginInsideInfo.setFollowSystem(pluginInsideInfo.isFollowSystem());
                log.info("创建新的 PluginInsideInfo: {}, 状态: {}", pluginId, restoredState);
                
                // 更新插件信息
                resolvedPlugins.put(pluginId, newPluginInsideInfo);
                
                if (wasStarted) {
                    log.info("重新启动插件: {}", pluginId);
                    start(pluginId);
                } else {
                    startedPlugins.remove(pluginId);
                    log.info("插件保持原有未启动状态: {}", pluginId);
                }
                log.info("Plugin reload finished: {}, current state: {}", pluginId,
                    startedPlugins.containsKey(pluginId) ? "STARTED" : newPluginInsideInfo.getPluginState());
                
                log.info("插件重新加载成功: {}", pluginId);
            } catch (Exception e) {
                log.error("重新加载插件失败: {}", pluginId, e);
                throw new PluginException("重新加载插件失败: " + pluginId, e);
            }
        });
    }

    @Override
    public boolean verify(Path jarPath) throws PluginException {
        try {
            // 校验插件路径
            basicChecker.checkPath(jarPath);
            
            // 加载插件描述符进行验证
            PluginDescriptor pluginDescriptor = provider.getPluginDescriptorLoader().load(jarPath);
            
            // 检查插件描述符是否有效
            if (pluginDescriptor == null) {
                log.error("插件描述符为空: {}", jarPath);
                return false;
            }
            
            // 检查插件基本信息的完整性
            if (ObjectUtils.isEmpty(pluginDescriptor.getPluginId()) || 
                ObjectUtils.isEmpty(pluginDescriptor.getName()) ||
                ObjectUtils.isEmpty(pluginDescriptor.getMainClass())) {
                log.error("插件基本信息不完整: {}", jarPath);
                return false;
            }
            
            log.debug("插件验证成功: {} ({})", pluginDescriptor.getName(), pluginDescriptor.getPluginId());
            return true;
            
        } catch (Exception e) {
            log.error("插件验证失败: {} - {}", jarPath, e.getMessage(), e);
            throw new PluginException("插件验证失败: " + jarPath, e);
        }
    }

    @Override
    public PluginInfo parse(Path pluginPath) throws PluginException {
        try {
            // 校验插件路径
            basicChecker.checkPath(pluginPath);
            
            // 加载插件描述符
            PluginDescriptor pluginDescriptor = provider.getPluginDescriptorLoader().load(pluginPath);
            
            // 创建插件信息
            PluginInsideInfo pluginInsideInfo =
                    createPluginInsideInfo((InsidePluginDescriptor) pluginDescriptor, EnhancedPluginState.PARSED);
            return createPluginSnapshot(pluginInsideInfo);
            
        } catch (Exception e) {
            log.error("解析插件失败: {} - {}", pluginPath, e.getMessage(), e);
            throw new PluginException("解析插件失败: " + pluginPath, e);
        }
    }

    @Override
    public void close() {
        withGlobalWriteLock(() -> {
            try {
                // 停止所有已启动的插件
                List<String> startedPluginIds = new ArrayList<>(startedPlugins.keySet());
                for (String pluginId : startedPluginIds) {
                    try {
                        stop(pluginId);
                    } catch (Exception e) {
                        log.error("停止插件失败: " + pluginId, e);
                    }
                }
                
                // 清理资源
                startedPlugins.clear();
                resolvedPlugins.clear();
                loaded.set(false);
                
                log.info("插件管理器已关闭");
            } catch (Exception e) {
                log.error("Close plugin manager failed", e);
            }
        });
    }

    // 辅助方法
    protected PluginListenerFactory createPluginListenerFactory() {
        return new DefaultPluginListenerFactory();
    }

    protected void start(PluginInsideInfo pluginInsideInfo) throws Exception {
        pluginInsideInfo.setPluginState(EnhancedPluginState.STARTED);
        startFinish(pluginInsideInfo);
    }

    protected void startFinish(PluginInsideInfo pluginInsideInfo) {
        // 启动完成后的处理
    }

    protected void stop(PluginInsideInfo pluginInsideInfo, PluginCloseType closeType) throws Exception {
        pluginInsideInfo.setPluginState(EnhancedPluginState.STOPPED);
    }

    /**
     * Hook before plugin registration on install.
     */
    protected void beforeInstall(PluginInsideInfo pluginInsideInfo) throws Exception {
    }

    /**
     * Hook before plugin metadata/files are removed on uninstall.
     */
    protected void beforeUninstall(PluginInsideInfo pluginInsideInfo) throws Exception {
    }

    protected Path getClusterSharedRoot() {
        return clusterSharedRoot;
    }

    private ClusterLockProvider createClusterLockProvider() {
        if (!Boolean.TRUE.equals(configuration.clusterEnabled())) {
            return new NoOpClusterLockProvider();
        }
        Path lockDir = clusterSharedRoot.resolve(".plugin-locks");
        log.info("Plugin cluster lock enabled. lockDir={}, timeout={}ms", lockDir, clusterLockTimeout.toMillis());
        return new FileClusterLockProvider(lockDir);
    }

    private Path resolveClusterSharedRoot() {
        String basePath = configuration.clusterSharedPath();
        if (ObjectUtils.isEmpty(basePath)) {
            if (ObjectUtils.isEmpty(pluginRootDirs)) {
                basePath = System.getProperty("user.dir");
            } else {
                basePath = pluginRootDirs.get(0);
            }
        }
        if (FilesUtils.isRelativePath(basePath)) {
            basePath = FilesUtils.resolveRelativePath(System.getProperty("user.dir"), basePath);
        }
        return Paths.get(basePath).toAbsolutePath().normalize();
    }

    private String getPluginClusterLockKey(String pluginId) {
        return "plugin:lifecycle:" + pluginId;
    }

    private void withClusterLock(String key, Runnable action) {
        try (ClusterLock ignored = clusterLockProvider.acquire(key, clusterLockTimeout)) {
            action.run();
        }
    }

    private <T> T withClusterLock(String key, java.util.function.Supplier<T> action) {
        try (ClusterLock ignored = clusterLockProvider.acquire(key, clusterLockTimeout)) {
            return action.get();
        }
    }

    private PluginInsideInfo getPluginInsideInfo(String pluginId) {
        PluginInsideInfo insideInfo = startedPlugins.get(pluginId);
        if (insideInfo == null) {
            insideInfo = resolvedPlugins.get(pluginId);
        }
        return insideInfo;
    }

    private List<String> resolvePath(String pluginPath) {
        if (ObjectUtils.isEmpty(pluginPath)) {
            return Collections.emptyList();
        }
        return Arrays.asList(pluginPath.split(","));
    }

    private void printOfNotFoundPlugins() {
        log.warn("No plugin found in directories {}", pluginRootDirs);
    }

    private PluginInsideInfo createPluginInsideInfo(InsidePluginDescriptor descriptor, EnhancedPluginState initialState) {
        PluginInsideInfo pluginInsideInfo = new DefaultPluginInsideInfo(descriptor);
        if (initialState != null) {
            pluginInsideInfo.setPluginState(initialState);
        }
        return pluginInsideInfo;
    }

    private PluginInfo createPluginSnapshot(PluginInfo pluginInfo) {
        return new DefaultPluginInfo(pluginInfo);
    }
}
