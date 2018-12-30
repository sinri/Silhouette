import com.aliyun.openservices.log.response.GetLogsResponse;
import com.aliyun.openservices.log.response.ListLogStoresResponse;
import com.sinri.Silhouette.LogAgent.AccessKeyConfig;
import com.sinri.Silhouette.LogAgent.LogAgent;
import com.sinri.Silhouette.SLBLogAgent.HackFloodSensor;

import java.io.IOException;
import java.util.Date;

public class HackFloodSensorTest {
    private static AccessKeyConfig getTestAK(){
        AccessKeyConfig accessKeyConfig=new AccessKeyConfig();
        accessKeyConfig.accessKeyId="";
        accessKeyConfig.accessKeySecret="";
        return accessKeyConfig;
    }

    public static void main(String[] args) {
        final String endpoint="cn-hangzhou.log.aliyuncs.com";
        final String project = "x-slb"; // 上面步骤创建的项目名称
        final String logStore = "x-slb-log"; // 上面步骤创建的日志库名称

        LogAgent logAgent = new LogAgent(getTestAK(),endpoint);

        HackFloodSensor hackFloodSensor = new HackFloodSensor(logAgent, project, logStore);
        try {
            hackFloodSensor.censor();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void test1(LogAgent logAgent, String project, String logStore){
        ListLogStoresResponse listLogStoresResponse = logAgent.listLogStores(project, 0, 10, "");
        System.out.println("listLogStoresResponse: "+listLogStoresResponse.GetCount()+ " items");
        if(listLogStoresResponse.GetCount()> 0){
            listLogStoresResponse.GetLogStores().forEach(str->{
                System.out.println("Log Store: "+str);
            });
        }

        String query="* | select client_ip,request_uri,count(*)  as count group by client_ip, request_uri order by count desc limit 10";

        long currentTime = new Date().getTime();
        GetLogsResponse getLogsResponse = logAgent.quickSearchLog(project, logStore, (int) ((currentTime - 60 * 1000 * 5) / 1000), (int) (currentTime / 1000), null, query);
        System.out.println("Logs count: "+getLogsResponse.GetCount());
        if(getLogsResponse.GetCount()>0){
            getLogsResponse.GetLogs().forEach(queriedLog -> {
                System.out.println("> "+queriedLog.GetSource()+", "+queriedLog.GetLogItem().GetTime()+": "+queriedLog.GetLogItem().ToJsonString());
            });
        }
    }
}
