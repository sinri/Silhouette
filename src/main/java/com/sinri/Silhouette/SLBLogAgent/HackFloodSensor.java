package com.sinri.Silhouette.SLBLogAgent;

import com.aliyun.openservices.log.common.QueriedLog;
import com.aliyun.openservices.log.response.GetLogsResponse;
import com.sinri.Silhouette.LogAgent.AliyunLogItem;
import com.sinri.Silhouette.LogAgent.LogAgent;
import org.slf4j.LoggerFactory;

import java.util.*;

public class HackFloodSensor {
    private LogAgent logAgent;
    private String project;
    private String logStore;
    private Long recentSeconds;
    private Long alertStandard;

    public Long getAlertStandardForClientIP() {
        return alertStandardForClientIP;
    }

    public void setAlertStandardForClientIP(Long alertStandardForClientIP) {
        this.alertStandardForClientIP = alertStandardForClientIP;
    }

    private Long alertStandardForClientIP;
    private ArrayList<String> hostList;
    private ArrayList<String> uriList;

    public HackFloodSensor(LogAgent logAgent, String project, String logStore) {
        this.logAgent = logAgent;
        this.project = project;
        this.logStore = logStore;
        this.recentSeconds = 60L;
        this.alertStandard = 10L;
        this.alertStandardForClientIP = 50L;
    }

    public Long getRecentSeconds() {
        return recentSeconds;
    }

    public void setRecentSeconds(Long recentSeconds) {
        this.recentSeconds = recentSeconds;
    }

    public Long getAlertStandard() {
        return alertStandard;
    }

    public void setAlertStandard(Long alertStandard) {
        this.alertStandard = alertStandard;
    }

    public ArrayList<String> getHostList() {
        return hostList;
    }

    public void setHostList(ArrayList<String> hostList) {
        this.hostList = hostList;
    }

    public ArrayList<String> getUriList() {
        return uriList;
    }

    public void setUriList(ArrayList<String> uriList) {
        this.uriList = uriList;
    }

    private String makeQueryString() {
        String where = " where 1=1 ";
        if (hostList != null && hostList.size() > 0) {
            LoggerFactory.getLogger(this.getClass()).debug("hostList size: " + hostList.size());
            StringJoiner stringJoiner = new StringJoiner(",", "'", "'");
            hostList.forEach(stringJoiner::add);
            where += " and host in (" + stringJoiner + ") ";
        }
        if (uriList != null && uriList.size() > 0) {
            LoggerFactory.getLogger(this.getClass()).debug("uriList size: " + uriList.size());
            StringJoiner stringJoiner = new StringJoiner(",", "'", "'");
            uriList.forEach(stringJoiner::add);
            where += " and request_uri in (" + stringJoiner + ") ";
        }
        return "* | " +
                "select client_ip,host,request_uri,count(*) as count " +
                " " + where + " " +
                "group by client_ip,host,request_uri order by count desc";
    }

    public ArrayList<ClientUriCountResult> censor() {
        String query = makeQueryString();
        LoggerFactory.getLogger(this.getClass()).debug("Query String: " + query);

        long currentTime = new Date().getTime();
        GetLogsResponse getLogsResponse = logAgent.quickSearchLog(project, logStore, (int) ((currentTime - 1000 * recentSeconds) / 1000), (int) (currentTime / 1000), null, query);
        LoggerFactory.getLogger(this.getClass()).info("Logs count: " + getLogsResponse.GetCount());
        if (getLogsResponse.GetCount() <= 0) {
            LoggerFactory.getLogger(this.getClass()).info("Cannot fetch request groups, it may be a silent spring.");
            return null;
        }
        ArrayList<ClientUriCountResult> groups = new ArrayList<>();
        getLogsResponse.GetLogs().forEach(queriedLog -> {
            ClientUriCountResult clientUriCountResult = new ClientUriCountResult(queriedLog);
            if(Long.parseLong(clientUriCountResult.getCount())>alertStandard) {
                LoggerFactory.getLogger(this.getClass()).warn("Parsed: " + clientUriCountResult.toString());
                groups.add(clientUriCountResult);
            } else {
                LoggerFactory.getLogger(this.getClass()).debug("Parsed: " + clientUriCountResult.toString());
            }
        });

        if(groups.isEmpty()){
            LoggerFactory.getLogger(this.getClass()).info("The world seems peaceful.");
            return groups;
        }

        return groups;
    }

    public HashMap<String, Integer> censorByClientIP(ArrayList<ClientUriCountResult> results) {
        HashMap<String, Integer> hostMap = new HashMap<>();
        results.forEach(result -> {
            String clientIP = result.getClientIP();
            Integer mapped = hostMap.getOrDefault(clientIP, 0);
            mapped += Integer.parseInt(result.getCount());
            hostMap.put(clientIP, mapped);
        });

        HashMap<String, Integer> clientRequestsHashMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : hostMap.entrySet()) {
            String clientIP = entry.getKey();
            Integer count = entry.getValue();
            if (count > alertStandardForClientIP) {
                LoggerFactory.getLogger(this.getClass()).warn("From " + clientIP + " totally " + count + " came.");
                clientRequestsHashMap.put(clientIP, count);
            }
        }

        return clientRequestsHashMap;
    }

    public class ClientUriCountResult extends AliyunLogItem {

        ClientUriCountResult(QueriedLog queriedLog) {
            super(queriedLog);
        }

//        String getLogTime() {
//            return entryMap.get("logtime");
//        }

        String getCount() {
            return entryMap.get("count");
        }

        String getClientIP() {
            return entryMap.get("client_ip");
        }

        String getRequestUri() {
            return entryMap.get("request_uri");
        }

        String getHost() {
            return entryMap.get("host");
        }

        @Override
        public String toString() {
            return "In this period, " + getClientIP() + " requested " + getHost() + (getRequestUri() != null ? getRequestUri() : "") + " for " + getCount() + " times.";
        }
    }

}
