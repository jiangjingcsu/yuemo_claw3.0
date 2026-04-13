package com.yuemo.demo.channel.dingtalk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuemo.demo.channel.AbstractChannel;
import com.yuemo.demo.channel.ChannelType;
import com.yuemo.demo.channel.swing.ChannelStatusProvider;
import com.yuemo.demo.common.config.ConfigManager;
import com.yuemo.demo.channel.dingtalk.config.DingTalkConfig;
import com.yuemo.demo.common.event.EventBus;
import com.yuemo.demo.common.event.definitions.MessageContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DingTalkChannel extends AbstractChannel implements ChannelStatusProvider {

    private final ConfigManager configManager;
    private final ObjectMapper objectMapper;
    private DingTalkConfig config;

    public DingTalkChannel(ConfigManager configManager, EventBus eventBus) {
        super(eventBus);
        this.configManager = configManager;
        this.objectMapper = new ObjectMapper();
        this.config = objectMapper.convertValue(configManager.getDingTalkConfigMap(), DingTalkConfig.class);
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.DINGTALK;
    }

    @Override
    public String getChannelName() {
        return "钉钉";
    }

    @Override
    public boolean isConnected() {
        return running && config.isEnabled();
    }

    @Override
    public void initialize() {
        log.info("初始化钉钉 Channel...");
        this.config = objectMapper.convertValue(configManager.getDingTalkConfigMap(), DingTalkConfig.class);

        if (!config.isEnabled()) {
            log.info("钉钉 Channel 未启用");
            return;
        }

        if (config.getAppKey() == null || config.getAppKey().isEmpty()) {
            log.warn("未配置钉钉 App Key，功能将不可用。请在配置文件中设置。");
        }
        if (config.getAppSecret() == null || config.getAppSecret().isEmpty()) {
            log.warn("未配置钉钉 App Secret，功能将不可用。请在配置文件中设置。");
        }
        if (config.getRobotWebhook() == null || config.getRobotWebhook().isEmpty()) {
            log.warn("未配置钉钉机器人 Webhook，消息发送功能将不可用。请在配置文件中设置。");
        }

        log.info("钉钉 Channel 初始化完成");
    }

    @Override
    public void start() {
        log.info("启动钉钉 Channel...");
        running = true;

        log.info("钉钉 Channel 已启动（骨架实现，需要完善真实的钉钉 API 集成）");
    }

    @Override
    public void stop() {
        log.info("停止钉钉 Channel...");
        running = false;

        log.info("钉钉 Channel 已停止");
    }

    @Override
    public void sendMessage(MessageContext context) {
        ensureRunning();
        log.info("发送消息到钉钉 - 用户: {}, 内容: {}", context.getUserId(), context.getContent());

        log.warn("钉钉 Channel 发送消息功能尚未实现（骨架）");
    }
}