package com.sinri.Silhouette.Tasks;

import java.util.Properties;

abstract public class AbstractTask {
    private Properties properties;

    String akId;
    String akSecret;
    String endpoint;
    String project;
    String logStore;
    String taskName;
    String dingtalkRobotUrl;

    AbstractTask(Properties properties) {
        this.properties = properties;

        akId = properties.getProperty("aliyun.ak.id", "");
        akSecret = properties.getProperty("aliyun.ak.secret", "");

        endpoint = properties.getProperty("aliyun.sls.endpoint", "");
        project = properties.getProperty("aliyun.sls.project", "");
        logStore = properties.getProperty("aliyun.sls.logstore", "");

        dingtalkRobotUrl = properties.getProperty("option.alert.dingtalk-robot");

        taskName = properties.getProperty("task.name", "Anonymous HackFloodSensor for " + project + ":" + logStore);
    }

    abstract public void runTask() throws Exception;

    abstract public void runTaskInDaemonMode() throws Exception;

    protected Properties getProperties() {
        return properties;
    }
}
