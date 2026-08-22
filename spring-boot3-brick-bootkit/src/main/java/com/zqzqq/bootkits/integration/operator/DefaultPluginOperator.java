/**
 * Copyright 2019-Present starBlues and the brick-bootkit contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package com.zqzqq.bootkits.integration.operator;

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.PluginLauncherManager;
import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.core.RealizeProvider;
import com.zqzqq.bootkits.core.admission.PluginAdmissionCheck;
import com.zqzqq.bootkits.core.admission.PluginAdmissionPipeline;
import com.zqzqq.bootkits.core.admission.PluginDescriptorAdmissionCheck;
import com.zqzqq.bootkits.core.exception.PluginDisabledException;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.core.lock.ClusterLockProvider;
import com.zqzqq.bootkits.core.state.EnhancedPluginState;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.integration.listener.PluginInitializerListener;
import com.zqzqq.bootkits.integration.listener.PluginInitializerListenerFactory;
import com.zqzqq.bootkits.integration.operator.upload.UploadByInputStreamParam;
import com.zqzqq.bootkits.integration.operator.upload.UploadByMultipartFileParam;
import com.zqzqq.bootkits.integration.operator.upload.UploadParam;
import com.zqzqq.bootkits.integration.rollout.PluginRolloutMode;
import com.zqzqq.bootkits.integration.rollout.PluginRolloutProbe;
import com.zqzqq.bootkits.integration.rollout.PluginRolloutProbeResult;
import com.zqzqq.bootkits.integration.spi.PluginLifecycleExtensionManager;
import com.zqzqq.bootkits.loader.launcher.DevelopmentModeSetting;
import com.zqzqq.bootkits.spring.web.PluginStaticResourceConfig;
import com.zqzqq.bootkits.utils.Assert;
import com.zqzqq.bootkits.utils.FilesUtils;
import com.zqzqq.bootkits.utils.ObjectUtils;
import com.zqzqq.bootkits.utils.PluginFileUtils;
import com.zqzqq.bootkits.utils.ResourceUtils;
import com.zqzqq.bootkits.utils.SpringBeanUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Default plugin operator.
 */
public class DefaultPluginOperator implements PluginOperator {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicBoolean IS_INIT = new AtomicBoolean(false);

    private final GenericApplicationContext applicationContext;
    private final IntegrationConfiguration configuration;
    private final PluginManager pluginManager;
    private final PluginInitializerListenerFactory pluginInitializerListenerFactory;
    private final List<PluginRolloutProbe> rolloutProbes;

