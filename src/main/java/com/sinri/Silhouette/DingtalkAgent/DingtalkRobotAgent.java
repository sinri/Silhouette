package com.sinri.Silhouette.DingtalkAgent;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DingtalkRobotAgent {
    private String api;

    public DingtalkRobotAgent(String api) {
        this.api = api;
    }

    public void send(String title, String content) throws IOException {
        JSONObject markdownObject = new JSONObject();
        markdownObject.put("title", title);
        markdownObject.put("text", "# " + title + "\n\n" + content);

        JSONObject object = new JSONObject();
        object.put("msgtype", "markdown");
        object.put("markdown", markdownObject);

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(this.api);
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setEntity(new StringEntity(object.toJSONString(), "UTF-8"));
        CloseableHttpResponse response2 = httpClient.execute(httpPost);

        LoggerFactory.getLogger(this.getClass()).debug("dingtalk robot api response: " + response2);
    }
}
