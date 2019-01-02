package com.sinri.Silhouette.LogAgent;

import com.aliyun.openservices.log.common.QueriedLog;

import java.util.HashMap;

public class AliyunLogItem {
    protected HashMap<String, String> entryMap;

    public AliyunLogItem(QueriedLog queriedLog) {
        entryMap = new HashMap<>();
        queriedLog.GetLogItem().GetLogContents().forEach(logContent -> {
            entryMap.put(logContent.GetKey(), logContent.GetValue());
        });
    }
}