    public DefaultPluginOperator(GenericApplicationContext applicationContext,
                                 RealizeProvider realizeProvider,
                                 IntegrationConfiguration configuration) {
        this.applicationContext = applicationContext;
        this.configuration = configuration;

        ClusterLockProvider clusterLockProvider = resolveClusterLockProvider(applicationContext, configuration);
        PluginAdmissionPipeline admissionPipeline = createAdmissionPipeline(applicationContext, configuration);
        PluginLifecycleExtensionManager extensionManager =
                new PluginLifecycleExtensionManager(applicationContext, configuration);

        this.pluginManager = new PluginLauncherManager(realizeProvider,
                applicationContext,
                configuration,
                clusterLockProvider,
                admissionPipeline,
                extensionManager);
        this.pluginInitializerListenerFactory = new PluginInitializerListenerFactory(applicationContext);
        this.rolloutProbes = new ArrayList<>(SpringBeanUtils.getBeans(applicationContext, PluginRolloutProbe.class));
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    @Override
    public synchronized boolean initPlugins(PluginInitializerListener pluginInitializerListener) throws PluginException {
        if (IS_INIT.get()) {
            throw new RuntimeException("Plugins already initialized");
        }
        try {
            log.info("Plugin environment: {}", configuration.environment().toString());
            log.info("Plugin development mode: {}", DevelopmentModeSetting.getDevelopmentMode());
            pluginInitializerListenerFactory.addListener(pluginInitializerListener);
            List<String> pluginsRoots = pluginManager.getPluginsRoots();
            if (pluginsRoots.isEmpty()) {
                return true;
            }
            initBeforeLogPrint();
            if (Boolean.FALSE.equals(configuration.enable())) {
                log.info("Plugin function disabled");
                return false;
            }
            List<PluginInfo> pluginInfos = pluginManager.loadPlugins();
            if (ObjectUtils.isEmpty(pluginInfos)) {
                return false;
            }
            pluginInitializerListenerFactory.before();
            boolean foundException = false;
            for (PluginInfo pluginInfo : pluginInfos) {
                try {
                    pluginManager.start(pluginInfo.getPluginId());
                } catch (Exception e) {
                    if (e instanceof PluginDisabledException) {
                        log.info(e.getMessage());
                        continue;
                    }
                    log.error(e.getMessage(), e);
                    foundException = true;
                }
            }
            IS_INIT.set(true);
            if (foundException) {
                pluginInitializerListenerFactory.failure(new PluginException("Plugin initialization has failures"));
                return false;
            }
            pluginInitializerListenerFactory.complete();
            return true;
        } catch (Exception e) {
            pluginInitializerListenerFactory.failure(e);
            throw e;
        }
    }

    private void initBeforeLogPrint() {
        List<String> pluginsRoots = pluginManager.getPluginsRoots();
        log.info("Begin load plugins from:\n{}", String.join("\n", pluginsRoots));
        PluginStaticResourceConfig resourceConfig = SpringBeanUtils.getExistBean(applicationContext,
                PluginStaticResourceConfig.class);
        if (resourceConfig != null) {
            resourceConfig.logPathPrefix();
        }
    }

    @Override
    public boolean verify(Path jarPath) throws PluginException {
        return pluginManager.verify(jarPath);
    }

    @Override
    public PluginInfo parse(Path pluginPath) throws PluginException {
        return pluginManager.parse(pluginPath);
    }

    @Override
    public PluginInfo install(Path pluginPath, boolean unpackPlugin) throws PluginException {
        return pluginManager.install(pluginPath);
    }

    @Override
    public void uninstall(String pluginId, boolean isDelete, boolean isBackup) throws PluginException {
        uninstallBackup(pluginId, isDelete, isBackup);
    }

    @Override
    public PluginInfo load(Path pluginPath, boolean unpackPlugin) throws PluginException {
        return pluginManager.parse(pluginPath);
    }

    @Override
    public boolean unload(String pluginId) throws PluginException {
        pluginManager.uninstall(pluginId);
        return true;
    }

    @Override
    public boolean start(String pluginId) throws PluginException {
        try {
            pluginManager.start(pluginId);
            return true;
        } catch (Exception e) {
            log.error("Start plugin failed: {}", pluginId, e);
            return false;
        }
    }

    @Override
    public boolean stop(String pluginId) throws PluginException {
        PluginInfo pluginInfo = pluginManager.getPlugin(pluginId);
        if (pluginInfo == null) {
            throw new PluginException("Plugin not found: " + pluginId);
        }
        try {
            pluginManager.stop(pluginId);
            return true;
        } catch (Exception e) {
            log.error("Stop plugin failed: {}", pluginId, e);
            return false;
        }
    }

    @Override
    public PluginInfo uploadPlugin(UploadParam uploadParam) throws PluginException {
        Assert.isNotNull(uploadParam, "uploadParam is null");
        try {
            if (uploadParam instanceof UploadByInputStreamParam) {
                UploadByInputStreamParam param = (UploadByInputStreamParam) uploadParam;
                return uploadPlugin(param.getPluginFileName(), param.getInputStream(),
                        param.isBackOldPlugin(), param.isUnpackPlugin());
            }
            if (uploadParam instanceof UploadByMultipartFileParam) {
                UploadByMultipartFileParam param = (UploadByMultipartFileParam) uploadParam;
                MultipartFile file = param.getPluginMultipartFile();
                return uploadPlugin(file.getOriginalFilename(), file.getInputStream(),
                        param.isBackOldPlugin(), param.isUnpackPlugin());
            }
            throw new PluginException("Unsupported upload param type: " + uploadParam.getClass().getName());
        } catch (Exception e) {
            if (e instanceof PluginException) {
                throw (PluginException) e;
            }
            throw new PluginException(e.getMessage(), e);
        }
    }

    @Override
    public Path backupPlugin(Path backDirPath, String sign) throws PluginException {
        if (configuration.isDev()) {
            return backDirPath;
        }
        Objects.requireNonNull(backDirPath);
        return operatePluginFile(backDirPath, sign, true, false);
    }

    @Override
    public Path backupPlugin(String pluginId, String sign) throws PluginException {
        if (configuration.isDev()) {
            return null;
        }
        PluginInfo pluginInfo = getPluginInfo(pluginId);
        return backupPlugin(Paths.get(pluginInfo.getPluginPath()), sign);
    }

    @Override
    public List<PluginInfo> getPluginInfo() {
        return pluginManager.getPlugins().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public PluginInfo getPluginInfo(String pluginId) {
        return pluginManager.getPlugin(pluginId);
    }

    protected Path uninstallBackup(String pluginId, boolean isDelete, boolean isBackup) {
        PluginInfo pluginInfo = pluginManager.getPlugin(pluginId);
        if (pluginInfo == null) {
            throw new PluginException(pluginId, "not found");
        }
        pluginManager.uninstall(pluginId);
        if (configuration.isDev()) {
            return null;
        }
        return operatePluginFile(Paths.get(pluginInfo.getPluginPath()), "uninstall", isBackup, isDelete);
    }

    protected PluginInfo uploadPlugin(String pluginFileName,
                                      InputStream inputStream,
                                      boolean isBackOldPlugin,
                                      boolean isUnpackPluginFile) throws Exception {
        if (ObjectUtils.isEmpty(pluginFileName)) {
            throw new PluginException("Plugin file name is empty");
        }
        if (!ResourceUtils.isJar(pluginFileName) && !ResourceUtils.isZip(pluginFileName)) {
            throw new PluginException("Only jar/zip plugin package is supported");
        }

        String tempPathString = FilesUtils.joiningFilePath(configuration.uploadTempPath(), pluginFileName);
        Path tempFilePath = Paths.get(tempPathString);
        File tempFile = PluginFileUtils.createExistFile(tempFilePath);

        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            IOUtils.copy(inputStream, outputStream);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        File unpackPluginFile = tempFile;
        try {
            if (isUnpackPluginFile) {
                unpackPluginFile = PluginFileUtils.decompressZip(tempFile.toString(), configuration.uploadTempPath());
            }

            PluginInfo uploadPluginInfo = pluginManager.parse(unpackPluginFile.toPath());
            if (uploadPluginInfo == null) {
                Exception exception = new Exception(pluginFileName + " verify failed");
                verifyFailureDelete(tempFilePath, exception);
                throw exception;
            }

            PluginInfo oldPluginInfo = getPluginInfo(uploadPluginInfo.getPluginId());
            if (oldPluginInfo == null) {
                return pluginManager.install(unpackPluginFile.toPath());
            }

            Path oldPluginPath = Paths.get(oldPluginInfo.getPluginPath());
            boolean oldPluginStarted = isPluginStarted(oldPluginInfo);
            boolean rollbackEnabled = configuration.pluginRolloutRollbackOnFailure();
            Path rollbackBackupPath = null;
            if (isBackOldPlugin || rollbackEnabled) {
                rollbackBackupPath = backupPlugin(oldPluginPath, "upload");
            }

            try {
                PluginInfo pluginInfo = pluginManager.install(unpackPluginFile.toPath());

                if (oldPluginStarted && configuration.pluginRolloutAutoStart()) {
                    pluginManager.start(pluginInfo.getPluginId());
                }

                if (oldPluginStarted && configuration.pluginRolloutMode() == PluginRolloutMode.GRAY) {
                    runGrayRolloutProbes(pluginInfo);
                }

                FileUtils.delete(oldPluginPath.toFile());
                return pluginInfo;
            } catch (Exception upgradeError) {
                if (rollbackEnabled && rollbackBackupPath != null) {
                    rollbackUpgrade(uploadPluginInfo.getPluginId(), rollbackBackupPath, oldPluginStarted);
                }
                throw upgradeError;
            }
        } catch (Exception e) {
            verifyFailureDelete(tempFilePath, e);
            throw e;
        } finally {
            FileUtils.deleteQuietly(unpackPluginFile);
        }
    }

    private boolean isPluginStarted(PluginInfo pluginInfo) {
        return pluginInfo != null && pluginInfo.getPluginState() == EnhancedPluginState.STARTED;
    }

    private void runGrayRolloutProbes(PluginInfo pluginInfo) {
        if (rolloutProbes.isEmpty()) {
            return;
        }
        for (PluginRolloutProbe probe : rolloutProbes) {
            PluginRolloutProbeResult result = probe.probe(pluginInfo.getPluginId(), pluginInfo);
            if (result == null) {
                continue;
            }
            if (!result.isPassed()) {
                throw new PluginException("Gray rollout probe rejected. probe=" + probe.getName()
                        + ", plugin=" + pluginInfo.getPluginId() + ", detail=" + result.getMessage());
            }
        }
    }

    private void rollbackUpgrade(String pluginId, Path rollbackBackupPath, boolean previousStarted) {
        log.warn("Plugin upgrade failed, start rollback. plugin={}, backup={}", pluginId, rollbackBackupPath);
        try {
            PluginInfo current = pluginManager.getPlugin(pluginId);
            if (current != null) {
                pluginManager.uninstall(pluginId);
            }
        } catch (Exception uninstallError) {
            log.warn("Ignore uninstall error during rollback. plugin={}", pluginId, uninstallError);
        }

        PluginInfo restored = pluginManager.install(rollbackBackupPath);
        if (previousStarted && restored != null) {
            pluginManager.start(pluginId);
        }
        log.warn("Plugin rollback completed. plugin={}", pluginId);
    }

    private Path operatePluginFile(Path pluginPath, String sign, boolean back, boolean delete) {
        if (!back && !delete) {
            return null;
        }
        if (pluginPath == null) {
            log.error("{} failed, plugin path is null", sign);
            return null;
        }
        if (!Files.exists(pluginPath)) {
            log.error("{} failed, path not exists: {}", sign, pluginPath);
            return null;
        }

        File sourceFile = pluginPath.toFile();
        try {
            Path targetBackPath = null;
            if (back) {
                touchBackupPath();
                String targetPathStr = configuration.backupPath() + File.separator;
                if (!ObjectUtils.isEmpty(sign)) {
                    targetPathStr = targetPathStr + sign;
                }
                targetPathStr = targetPathStr + "_" + getNowTimeByFormat() + "_" + sourceFile.getName();
                targetBackPath = Paths.get(targetPathStr);
                File targetBackFile = targetBackPath.toFile();
                copyFile(sourceFile, targetBackFile);
                log.info("Backup plugin file to {}", targetBackFile.getAbsolutePath());
            }

            if (delete) {
                if (sourceFile.isFile()) {
                    FileUtils.delete(sourceFile);
                } else {
                    FileUtils.deleteDirectory(sourceFile);
                }
            }
            return targetBackPath;
        } catch (IOException e) {
            log.error("{} path [{}] failed: {}", sign, pluginPath, e.getMessage(), e);
            return null;
        }
    }

    private void copyFile(File sourceFile, File targetFile) throws IOException {
        if (sourceFile.isDirectory()) {
            FileUtils.copyDirectory(sourceFile, targetFile);
        } else if (sourceFile.isFile()) {
            FileUtils.copyFile(sourceFile, targetFile);
        }
    }

    protected void verifyFailureDelete(Path tempPluginFile, Exception e) throws Exception {
        try {
            Files.deleteIfExists(tempPluginFile);
        } catch (IOException ignored) {
            log.error("Delete temporary plugin file failed: {}. {}", tempPluginFile, e.getMessage());
        }
    }

    protected String getNowTimeByFormat() {
        LocalDateTime localDateTime = LocalDateTime.now();
        return FORMAT.format(localDateTime);
    }

    protected void touchBackupPath() throws IOException {
        String backupPath = configuration.backupPath();
        File file = new File(backupPath);
        if (file.exists()) {
            return;
        }
        FileUtils.forceMkdir(file);
    }

    private ClusterLockProvider resolveClusterLockProvider(GenericApplicationContext context,
                                                           IntegrationConfiguration config) {
        String beanName = config.clusterLockProviderBeanName();
        if (!ObjectUtils.isEmpty(beanName)) {
            ClusterLockProvider provider = SpringBeanUtils.getExistBean(context, beanName, ClusterLockProvider.class);
            if (provider != null) {
                return provider;
            }
            log.warn("clusterLockProvider bean not found: {}", beanName);
            return null;
        }

        Map<String, ClusterLockProvider> providerMap = context.getBeansOfType(ClusterLockProvider.class, false, false);
        if (providerMap.size() == 1) {
            return providerMap.values().iterator().next();
        }
        if (providerMap.size() > 1) {
            log.warn("Multiple ClusterLockProvider beans found, set plugin.clusterLockProviderBeanName to choose one");
        }
        return null;
    }

    private PluginAdmissionPipeline createAdmissionPipeline(GenericApplicationContext context,
                                                            IntegrationConfiguration config) {
        List<PluginAdmissionCheck> checks = new ArrayList<>(SpringBeanUtils.getBeans(context, PluginAdmissionCheck.class));
        boolean hasDescriptorCheck = checks.stream().anyMatch(it -> it instanceof PluginDescriptorAdmissionCheck);
        if (!hasDescriptorCheck) {
            checks.add(new PluginDescriptorAdmissionCheck());
        }
        return new PluginAdmissionPipeline(config.pluginAdmissionMode(), checks);
    }
}
