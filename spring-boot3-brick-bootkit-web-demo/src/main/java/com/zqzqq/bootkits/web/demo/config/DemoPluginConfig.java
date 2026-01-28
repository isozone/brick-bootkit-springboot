//package com.zqzqq.bootkits.web.demo.config;
//
//import com.zqzqq.bootkits.core.PluginInfo;
//import com.zqzqq.bootkits.core.PluginState;
//import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
//import com.zqzqq.bootkits.core.descriptor.PluginDescriptor;
//import com.zqzqq.bootkits.core.descriptor.PluginLibInfo;
//import com.zqzqq.bootkits.core.exception.PluginException;
//import com.zqzqq.bootkits.web.dto.ApiResult;
//import com.zqzqq.bootkits.web.dto.PluginDTO;
//import com.zqzqq.bootkits.web.dto.PluginDetailDTO;
//import com.zqzqq.bootkits.web.dto.PageResult;
//import com.zqzqq.bootkits.web.service.DemoPluginService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.File;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption;
//import java.time.LocalDateTime;
//import java.time.ZoneOffset;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.jar.JarEntry;
//import java.util.jar.JarFile;
//import java.util.stream.Collectors;
//
///**
// * Demo 环境的插件管理配置
// * 提供简化的上传和安装功能
// *
// * @author brick-bootkit
// */
//@Slf4j
//@Configuration
//public class DemoPluginConfig {
//
//    /**
//     * Demo 插件状态
//     */
//    public enum DemoPluginState implements PluginState {
//        STARTED {
//            @Override
//            public boolean canTransitionTo(PluginState targetState) {
//                return targetState == STOPPED;
//            }
//            @Override
//            public String getDescription() {
//                return "运行中";
//            }
//        },
//        STOPPED {
//            @Override
//            public boolean canTransitionTo(PluginState targetState) {
//                return targetState == STARTED;
//            }
//            @Override
//            public String getDescription() {
//                return "已停止";
//            }
//        }
//    }
//
//
//
//    /**
//     * Demo 插件信息
//     */
//    public static class DemoPluginInfo implements PluginInfo {
//        private final String pluginId;
//        private final String name;
//        private final String version;
//        private final String author;
//        private final String description;
//        private final String pluginPath;
//        private final InsidePluginDescriptor descriptor;
//        private DemoPluginState state = DemoPluginState.STOPPED;
//        private long startTime = 0;
//        private long stopTime = 0;
//
//        public DemoPluginInfo(String pluginId, String name, String version, String author,
//                              String description, String pluginPath) {
//            this.pluginId = pluginId;
//            this.name = name;
//            this.version = version;
//            this.author = author;
//            this.description = description;
//            this.pluginPath = pluginPath;
//            this.descriptor = new DemoDescriptor(pluginId, name, version, description, author);
//        }
//
//        @Override public String getPluginId() { return pluginId; }
//        @Override public String getName() { return name; }
//        @Override public String getVersion() { return version; }
//        @Override public String getAuthor() { return author; }
//        @Override public String getDescription() { return description; }
//        @Override public PluginState getPluginState() { return state; }
//        @Override public long getStartTime() { return startTime; }
//        @Override public long getStopTime() { return stopTime; }
//        @Override public boolean isFollowSystem() { return true; }
//        @Override public String getPluginPath() { return pluginPath; }
//        @Override public InsidePluginDescriptor getPluginDescriptor() { return descriptor; }
//        @Override public ClassLoader getClassLoader() { return getClass().getClassLoader(); }
//        @Override public Map<String, Object> getExtensionInfo() { return Collections.emptyMap(); }
//
//        public void setState(DemoPluginState state) { this.state = state; }
//        public void setStartTime(long t) { this.startTime = t; }
//        public void setStopTime(long t) { this.stopTime = t; }
//    }
//
//    /**
//     * Demo 插件存储
//     */
//    public static class DemoPluginStore {
//        private final Map<String, DemoPluginInfo> plugins = new ConcurrentHashMap<>();
//        private final Path pluginsDir;
//        private final Path uploadTempPath;
//
//        public DemoPluginStore() {
//            this.pluginsDir = Paths.get(System.getProperty("java.io.tmpdir"), "brick-demo-plugins");
//            this.uploadTempPath = Paths.get(System.getProperty("java.io.tmpdir"), "brick-upload-temp");
//            try {
//                Files.createDirectories(pluginsDir);
//                Files.createDirectories(uploadTempPath);
//                log.info("Demo 插件目录: {}", pluginsDir);
//            } catch (Exception e) {
//                log.warn("创建插件目录失败: {}", e.getMessage());
//            }
//        }
//
//        public List<PluginInfo> getAll() { return new ArrayList<>(plugins.values()); }
//
//        public PluginInfo get(String pluginId) { return plugins.get(pluginId); }
//
//        public PluginInfo add(DemoPluginInfo plugin) { return plugins.put(plugin.getPluginId(), plugin); }
//
//        public PluginInfo remove(String pluginId) { return plugins.remove(pluginId); }
//
//        public Path getUploadTempPath() { return uploadTempPath; }
//
//        public Path getPluginsDir() { return pluginsDir; }
//    }
//
//    /**
//     * Demo 插件服务实现
//     */
//    @Slf4j
//    public static class DemoPluginServiceImpl implements DemoPluginService {
//        private final DemoPluginStore store;
//
//        public DemoPluginServiceImpl(DemoPluginStore store) {
//            this.store = store;
//        }
//
//        @Override
//        public PageResult<PluginDTO> listPlugins(int page, int size, String state, String keyword) {
//            List<PluginDTO> list = store.getAll().stream()
//                .map(this::toDTO)
//                .filter(dto -> {
//                    if ("all".equalsIgnoreCase(state) || !org.springframework.util.StringUtils.hasText(state)) return true;
//                    return state.equalsIgnoreCase(dto.getState());
//                })
//                .filter(dto -> !org.springframework.util.StringUtils.hasText(keyword) ||
//                    dto.getPluginId().toLowerCase().contains(keyword.toLowerCase()) ||
//                    (dto.getName() != null && dto.getName().toLowerCase().contains(keyword.toLowerCase())))
//                .collect(Collectors.toList());
//
//            int from = (page - 1) * size;
//            int to = Math.min(from + size, list.size());
//            if (from > list.size()) from = 0;
//            List<PluginDTO> pageList = from < list.size() ? list.subList(from, to) : Collections.emptyList();
//            return PageResult.of(pageList, list.size(), page, size);
//        }
//
//        @Override
//        public List<PluginDTO> getAllPlugins() {
//            return store.getAll().stream().map(this::toDTO).collect(Collectors.toList());
//        }
//
//        @Override
//        public PluginDetailDTO getDetail(String pluginId) {
//            DemoPluginInfo plugin = (DemoPluginInfo) store.get(pluginId);
//            if (plugin == null) return null;
//            return PluginDetailDTO.builder()
//                .pluginId(pluginId)
//                .name(plugin.getName())
//                .version(plugin.getVersion())
//                .author(plugin.getAuthor())
//                .description(plugin.getDescription())
//                .state(plugin.getPluginState().name())
//                .stateDescription(plugin.getPluginState().getDescription())
//                .pluginPath(plugin.getPluginPath())
//                .build();
//        }
//
//        @Override
//        public ApiResult<PluginDTO> uploadPlugin(MultipartFile file, Boolean enableAfterUpload) {
//            if (file.isEmpty()) throw new PluginException("上传文件不能为空");
//            String filename = file.getOriginalFilename();
//            if (filename == null || !filename.endsWith(".jar")) {
//                throw new PluginException("只能上传 JAR 格式的插件文件");
//            }
//
//            try {
//                Path uploadPath = store.getUploadTempPath();
//                Path targetPath = uploadPath.resolve(filename);
//                Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
//                log.info("文件上传到: {}", targetPath);
//
//                if (enableAfterUpload != null && enableAfterUpload) {
//                    PluginInfo plugin = installPlugin(targetPath);
//                    return ApiResult.success(toDTO((DemoPluginInfo) plugin));
//                }
//                return ApiResult.success();
//
//            } catch (Exception e) {
//                log.error("上传失败", e);
//                throw new PluginException("插件上传失败: " + e.getMessage());
//            }
//        }
//
//        @Override
//        public PluginDTO installPlugin(Path pluginPath) {
//            try {
//                PluginDescriptorInfo info = parseDescriptor(pluginPath.toFile());
//                Path targetPath = store.getPluginsDir().resolve(pluginPath.getFileName());
//                Files.copy(pluginPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
//
//                DemoPluginInfo plugin = new DemoPluginInfo(info.pluginId, info.name, info.version,
//                                                           info.author, info.description, targetPath.toString());
//                store.add(plugin);
//                log.info("插件安装成功: {} ({})", info.name, info.pluginId);
//                return toDTO(plugin);
//
//            } catch (Exception e) {
//                throw new PluginException("安装失败: " + e.getMessage());
//            }
//        }
//
//        @Override
//        public void startPlugin(String pluginId) {
//            DemoPluginInfo plugin = (DemoPluginInfo) store.get(pluginId);
//            if (plugin == null) throw new PluginException("插件不存在: " + pluginId);
//            plugin.setState(DemoPluginState.STARTED);
//            plugin.setStartTime(System.currentTimeMillis());
//            log.info("插件启动: {}", pluginId);
//        }
//
//        @Override
//        public void stopPlugin(String pluginId) {
//            DemoPluginInfo plugin = (DemoPluginInfo) store.get(pluginId);
//            if (plugin == null) throw new PluginException("插件不存在: " + pluginId);
//            plugin.setState(DemoPluginState.STOPPED);
//            plugin.setStopTime(System.currentTimeMillis());
//            log.info("插件停止: {}", pluginId);
//        }
//
//        @Override
//        public void restartPlugin(String pluginId) {
//            stopPlugin(pluginId);
//            startPlugin(pluginId);
//        }
//
//        @Override
//        public void uninstallPlugin(String pluginId) {
//            DemoPluginInfo plugin = (DemoPluginInfo) store.remove(pluginId);
//            if (plugin != null) {
//                try { Files.deleteIfExists(Paths.get(plugin.getPluginPath())); }
//                catch (Exception e) { log.warn("删除文件失败: {}", e.getMessage()); }
//            }
//            log.info("插件卸载: {}", pluginId);
//        }
//
//        @Override
//        public boolean verifyPlugin(Path pluginPath) {
//            try { parseDescriptor(pluginPath.toFile()); return true; }
//            catch (Exception e) { return false; }
//        }
//
//        private PluginDTO toDTO(DemoPluginInfo plugin) {
//            return PluginDTO.builder()
//                .pluginId(plugin.getPluginId())
//                .name(plugin.getName())
//                .version(plugin.getVersion())
//                .author(plugin.getAuthor())
//                .description(plugin.getDescription())
//                .state(plugin.getPluginState().name())
//                .stateDescription(plugin.getPluginState().getDescription())
//                .pluginPath(plugin.getPluginPath())
//                .startTime(plugin.getStartTime() > 0 ?
//                    LocalDateTime.ofEpochSecond(plugin.getStartTime() / 1000, 0, ZoneOffset.UTC) : null)
//                .stopTime(plugin.getStopTime() > 0 ?
//                    LocalDateTime.ofEpochSecond(plugin.getStopTime() / 1000, 0, ZoneOffset.UTC) : null)
//                .build();
//        }
//
//        private PluginDescriptorInfo parseDescriptor(File file) throws Exception {
//            Properties props = new Properties();
//            try (JarFile jar = new JarFile(file)) {
//                JarEntry entry = jar.getJarEntry("plugin.properties");
//                if (entry == null) entry = jar.getJarEntry("META-INF/plugin.properties");
//                if (entry == null) throw new RuntimeException("未找到 plugin.properties");
//                try (var in = jar.getInputStream(entry)) { props.load(in); }
//            }
//            String pluginId = props.getProperty("plugin.id");
//            if (pluginId == null) throw new RuntimeException("缺少 plugin.id");
//            return new PluginDescriptorInfo(
//                pluginId,
//                props.getProperty("plugin.name", pluginId),
//                props.getProperty("plugin.version", "1.0.0"),
//                props.getProperty("plugin.author", "未知"),
//                props.getProperty("plugin.description", "")
//            );
//        }
//
//        private record PluginDescriptorInfo(String pluginId, String name, String version, String author, String description) {}
//    }
//
//    @Bean
//    public DemoPluginStore demoPluginStore() {
//        return new DemoPluginStore();
//    }
//
//    @Bean
//    @ConditionalOnMissingBean
//    public DemoPluginService demoPluginService(DemoPluginStore store) {
//        return new DemoPluginServiceImpl(store);
//    }
//}
