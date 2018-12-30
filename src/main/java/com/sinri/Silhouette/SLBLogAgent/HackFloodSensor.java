package com.sinri.Silhouette.SLBLogAgent;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.log.common.QueriedLog;
import com.aliyun.openservices.log.response.GetLogsResponse;
import com.sinri.Silhouette.LogAgent.AccessKeyConfig;
import com.sinri.Silhouette.LogAgent.LogAgent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

public class HackFloodSensor {
    private LogAgent logAgent;
    private String project;
    private String logStore;

    private String dingtalkRobotUrl;
    private String alertSenderTitle;

    private Long recentSeconds;
    private Long alertStandard;

    public HackFloodSensor(LogAgent logAgent,String project,String logStore){
        this.logAgent=logAgent;
        this.project=project;
        this.logStore=logStore;
        this.recentSeconds=60L;
        this.alertStandard=10L;
    }

    protected Logger getLogger(){
        return LoggerFactory.getLogger(this.getClass());
    }

    public void censor() throws IOException {
        String query = "* | select client_ip,request_uri,count(*)  as count group by client_ip, request_uri order by count desc";

        long currentTime = new Date().getTime();
        GetLogsResponse getLogsResponse = logAgent.quickSearchLog(project, logStore, (int) ((currentTime - 1000 * recentSeconds) / 1000), (int) (currentTime / 1000), null, query);
//        System.out.println("Logs count: " + getLogsResponse.GetCount());
        LoggerFactory.getLogger(this.getClass()).debug("Logs count: " + getLogsResponse.GetCount());
        if (getLogsResponse.GetCount() <= 0) {
//            System.out.println("Cannot fetch request groups, it may be a silent spring.");
            LoggerFactory.getLogger(this.getClass()).debug("Cannot fetch request groups, it may be a silent spring.");
            return;
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
            return;
        }

        alert(groups);
    }

    protected void alert(ArrayList<ClientUriCountResult> groups) throws IOException {
        //System.out.println("HackFloodSensor Alert for flood attack of "+groups.size()+ " types");
        LoggerFactory.getLogger(this.getClass()).warn("HackFloodSensor Alert for flood attack of "+groups.size()+ " types");
        groups.forEach(clientUriCountResult -> {
//            System.out.println(clientUriCountResult.getClientIP());
            LoggerFactory.getLogger(this.getClass()).warn(clientUriCountResult.toString());
        });
        // TODO
        if(dingtalkRobotUrl!=null){
            // Dingtalk Robot
            StringBuilder content=new StringBuilder();
            content.append(groups.size()).append(" flood attacks (> ").append(alertStandard).append(") reported in recent ").append(recentSeconds).append(" seconds.\n\n");
            groups.forEach(clientUriCountResult -> {
                content.append("* ").append(clientUriCountResult.toString()).append("\n");
            });
            content.append("\n").append("> reported on ").append(new Date());

            JSONObject markdownObject=new JSONObject();
            markdownObject.put("title",alertSenderTitle);
            markdownObject.put("text",content);

            JSONObject object = new JSONObject();
            object.put("msgtype","markdown");
            object.put("markdown",markdownObject);

            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(dingtalkRobotUrl);
            httpPost.setHeader("Content-Type","application/json");
            httpPost.setEntity(new StringEntity(object.toJSONString(),"UTF-8"));
            CloseableHttpResponse response2 = httpClient.execute(httpPost);

            LoggerFactory.getLogger(this.getClass()).debug("dingtalk robot api response: "+response2);
        }
    }

    class ClientUriCountResult{
        HashMap<String,String> entryMap;

        ClientUriCountResult(QueriedLog queriedLog){
            entryMap=new HashMap<>();
            queriedLog.GetLogItem().GetLogContents().forEach(logContent -> {
                entryMap.put(logContent.GetKey(),logContent.GetValue());
            });
        }

        public String getLogTime(){
            return entryMap.get("logtime");
        }
        public String getCount(){
            return entryMap.get("count");
        }
        public String getClientIP(){
            return entryMap.get("client_ip");
        }
        public String getRequestUri(){
            return entryMap.get("request_uri");
        }

        @Override
        public String toString() {
            return "In this period, "+getClientIP()+" requested "+getRequestUri()+" for "+getCount()+" times.";
        }
    }

    public static void runTask(Properties properties) throws IOException {
        AccessKeyConfig accessKeyConfig = new AccessKeyConfig(
                properties.getProperty("aliyun.ak.id", ""),
                properties.getProperty("aliyun.ak.secret", "")
        );
        LogAgent logAgent = new LogAgent(accessKeyConfig, properties.getProperty("aliyun.sls.endpoint", ""));
        HackFloodSensor hackFloodSensor = new HackFloodSensor(
                logAgent,
                properties.getProperty("aliyun.sls.project", ""),
                properties.getProperty("aliyun.sls.logstore", "")
        );

        String dintalkRobotUrl = properties.getProperty("option.alert.dingtalk-robot");
        if(dintalkRobotUrl!=null){
            hackFloodSensor.dingtalkRobotUrl=dintalkRobotUrl;
            hackFloodSensor.alertSenderTitle=properties.getProperty("task.name","Anonymous HackFloodSensor for "+properties.getProperty("aliyun.sls.project", "")+":"+properties.getProperty("aliyun.sls.logstore", ""));
        }

        hackFloodSensor.recentSeconds=Long.parseLong(properties.getProperty("option.period-in-second","60"));
        hackFloodSensor.alertStandard=Long.parseLong(properties.getProperty("option.frequency-limit","120"));

        hackFloodSensor.censor();
    }
}
