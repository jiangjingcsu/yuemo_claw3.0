package com.yuemo.demo.channel.feishu.config;

import lombok.Data;

@Data
public class FeishuConfig {

    private String appId = "";
    private String appSecret = "";
    private String verificationToken = "";
    private String encryptKey = "";
    private boolean enabled = false;
    private boolean configured = false;

    private String botOpenId = "";

    public boolean isConfigured() {
        return enabled && appId != null && !appId.isBlank() && appSecret != null && !appSecret.isBlank();
    }
}