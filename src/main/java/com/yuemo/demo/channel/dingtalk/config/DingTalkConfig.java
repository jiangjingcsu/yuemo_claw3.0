package com.yuemo.demo.channel.dingtalk.config;

import lombok.Data;

@Data
public class DingTalkConfig {

    private String appKey = "";
    private String appSecret = "";
    private String robotWebhook = "";
    private String webhook = "";
    private String secret = "";
    private boolean enabled = false;
    private boolean configured = false;

    public boolean isConfigured() {
        return enabled && ((webhook != null && !webhook.isBlank()) || (robotWebhook != null && !robotWebhook.isBlank()));
    }
}