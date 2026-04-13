package com.yuemo.demo.channel;

import com.yuemo.demo.common.event.EventBus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ChannelManager {

    private final List<Channel> channels;
    private final EventBus eventBus;

    public ChannelManager(List<Channel> channels, EventBus eventBus) {
        this.channels = channels;
        this.eventBus = eventBus;
    }

    @PostConstruct
    public void initialize() {
        log.info("初始化 Channel 管理器...");

        if (channels != null && !channels.isEmpty()) {
            log.info("发现 {} 个 Channel", channels.size());
            channels.forEach(channel -> {
                log.info("  - 启动 Channel: {} ({})", channel.getChannelType().getCode(), channel.getChannelType());
                channel.initialize();
                channel.start();
            });
        } else {
            log.warn("没有发现任何 Channel");
        }

        log.info("Channel 管理器初始化完成");
    }

    @PreDestroy
    public void shutdown() {
        log.info("关闭所有 Channel...");
        if (channels != null) {
            channels.forEach(Channel::stop);
        }
        log.info("所有 Channel 已关闭");
    }

    public List<Channel> getAllChannels() {
        return channels;
    }

    public Channel getChannel(ChannelType channelType) {
        if (channels == null) {
            return null;
        }
        return channels.stream()
                .filter(c -> c.getChannelType() == channelType)
                .findFirst()
                .orElse(null);
    }
}
