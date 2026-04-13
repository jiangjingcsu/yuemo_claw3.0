package com.yuemo.demo.common.event.definitions;

import com.yuemo.demo.common.event.BaseEvent;
import com.yuemo.demo.common.event.EventListener;
import lombok.Getter;

/**
 * 通道相关事件工厂
 *
 * <p>提供通道消息流转过程中的事件定义，用于解耦通道层与上层的消息处理。</p>
 *
 * <p>典型消息流程：</p>
 * <pre>
 * 用户消息 → Channel.dispatchMessage() → MESSAGE_RECEIVED
 *     → MessageGateway.handleMessageReceivedEvent() → 鉴权/处理
 *     → MessageGateway.sendResponse() → Channel.sendMessage()
 *     → MESSAGE_SENT
 * </pre>
 */
public class ChannelEvents {

    public static final String MESSAGE_RECEIVED = "channel.message.received";
    public static final String MESSAGE_SENT = "channel.message.sent";
    public static final String TOOL_MESSAGE_REQUEST = "channel.tool.message.request";

    private ChannelEvents() {
    }

    public static MessageReceivedEvent createMessageReceivedEvent(Object channel, MessageContext context) {
        return new MessageReceivedEvent(channel, context);
    }

    public static MessageSentEvent createMessageSentEvent(Object channel, MessageContext context) {
        return new MessageSentEvent(channel, context);
    }

    public static ToolMessageRequestEvent createToolMessageRequestEvent(MessageContext context) {
        return new ToolMessageRequestEvent(context);
    }

    @Getter
    public static class MessageReceivedEvent extends BaseEvent {
        private final Object channel;
        private final MessageContext context;

        public MessageReceivedEvent(Object channel, MessageContext context) {
            super(MESSAGE_RECEIVED);
            this.channel = channel;
            this.context = context;
        }
    }

    @Getter
    public static class MessageSentEvent extends BaseEvent {
        private final Object channel;
        private final MessageContext context;

        public MessageSentEvent(Object channel, MessageContext context) {
            super(MESSAGE_SENT);
            this.channel = channel;
            this.context = context;
        }
    }

    @Getter
    public static class ToolMessageRequestEvent extends BaseEvent {
        private final MessageContext context;

        public ToolMessageRequestEvent(MessageContext context) {
            super(TOOL_MESSAGE_REQUEST);
            this.context = context;
        }
    }
}
