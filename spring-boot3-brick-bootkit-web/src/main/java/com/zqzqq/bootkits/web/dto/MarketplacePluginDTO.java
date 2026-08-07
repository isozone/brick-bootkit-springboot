package com.zqzqq.bootkits.web.dto;

import lombok.Data;

/**
 * 插件市场条目。
 * 描述一个可从插件市场下载安装的插件。
 *
 * @author brick-bootkit
 */
@Data
public class MarketplacePluginDTO {

    private String pluginId;
    private String name;
    private String version;
    private String description;
    private String downloadUrl;
    private Long sizeBytes;
    private boolean installed;
    private String state; // STARTED / STOPPED / 未安装等
}
