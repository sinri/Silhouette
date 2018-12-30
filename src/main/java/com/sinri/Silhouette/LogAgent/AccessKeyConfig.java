package com.sinri.Silhouette.LogAgent;

public class AccessKeyConfig {
    public String accessKeyId;//使用您的阿里云访问密钥 AccessKeyId
    public String accessKeySecret;// as above

    public AccessKeyConfig()
    {}

    public AccessKeyConfig(String accessKeyId,String accessKeySecret){
        this.accessKeyId=accessKeyId;
        this.accessKeySecret=accessKeySecret;
    }
}
