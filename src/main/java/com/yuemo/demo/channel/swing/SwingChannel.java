package com.yuemo.demo.channel.swing;

import com.yuemo.demo.channel.AbstractChannel;
import com.yuemo.demo.channel.ChannelType;
import com.yuemo.demo.channel.model.ChatMessage;
import com.yuemo.demo.common.config.ConfigManager;
import com.yuemo.demo.common.event.definitions.ChannelEvents;
import com.yuemo.demo.common.event.definitions.MessageContext;
import com.yuemo.demo.common.event.Event;
import com.yuemo.demo.common.event.EventBus;
import com.yuemo.demo.common.event.EventListener;
import com.yuemo.demo.channel.swing.ui.ChatPanel;
import com.yuemo.demo.channel.swing.ui.ConfigPanel;
import com.yuemo.demo.channel.swing.ui.ConnectionStatusPanel;
import com.yuemo.demo.channel.swing.ui.MainFrame;
import com.yuemo.demo.channel.swing.ui.PromptPanel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.util.List;

@Slf4j
@Component
public class SwingChannel extends AbstractChannel implements ChannelStatusProvider {

    private final ConfigManager configManager;
    private final List<ChannelStatusProvider> statusProviders;

    private MainFrame mainFrame;
    private ChatPanel chatPanel;

    public SwingChannel(ConfigManager configManager,
                       List<ChannelStatusProvider> statusProviders,
                       EventBus eventBus) {
        super(eventBus);
        this.configManager = configManager;
        this.statusProviders = statusProviders;
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.SWING;
    }

    @Override
    public String getChannelName() {
        return "Swing 桌面界面";
    }

    @Override
    public boolean isConnected() {
        return running;
    }

    @Override
    public void initialize() {
        log.info("初始化 Swing Channel...");

        chatPanel = new ChatPanel(this::onUserInput);
        ConfigPanel configPanel = new ConfigPanel(configManager, this::onConfigSaved);
        ConnectionStatusPanel connectionPanel = new ConnectionStatusPanel();
        connectionPanel.setStatusProviders(statusProviders);
        PromptPanel promptPanel = new PromptPanel();

        mainFrame = new MainFrame(chatPanel, configPanel, connectionPanel, promptPanel);

        subscribeMessageEvents();

        log.info("Swing Channel 初始化完成");
    }

    @Override
    public void start() {
        log.info("启动 Swing Channel...");
        running = true;
        SwingUtilities.invokeLater(() -> {
            mainFrame.show(ChannelType.SWING.getCode());
            mainFrame.getConnectionStatusPanel().refreshStatus();
        });
    }

    @Override
    public void stop() {
        log.info("停止 Swing Channel...");
        running = false;
        SwingUtilities.invokeLater(() -> mainFrame.dispose());
    }

    @Override
    public void sendMessage(MessageContext context) {
        ensureRunning();
        ChatPanel.MessageSource source = resolveMessageSource(context);
        chatPanel.appendMessage(context.getUserName(), context.getContent(), source);
    }

    private void subscribeMessageEvents() {
        eventBus.subscribe(ChannelEvents.MESSAGE_RECEIVED, new EventListener() {
            @Override
            public void onEvent(Event event) {
                if (event instanceof ChannelEvents.MessageReceivedEvent msgEvent) {
                    MessageContext context = msgEvent.getContext();
                    if (context != null && ChannelType.SWING != context.getChannelType()) {
                        ChatPanel.MessageSource source = resolveMessageSource(context);
                        chatPanel.appendMessage(
                                "[" + context.getChannelType().getCode() + "] " + context.getUserName(),
                                context.getContent(),
                                source);
                    }
                }
            }
        });

        eventBus.subscribe(ChannelEvents.MESSAGE_SENT, new EventListener() {
            @Override
            public void onEvent(Event event) {
                if (event instanceof ChannelEvents.MessageSentEvent sentEvent) {
                    MessageContext context = sentEvent.getContext();
                    if (context != null && ChannelType.SWING != context.getChannelType()) {
                        chatPanel.appendMessage(
                                "[" + context.getChannelType().getCode() + "] " + context.getUserName(),
                                context.getContent(),
                                ChatPanel.MessageSource.ASSISTANT);
                    }
                }
            }
        });
    }

    private void onUserInput(String text) {
        chatPanel.setInputEnabled(false);

        ChatMessage chatMessage = ChatMessage.createTextMessage(
                ChannelType.SWING,
                "user_001",
                "用户",
                text
        );

        dispatchMessage(chatMessage);

        chatPanel.setInputEnabled(true);
    }

    private void onConfigSaved() {
        mainFrame.getConnectionStatusPanel().refreshStatus();
    }

    private ChatPanel.MessageSource resolveMessageSource(MessageContext context) {
        if (context.getChannelType() == null) {
            return ChatPanel.MessageSource.SYSTEM;
        }

        ChannelType channelType = context.getChannelType();
        if (channelType == ChannelType.FEISHU) {
            return ChatPanel.MessageSource.FEISHU;
        }
        if (channelType == ChannelType.DINGTALK) {
            return ChatPanel.MessageSource.DINGTALK;
        }
        if ("ASSISTANT".equalsIgnoreCase(context.getUserId()) || "智能助手".equals(context.getUserName())) {
            return ChatPanel.MessageSource.ASSISTANT;
        }
        return ChatPanel.MessageSource.USER;
    }
}