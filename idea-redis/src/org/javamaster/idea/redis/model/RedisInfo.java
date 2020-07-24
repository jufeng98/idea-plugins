package org.javamaster.idea.redis.model;

/**
 * @author yudong
 * @date 2020/7/23
 */
public class RedisInfo {
    private String hosts;
    private String ports;
    private String password;

    private boolean def;
    private boolean cluster;

    public RedisInfo() {
    }

    public RedisInfo(String hosts, String ports, String password) {
        this.hosts = hosts;
        this.ports = ports;
        this.password = password;
    }

    public String getHosts() {
        return hosts;
    }

    public void setHosts(String hosts) {
        this.hosts = hosts;
    }

    public String getPorts() {
        return ports;
    }

    public void setPorts(String ports) {
        this.ports = ports;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isDef() {
        return def;
    }

    public void setDef(boolean def) {
        this.def = def;
    }

    public boolean isCluster() {
        return cluster;
    }

    public void setCluster(boolean cluster) {
        this.cluster = cluster;
    }
}
