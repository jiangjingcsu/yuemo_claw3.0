package com.yuemo.demo.gateway;

import com.yuemo.demo.channel.Channel;
import com.yuemo.demo.channel.ChannelManager;
import com.yuemo.demo.channel.ChannelType;
import com.yuemo.demo.common.event.definitions.ChannelEvents;
import com.yuemo.demo.common.event.definitions.MessageContext;
import com.yuemo.demo.common.event.definitions.MessageType;
import com.yuemo.demo.common.event.Event;
import com.yuemo.demo.common.event.EventBus;
import com.yuemo.demo.memory.SessionContextHolder;
import com.yuemo.demo.agent.AgentService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageGateway {

    private final AgentService agentService;
    private final EventBus eventBus;
    private final ChannelManager channelManager;

    public MessageGateway(AgentService agentService, EventBus eventBus, ChannelManager channelManager) {
        this.agentService = agentService;
        this.eventBus = eventBus;
        this.channelManager = channelManager;
    }

    @PostConstruct
    public void init() {
        log.info("初始化 MessageGateway，订阅消息事件...");
        eventBus.subscribe(ChannelEvents.MESSAGE_RECEIVED, this::handleMessageReceivedEvent);
        eventBus.subscribe(ChannelEvents.TOOL_MESSAGE_REQUEST, this::handleToolMessageRequestEvent);
    }

    private void handleMessageReceivedEvent(Event event) {
        if (!(event instanceof ChannelEvents.MessageReceivedEvent msgEvent)) {
            log.warn("事件类型不匹配");
            return;
        }

        MessageContext context = msgEvent.getContext();
        Channel channel = (Channel) msgEvent.getChannel();

        if (context == null || channel == null) {
            log.warn("事件中没有消息数据或通道信息");
            return;
        }

        if (authenticate(context)) {
            log.info("鉴权通过 - 用户: {}, Channel: {}", context.getUserName(), context.getChannelType());
            processMessage(context, channel);
        } else {
            log.warn("鉴权失败 - 用户: {}, Channel: {}", context.getUserName(), context.getChannelType());
            sendResponse(context, channel, "抱歉，您没有权限访问此服务。");
        }
    }

    private void handleToolMessageRequestEvent(Event event) {
        if (!(event instanceof ChannelEvents.ToolMessageRequestEvent toolEvent)) {
            log.warn("事件类型不匹配");
            return;
        }

        MessageContext context = toolEvent.getContext();
        if (context == null) {
            log.warn("工具消息请求中没有消息数据");
            return;
        }

        Channel channel = channelManager.getChannel(context.getChannelType());
        if (channel == null) {
            log.warn("未找到对应通道: {}", context.getChannelType());
            return;
        }

        doSendMessage(channel, context);
    }

    private boolean authenticate(MessageContext context) {
        String userId = context.getUserId();
        ChannelType channelType = context.getChannelType();

        if (userId == null || userId.isEmpty()) {
            log.warn("鉴权失败：用户ID为空");
            return false;
        }

        if (channelType == null) {
            log.warn("鉴权失败：Channel类型为空");
            return false;
        }

        return true;
    }

    private void processMessage(MessageContext context, Channel channel) {
        String sessionId = agentService.getOrCreateCurrentSession(context.getUserId());

        SessionContextHolder.setContext(sessionId, context.getUserId());

        try {
            String response = agentService.processMessage(context.getContent(), sessionId);
            sendResponse(context, channel, response);
        } catch (Exception e) {
            log.error("处理消息时出错", e);
            sendResponse(context, channel, "抱歉，处理您的请求时出现错误：" + e.getMessage());
        } finally {
            SessionContextHolder.clear();
        }
    }

    public void sendResponse(MessageContext originalContext, Channel channel, String response) {
        MessageContext responseContext = new MessageContext();
        responseContext.setMessageId(originalContext.getMessageId() + "_reply");
        responseContext.setChannelType(originalContext.getChannelType());
        responseContext.setUserId(channel.getBotUserId());
        responseContext.setUserName(channel.getBotUserName());
        responseContext.setTargetUserId(originalContext.getUserId());
        responseContext.setContent(response);
        responseContext.setMessageType(MessageType.TEXT);

        doSendMessage(channel, responseContext);
    }

    private void doSendMessage(Channel channel, MessageContext context) {
        if (channel.isRunning()) {
            channel.sendMessage(context);
            eventBus.publish(ChannelEvents.createMessageSentEvent(channel, context));
        } else {
            log.warn("Channel 不可用，无法发送消息: {}", channel.getChannelType());
        }
    }
}
