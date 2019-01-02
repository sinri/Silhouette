# Silhouette
Attack Monitor based on Aliyun SLB Log Service

## Basic Usage

Run as a CLI task for once.

Prepare a properties file to define what task to run and how to run as the following

```bash
java -jar Silhouette.jar -c any.properties
```

Okay now.

## Tasks

### FloodAttackWarnTask

Monitor the Log of Aliyun SLB, with given Aliyun Configuration.
If in certain period, from certain IP called certain request URI too many times, an alert would be reported.
By default, the alert would be written to log file.
If Dingtalk Robot is given, the alert would be sent through it as well.

For more, see Wiki. 