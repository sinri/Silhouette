package com.sinri.Silhouette;

import com.sinri.Silhouette.Tasks.FloodAttackWarnTask;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

public class Silhouette {
    public static void main(String[] args){
        Options ops = new Options();
        ops.addOption("help", "Display help information");
        ops.addOption("c",true,"Configuration File Path");
        ops.addOption("d", "Run in Daemon Mode");

        try {
            CommandLine options = new DefaultParser().parse(ops, args);

            if (options.hasOption("help")) {
                help(ops);
                return;
            }

            if(!options.hasOption("c")){
                LoggerFactory.getLogger(Silhouette.class).error("Config Parameter Missing, die.");
                help(ops);
                return;
            }

            String configPath = options.getOptionValue('c');
            File configFile = new File(configPath);
            if(!configFile.exists()){
                LoggerFactory.getLogger(Silhouette.class).error("Config File Missing: "+configFile.getAbsolutePath());
                return;
            }
            if(!configFile.canRead()){
                LoggerFactory.getLogger(Silhouette.class).error("Cannot Read Config File: "+configFile.getAbsolutePath());
                return;
            }

            Properties properties = new Properties();
            properties.load(new FileReader(configFile));

            runFollowConfig(properties, ops.hasOption("d"));
        } catch (Exception e) {
            LoggerFactory.getLogger(Silhouette.class).error(e.getMessage(),e);
        }
    }

    private static void runFollowConfig(Properties properties, Boolean daemonMode) throws Exception {
        String taskType = properties.getProperty("task.type");
        if(taskType==null || taskType.isEmpty()){
            throw new Exception("Property task.type is not defined.");
        }
        String taskName = properties.getProperty("task.name");
        if(taskName==null || taskName.isEmpty()){
            throw new Exception("Property task.type is not defined.");
        }
        if (taskType.equals("FloodAttackWarnTask")) {
            if (daemonMode) {
                (new FloodAttackWarnTask(properties)).runTaskInDaemonMode();
            } else {
                (new FloodAttackWarnTask(properties)).runTask();
            }
        }
    }

    private static void help(Options ops) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("options", ops);
    }

}
