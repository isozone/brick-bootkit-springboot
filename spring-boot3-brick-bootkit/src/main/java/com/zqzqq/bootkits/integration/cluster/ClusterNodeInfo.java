package com.zqzqq.bootkits.integration.cluster;

/**
 * 集群节点信息。
 *
 * @author brick-bootkit
 */
public class ClusterNodeInfo {

    private String nodeId;
    private String host;
    private long startedAt;
    private long lastHeartbeat;
    private int pluginCount;
    private String status; // ONLINE / OFFLINE

    public ClusterNodeInfo() {
    }

    public ClusterNodeInfo(String nodeId, String host, long startedAt, long lastHeartbeat,
                           int pluginCount, String status) {
        this.nodeId = nodeId;
        this.host = host;
        this.startedAt = startedAt;
        this.lastHeartbeat = lastHeartbeat;
        this.pluginCount = pluginCount;
        this.status = status;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(long lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public int getPluginCount() {
        return pluginCount;
    }

    public void setPluginCount(int pluginCount) {
        this.pluginCount = pluginCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
