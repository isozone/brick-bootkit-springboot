package com.zqzqq.bootkits.core.descriptor;

import lombok.Setter;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * 内部的默认插件描述器
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.2
 */
public class DefaultInsidePluginDescriptor extends DefaultPluginDescriptor implements InsidePluginDescriptor {

    @Setter
    private Path pluginPath;
    private String pluginFileName;

    @Setter
    private String pluginClassPath;
    @Setter
    private Properties properties;
    @Setter
    private String configFileName;
    @Setter
    private String configFileLocation;
    @Setter
    private String args;
    @Setter
    private String pluginLibDir;
    @Setter
    private Set<PluginLibInfo> pluginLibInfo;
    @Setter
    private Set<String> includeMainResourcePatterns;
    @Setter
    private Set<String> excludeMainResourcePatterns;

    private String licenseCode;

    private String licenseDesc;

    private Long licenseDateTimeMill;
    private Set<String> excludeAutoConfigurations = new HashSet<>();
    private boolean useMainDataSource = true;
    private String pluginDataSourceConfig;

    public DefaultInsidePluginDescriptor(String pluginId, String pluginVersion, String pluginClass, Path pluginPath) {
        super(pluginId, pluginVersion, pluginClass, pluginPath.toAbsolutePath().toString());
        this.pluginPath = pluginPath;
        this.pluginFileName = pluginPath.toFile().getName();
    }

    @Override
    public void setPluginPath(Path pluginPath) {
        this.pluginPath = pluginPath;
        this.pluginFileName = pluginPath.toFile().getName();
        // 更新父类的路径
        super.setPluginPath(pluginPath.toAbsolutePath().toString());
    }

    @Override
    public String getPluginClassPath() {
        return pluginClassPath;
    }

    @Override
    public String getPluginLibDir() {
        return pluginLibDir;
    }

    @Override
    public Set<PluginLibInfo> getPluginLibInfo() {
        return pluginLibInfo;
    }

    @Override
    public Set<String> getIncludeMainResourcePatterns() {
        return includeMainResourcePatterns;
    }

    @Override
    public Set<String> getExcludeMainResourcePatterns() {
        return excludeMainResourcePatterns;
    }

    @Override
    public String getConfigFileName() {
        return configFileName;
    }

    @Override
    public String getConfigFileLocation() {
        return configFileLocation;
    }

    @Override
    public String getArgs() {
        return args;
    }

    @Override
    public Path getInsidePluginPath() {
        return pluginPath;
    }

    @Override
    public String getPluginFileName() {
        return pluginFileName;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public PluginDescriptor toPluginDescriptor() {
        Path pluginPath = getInsidePluginPath();
        if(getType() == PluginType.DEV) {
            // dev模式 插件璺緞灞曠ず椤圭洰鐩綍
            pluginPath = pluginPath.getParent().getParent();
        }
        DefaultPluginDescriptor descriptor = new DefaultPluginDescriptor(
                getPluginId(), getPluginVersion(), getPluginBootstrapClass(), pluginPath.toAbsolutePath().toString()
        );
        descriptor.setType(getType());
        descriptor.setDescription(getDescription());
        descriptor.setProvider(getProvider());
        descriptor.setRequires(getRequires());
        descriptor.setLicense(getLicense());
        descriptor.setLicenseCode(getLicenseCode());
        descriptor.setLicenseDesc(getLicenseDesc());
        descriptor.setLicenseDateMill(getLicenseDateMill());
        return descriptor;
    }

    @Override
    public void setLicenseCode(String code) {
        this.licenseCode = code;
    }

    @Override
    public void setLicenseDesc(String desc) {
       this.licenseDesc = desc;
    }

    @Override
    public void setLicenseDateMill(Long mills) {
      this.licenseDateTimeMill = mills;
    }

    @Override
    public String getLicenseCode() {
        return this.licenseCode;
    }

    @Override
    public String getLicenseDesc() {
        return this.licenseDesc;
    }

    @Override
    public Long getLicenseDateMill() {
        return this.licenseDateTimeMill;
    }

    /**
     * 是否使用主应用数据源
     * @return true使用主数据源，false使用插件独立数据源
     */
    public boolean isUseMainDataSource() {
        return useMainDataSource;
    }

    /**
     * 设置是否使用主应用数据源
     * @param useMainDataSource true使用涓绘暟鎹簮锛宖alse使用插件鐙珛数据婧?
     */
    public void setUseMainDataSource(boolean useMainDataSource) {
        this.useMainDataSource = useMainDataSource;
    }

    /**
     * 获取插件数据源配置(JSON格式)
     * @return 数据源配置
     */
    public String getPluginDataSourceConfig() {
        return pluginDataSourceConfig;
    }

    /**
     * 设置插件数据源配置JSON鏍煎紡)
     * @param config 数据源配置
     */
    public void setPluginDataSourceConfig(String config) {
        this.pluginDataSourceConfig = config;
        if(config != null && !config.trim().isEmpty()){
            this.useMainDataSource = false;
        }
    }

    @Override
    public Set<String> getExcludeAutoConfigurations() {
        return excludeAutoConfigurations;
    }

    @Override
    public void setExcludeAutoConfigurations(Set<String> excludeClasses) {
        this.excludeAutoConfigurations = excludeClasses != null ? 
            new HashSet<>(excludeClasses) : new HashSet<>();
    }

    // 手动添加setter方法以确保编译通过
    public void setPluginLibDir(String pluginLibDir) {
        this.pluginLibDir = pluginLibDir;
    }

    public void setPluginLibInfo(Set<PluginLibInfo> pluginLibInfo) {
        this.pluginLibInfo = pluginLibInfo != null ? 
            new HashSet<>(pluginLibInfo) : new HashSet<>();
    }

    public void setIncludeMainResourcePatterns(Set<String> includeMainResourcePatterns) {
        this.includeMainResourcePatterns = includeMainResourcePatterns != null ? 
            new HashSet<>(includeMainResourcePatterns) : new HashSet<>();
    }

    public void setExcludeMainResourcePatterns(Set<String> excludeMainResourcePatterns) {
        this.excludeMainResourcePatterns = excludeMainResourcePatterns != null ? 
            new HashSet<>(excludeMainResourcePatterns) : new HashSet<>();
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void setPluginClassPath(String pluginClassPath) {
        this.pluginClassPath = pluginClassPath;
    }

    public void setConfigFileName(String configFileName) {
        this.configFileName = configFileName;
    }

    public void setConfigFileLocation(String configFileLocation) {
        this.configFileLocation = configFileLocation;
    }

    public void setArgs(String args) {
        this.args = args;
    }
}