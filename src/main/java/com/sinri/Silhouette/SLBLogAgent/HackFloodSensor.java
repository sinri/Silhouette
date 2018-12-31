package com.sinri.Silhouette.SLBLogAgent;

import com.aliyun.openservices.log.common.QueriedLog;
import com.aliyun.openservices.log.response.GetLogsResponse;
import com.sinri.Silhouette.LogAgent.LogAgent;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.StringJoiner;

public class HackFloodSensor {
    private LogAgent logAgent;
    private String project;
    private String logStore;

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

    private Long recentSeconds;
    private Long alertStandard;

    private ArrayList<String> hostList;
    private ArrayList<String> uriList;

    public HackFloodSensor(LogAgent logAgent,String project,String logStore){
        this.logAgent=logAgent;
        this.project=project;
        this.logStore=logStore;
        this.recentSeconds=60L;
        this.alertStandard=10L;
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
        return "* | select client_ip,host,request_uri,count(*) as count " + where + " group by client_ip,host,request_uri order by count desc";
    }

    public ArrayList<ClientUriCountResult> censor() {
        String query = makeQueryString();
        LoggerFactory.getLogger(this.getClass()).debug("Query String: " + query);

        long currentTime = new Date().getTime();
        GetLogsResponse getLogsResponse = logAgent.quickSearchLog(project, logStore, (int) ((currentTime - 1000 * recentSeconds) / 1000), (int) (currentTime / 1000), null, query);
//        System.out.println("Logs count: " + getLogsResponse.GetCount());
        LoggerFactory.getLogger(this.getClass()).debug("Logs count: " + getLogsResponse.GetCount());
        if (getLogsResponse.GetCount() <= 0) {
//            System.out.println("Cannot fetch request groups, it may be a silent spring.");
            LoggerFactory.getLogger(this.getClass()).debug("Cannot fetch request groups, it may be a silent spring.");
            return null;
        }
        ArrayList<ClientUriCountResult> groups = new ArrayList<>();
        getLogsResponse.GetLogs().forEach(queriedLog -> {
            //System.out.println("> "+queriedLog.GetSource()+", "+queriedLog.GetLogItem().GetTime()+": "+queriedLog.GetLogItem().ToJsonString());
            ClientUriCountResult clientUriCountResult = new ClientUriCountResult(queriedLog);
            LoggerFactory.getLogger(this.getClass()).debug("Parsed: "+clientUriCountResult.toString());
            if(Long.parseLong(clientUriCountResult.getCount())>alertStandard) {
                groups.add(clientUriCountResult);
            }
        });

        if(groups.isEmpty()){
//            System.out.println("The world seems peaceful.");
            LoggerFactory.getLogger(this.getClass()).debug("The world seems peaceful.");
            return groups;
        }

        //alert(groups);
        return groups;
    }

    public class ClientUriCountResult {
        HashMap<String,String> entryMap;

        ClientUriCountResult(QueriedLog queriedLog){
            entryMap=new HashMap<>();
            queriedLog.GetLogItem().GetLogContents().forEach(logContent -> {
                entryMap.put(logContent.GetKey(),logContent.GetValue());
            });
        }

        String getLogTime() {
            return entryMap.get("logtime");
        }

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
            return "In this period, " + getClientIP() + " requested " + getHost() + getRequestUri() + " for " + getCount() + " times.";
        }
    }

}
