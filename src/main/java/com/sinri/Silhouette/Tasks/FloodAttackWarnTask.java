package com.sinri.Silhouette.Tasks;

import com.sinri.Silhouette.DingtalkAgent.DingtalkRobotAgent;
import com.sinri.Silhouette.LogAgent.AccessKeyConfig;
import com.sinri.Silhouette.LogAgent.LogAgent;
import com.sinri.Silhouette.SLBLogAgent.HackFloodSensor;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

public class FloodAttackWarnTask extends AbstractTask {

    private Long period;
    private Long alertStandard;

    private ArrayList<String> hosts;
    private ArrayList<String> uris;

    public FloodAttackWarnTask(Properties properties) {
        super(properties);

        period = Long.parseLong(properties.getProperty("option.period-in-second", "60"));
        alertStandard = Long.parseLong(properties.getProperty("option.frequency-limit", "120"));

        String hostsString = properties.getProperty("option.where-host", "");
        hosts = new ArrayList<>(Arrays.asList(hostsString.split("[,\\s]+")));
        hosts.removeIf(String::isEmpty);

        String urisString = properties.getProperty("option.where-uri", "");
        uris = new ArrayList<>(Arrays.asList(urisString.split("[,\\s]+")));
        uris.removeIf(String::isEmpty);
    }

    @Override
    public void runTask() throws Exception {
        AccessKeyConfig accessKeyConfig = new AccessKeyConfig(akId, akSecret);
        LogAgent logAgent = new LogAgent(accessKeyConfig, endpoint);
        HackFloodSensor hackFloodSensor = new HackFloodSensor(logAgent, project, logStore);

        hackFloodSensor.setRecentSeconds(period);
        hackFloodSensor.setAlertStandard(alertStandard);
        hackFloodSensor.setHostList(hosts);
        hackFloodSensor.setUriList(uris);

        ArrayList<HackFloodSensor.ClientUriCountResult> results = hackFloodSensor.censor();

        if (results != null && results.size() > 0) {
            LoggerFactory.getLogger(this.getClass()).warn("HackFloodSensor Alert for flood attack of " + results.size() + " types");
            results.forEach(clientUriCountResult -> LoggerFactory.getLogger(this.getClass()).warn(clientUriCountResult.toString()));

            if (dingtalkRobotUrl != null && !dingtalkRobotUrl.isEmpty()) {
                alertThroughDingtalkRobot(taskName, results);
            }
        }
    }

    private void alertThroughDingtalkRobot(String title, ArrayList<HackFloodSensor.ClientUriCountResult> groups) throws IOException {
        StringBuilder content = new StringBuilder();
        content.append(groups.size()).append(" flood attacks (> ").append(alertStandard).append(") reported in recent ").append(period).append(" seconds.\n\n");
        groups.forEach(clientUriCountResult -> content.append("* ").append(clientUriCountResult.toString()).append("\n"));
        content.append("\n").append("> reported on ").append(new Date());

        (new DingtalkRobotAgent(dingtalkRobotUrl)).send(title, content.toString());
    }
}
