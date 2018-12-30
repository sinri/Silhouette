package com.sinri.Silhouette.LogAgent;

import com.aliyun.openservices.log.Client;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.request.GetLogsRequest;
import com.aliyun.openservices.log.request.ListLogStoresRequest;
import com.aliyun.openservices.log.response.GetLogsResponse;
import com.aliyun.openservices.log.response.ListLogStoresResponse;
import org.slf4j.LoggerFactory;

public class LogAgent {

    //private AccessKeyConfig accessKeyConfig;

    private Client client;

    public LogAgent(AccessKeyConfig accessKeyConfig,String endpoint){
        //this.accessKeyConfig=accessKeyConfig;
        client = new Client(endpoint,accessKeyConfig.accessKeyId,accessKeyConfig.accessKeySecret);
    }

    /**
     * 列出当前 project 下的所有日志库名称
     * @param project project name
     * @param offset since 0
     * @param size such as 100
     * @param logStoreSubName search, maybe ""
     */
    public ListLogStoresResponse listLogStores(String project, int offset, int size, String logStoreSubName){
        try {
            ListLogStoresRequest req1 = new ListLogStoresRequest(project, offset, size, logStoreSubName);
            return client.ListLogStores(req1);
        } catch (LogException e) {
//            e.printStackTrace();
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
        }

        return null;
    }

    /**
     * GetLogs 接口查询指定 Project 下某个 Logstore 中的日志数据。还可以通过指定相关参数仅查询符合指定条件的日志数据。
     *
     * 当日志写入到 Logstore 中，日志服务的查询接口（GetHistograms 和 GetLogs）能够查到该日志的延时因写入日志类型不同而异。日志服务按日志时间戳把日志分为如下两类：
     *
     * 实时数据：日志中时间点为服务器当前时间点 (-180秒，900秒]。例如，日志时间为 UTC 2014-09-25 12:03:00，服务器收到时为 UTC 2014-09-25 12:05:00，则该日志被作为实时数据处理，一般出现在正常场景下。
     * 历史数据：日志中时间点为服务器当前时间点 [-7 x 86400秒, -180秒)。例如，日志时间为 UTC 2014-09-25 12:00:00，服务器收到时为 UTC 2014-09-25 12:05:00，则该日志被作为历史数据处理，一般出现在补数据场景下。
     *
     * 其中，实时数据写入至可查询的最大延时为3秒（99.9%情况下1秒内即可查询）。
     *
     * @param project project name
     * @param logStore log store name
     * @param fromTimestamp 查询开始时间点（精度为秒，从 1970-1-1 00:00:00 UTC 计算起的秒数）。
     * @param toTimestamp 查询结束时间点（精度为秒，从 1970-1-1 00:00:00 UTC 计算起的秒数）。
     * @param topic 查询日志主题。
     * @param query 查询表达式。关于查询表达式的详细语法，请参考 查询语法。
     * @param offset 请求返回日志的起始点。取值范围为 0 或正整数，默认值为 0。
     * @param maxReturnLines 请求返回的最大日志条数。取值范围为 0~100，默认值为 100。
     * @param reverse 是否按日志时间戳逆序返回日志。true 表示逆序，false 表示顺序，默认值为 false。
     */
    public GetLogsResponse searchLog(String project, String logStore, int fromTimestamp, int toTimestamp, String topic, String query, int offset, int maxReturnLines, boolean reverse){
        try {
            GetLogsRequest getLogsRequest = new GetLogsRequest(project, logStore, fromTimestamp, toTimestamp, topic, query, offset, maxReturnLines, reverse);
            return client.GetLogs(getLogsRequest);
        } catch (LogException e) {
//            e.printStackTrace();
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
        }
        return null;
    }

    /**
     *
     * @param project project name
     * @param logStore log store name
     * @param fromTimestamp 查询开始时间点（精度为秒，从 1970-1-1 00:00:00 UTC 计算起的秒数）。
     * @param toTimestamp 查询结束时间点（精度为秒，从 1970-1-1 00:00:00 UTC 计算起的秒数）。
     * @param topic 查询日志主题。
     * @param query 查询表达式。关于查询表达式的详细语法，请参考 查询语法。
     */
    public GetLogsResponse quickSearchLog(String project, String logStore, int fromTimestamp, int toTimestamp, String topic, String query){
        try {
            GetLogsRequest getLogsRequest = new GetLogsRequest(project, logStore, fromTimestamp, toTimestamp, topic, query);
            return client.GetLogs(getLogsRequest);
        } catch (LogException e) {
//            e.printStackTrace();
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
        }
        return null;
    }

}
