package com.sinri.Silhouette;

import com.sinri.Silhouette.SLBLogAgent.HackFloodSensor;
import org.apache.commons.cli.*;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class Silhouette {
    public static void main(String[] args){
        Options ops = new Options();
        ops.addOption("help", "Display help information");
        ops.addOption("c",true,"Configuration File Path");

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

            runFollowConfig(properties);
        } catch (Exception e) {
            LoggerFactory.getLogger(Silhouette.class).error(e.getMessage(),e);
        }
    }

    private static void runFollowConfig(Properties properties) throws Exception {
        String taskType = properties.getProperty("task.type");
        if(taskType==null || taskType.isEmpty()){
            throw new Exception("Property task.type is not defined.");
        }
        String taskName = properties.getProperty("task.name");
        if(taskName==null || taskName.isEmpty()){
            throw new Exception("Property task.type is not defined.");
        }
        if(taskType.equals("HackFloodSensor")){
            HackFloodSensor.runTask(properties);
        }
    }

    private static void help(Options ops) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("options", ops);
    }

}
