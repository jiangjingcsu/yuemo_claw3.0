package com.yuemo.demo.channel;

import com.yuemo.demo.channel.model.ChatMessage;
import com.yuemo.demo.common.event.definitions.ChannelEvents;
import com.yuemo.demo.common.event.definitions.MessageContext;
import com.yuemo.demo.common.event.EventBus;
import lombok.extern.slf4j.Slf4j;

/**
 * 通道抽象基类
 *
 * <p>提供通道接口的通用实现，包括消息分发、运行状态管理、事件发布等。
 * 所有具体通道实现（飞书、钉钉、Swing）均继承此类。</p>
 *
 * <p>消息分发机制：收到消息后仅通过 EventBus 发布事件，
 * 由 MessageGateway 统一订阅处理，避免消息重复消费。</p>
 *
 * <p>设计职责：</p>
 * <ul>
 *   <li>负责 ChatMessage → MessageContext 的转换</li>
 *   <li>通过 EventBus 发布事件，不直接调用其他组件</li>
 *   <li>子类负责提供具体的 Bot 账户信息</li>
 * </ul>
 *
 * @see Channel 通道接口
 */
@Slf4j
public abstract class AbstractChannel implements Channel {

    protected final EventBus eventBus;
    protected volatile boolean running = false;

    public AbstractChannel(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public String getBotUserId() {
        return getChannelType().getCode().toLowerCase() + "_bot";
    }

    @Override
    public String getBotUserName() {
        return "智能助手";
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * 分发消息
     *
     * <p>将收到的消息发布到 EventBus，由 MessageGateway 统一订阅处理。
     * 内部将 ChatMessage 转换为 MessageContext，实现 Channel 层与事件层的解耦。</p>
     *
     * @param message 收到的消息，不可为null
     */
    protected void dispatchMessage(ChatMessage message) {
        log.info("Channel 收到消息，发布事件 - Channel: {}, 用户: {}, 内容: {}",
                message.getChannelType(),
                message.getUserName(),
                message.getContent());

        MessageContext context = toMessageContext(message);
        eventBus.publish(ChannelEvents.createMessageReceivedEvent(this, context));
    }

    /**
     * 将 ChatMessage 转换为 MessageContext
     */
    protected MessageContext toMessageContext(ChatMessage message) {
        MessageContext ctx = new MessageContext();
        ctx.setMessageId(message.getMessageId());
        ctx.setChannelType(message.getChannelType());
        ctx.setUserId(message.getUserId());
        ctx.setUserName(message.getUserName());
        ctx.setTargetUserId(message.getTargetUserId());
        ctx.setContent(message.getContent());
        ctx.setMessageType(message.getMessageType());
        return ctx;
    }

    protected void ensureRunning() {
        if (!running) {
            throw new IllegalStateException("Channel 未运行: " + getChannelType().getCode());
        }
    }
}
