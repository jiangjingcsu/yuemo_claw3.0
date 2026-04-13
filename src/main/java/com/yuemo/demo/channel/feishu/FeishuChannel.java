package com.yuemo.demo.channel.feishu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lark.oapi.service.im.v1.model.EventMessage;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1Data;
import com.yuemo.demo.channel.AbstractChannel;
import com.yuemo.demo.channel.ChannelType;
import com.yuemo.demo.channel.feishu.data.FeishuUserRepository;
import com.yuemo.demo.channel.feishu.data.FeishuWebSocketClient;
import com.yuemo.demo.channel.feishu.exception.FeishuException;
import com.yuemo.demo.channel.model.ChatMessage;
import com.yuemo.demo.common.event.definitions.MessageType;
import com.yuemo.demo.channel.swing.ChannelStatusProvider;
import com.yuemo.demo.common.config.ConfigManager;
import com.yuemo.demo.channel.feishu.config.FeishuConfig;
import com.yuemo.demo.common.event.EventBus;
import com.yuemo.demo.common.event.definitions.MessageContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 飞书消息通道
 *
 * <p>基于飞书开放平台 SDK 实现的消息通道，通过 WebSocket 长连接接收飞书事件推送，
 * 通过 REST API 发送消息。无需公网 URL 或内网穿透。</p>
 *
 * <p>核心职责：</p>
 * <ul>
 *   <li>管理飞书通道的生命周期（初始化、启动、停止）</li>
 *   <li>接收飞书消息并转换为统一的 {@link ChatMessage} 格式</li>
 *   <li>发送消息时将 {@link MessageContext} 转换为飞书格式</li>
 *   <li>基于 messageId 进行消息去重，防止飞书重推导致重复处理</li>
 *   <li>通过 {@link FeishuWebSocketClient} 发送消息到飞书</li>
 *   <li>管理飞书用户信息缓存</li>
 * </ul>
 *
 * @see FeishuWebSocketClient WebSocket 长连接客户端
 * @see FeishuUserRepository 用户信息缓存
 * @see FeishuConfig 飞书配置
 */
@Slf4j
@Component
public class FeishuChannel extends AbstractChannel implements ChannelStatusProvider {

    private static final int MAX_PROCESSED_IDS = 1000;

    private final ConfigManager configManager;
    private final FeishuUserRepository userRepository;
    private final FeishuWebSocketClient webSocketClient;
    private final ObjectMapper objectMapper;

    private FeishuConfig config;

    private final Set<String> processedMessageIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public FeishuChannel(ConfigManager configManager,
                       FeishuUserRepository userRepository,
                       FeishuWebSocketClient webSocketClient,
                       EventBus eventBus) {
        super(eventBus);
        this.configManager = configManager;
        this.userRepository = userRepository;
        this.webSocketClient = webSocketClient;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.FEISHU;
    }

    @Override
    public String getBotUserId() {
        return config != null ? config.getBotOpenId() : "assistant";
    }

    @Override
    public String getBotUserName() {
        return "飞书助手";
    }

    @Override
    public void initialize() {
        log.info("初始化飞书 Channel...");
        this.config = objectMapper.convertValue(configManager.getFeishuConfigMap(), FeishuConfig.class);

        if (!config.isEnabled()) {
            log.info("飞书 Channel 未启用");
            return;
        }

        validateConfig();
        log.info("飞书 Channel 初始化完成");
    }

    @Override
    public void start() {
        log.info("启动飞书 Channel...");
        this.config = objectMapper.convertValue(configManager.getFeishuConfigMap(), FeishuConfig.class);

        if (!config.isEnabled()) {
            log.info("飞书 Channel 未启用，跳过启动");
            return;
        }

        running = true;

        try {
            log.info("启动飞书长连接...");
            webSocketClient.start(config, this::handleWebSocketMessage);
            log.info("飞书长连接已启动");
        } catch (Exception e) {
            log.error("启动飞书长连接失败", e);
        }

        log.info("飞书 Channel 已启动");
        log.info("使用长连接方式接收飞书事件，无需公网 URL");
    }

    @Override
    public void stop() {
        log.info("停止飞书 Channel...");
        running = false;

        webSocketClient.stop();
        userRepository.clearCache();
        processedMessageIds.clear();

        log.info("飞书 Channel 已停止");
    }

    @Override
    public void sendMessage(MessageContext context) {
        ensureRunning();

        try {
            String openId = context.getTargetUserId() != null ? context.getTargetUserId() : context.getUserId();
            MessageType messageType = context.getMessageType();
            String content = context.getContent();

            log.info("发送消息到飞书 - OpenId: {}, Type: {}, Content: {}", openId, messageType, content);

            if (MessageType.FILE.equals(messageType)) {
                webSocketClient.sendFileMessage(openId, content);
            } else {
                webSocketClient.sendTextMessage(openId, content);
            }
        } catch (Exception e) {
            log.error("发送消息失败", e);
        }
    }

    @Override
    public String getChannelName() {
        return "飞书长连接";
    }

    @Override
    public boolean isConnected() {
        return webSocketClient.isConnected();
    }

    /**
     * 处理 WebSocket 收到的消息事件
     *
     * <p>解析飞书推送的消息事件，提取发送者信息和消息内容，
     * 基于 messageId 去重后转换为统一的 {@link ChatMessage} 格式分发。</p>
     *
     * @param event 飞书消息接收事件，不可为null
     */
    private void handleWebSocketMessage(P2MessageReceiveV1 event) {
        log.info("收到飞书长连接消息");
        try {
            P2MessageReceiveV1Data eventData = event.getEvent();
            if (eventData == null) {
                log.warn("消息事件中 event data 为空");
                return;
            }

            EventMessage message = eventData.getMessage();
            if (message == null) {
                log.warn("消息事件中 message 为空");
                return;
            }

            String openId = eventData.getSender().getSenderId().getOpenId();
            String messageId = message.getMessageId();
            String messageType = message.getMessageType();
            String content = message.getContent();

            log.info("长连接消息 - OpenId: {}, MessageId: {}, Type: {}", openId, messageId, messageType);

            if (isDuplicateMessage(messageId)) {
                log.warn("重复消息，已跳过 - MessageId: {}", messageId);
                return;
            }

            String textContent = parseTextContent(messageType, content);
            if (textContent == null || textContent.isEmpty()) {
                log.warn("无法解析消息内容");
                return;
            }

            String userName = userRepository.getUserName(openId);

            ChatMessage chatMessage = ChatMessage.createTextMessage(
                    ChannelType.FEISHU,
                    openId,
                    userName,
                    textContent
            );
            chatMessage.setMessageId(messageId);

            userRepository.cacheUser(openId, userName);

            dispatchMessage(chatMessage);

        } catch (Exception e) {
            log.error("处理长连接消息异常", e);
        }
    }

    /**
     * 检查消息是否重复
     *
     * <p>基于 messageId 判断是否已处理过该消息。
     * 飞书在未收到事件确认时会重试推送，导致同一条消息被接收多次。
     * 使用 ConcurrentHashMap 支持并发安全，超出容量时清空重建。</p>
     *
     * @param messageId 消息唯一标识
     * @return true 表示重复消息
     */
    private boolean isDuplicateMessage(String messageId) {
        if (messageId == null || messageId.isEmpty()) {
            return false;
        }

        if (processedMessageIds.contains(messageId)) {
            return true;
        }

        processedMessageIds.add(messageId);

        if (processedMessageIds.size() > MAX_PROCESSED_IDS) {
            log.info("消息去重缓存已满({})，清空重建", processedMessageIds.size());
            processedMessageIds.clear();
            processedMessageIds.add(messageId);
        }

        return false;
    }

    /**
     * 发送消息接收确认回复
     *
     * <p>收到飞书消息后立即回复一条确认消息，告知用户消息已收到正在处理。
     * 同时起到两个作用：</p>
     * <ol>
     *   <li>用户体验：让用户知道消息已被接收，避免重复发送</li>
     *   <li>防重推：飞书收到回复后确认事件已处理，不再重推同一条消息</li>
     * </ol>
     *
     * @param openId 消息发送者的 openId
     */
    private void sendAckMessage(String openId) {
        try {
            String ackText = "收到消息，正在思考中，请稍候… ✨";
            webSocketClient.sendTextMessage(openId, ackText);
            log.debug("已发送确认回复 - OpenId: {}", openId);
        } catch (Exception e) {
            log.warn("发送确认回复失败，不影响消息处理 - OpenId: {}", openId, e);
        }
    }

    private String parseTextContent(String messageType, String content) {
        try {
            if ("text".equals(messageType)) {
                JsonNode contentNode = objectMapper.readTree(content);
                return contentNode.path("text").asText();
            }
            log.warn("不支持的消息类型: {}", messageType);
            return null;
        } catch (Exception e) {
            log.error("解析消息内容失败", e);
            return null;
        }
    }

    private void validateConfig() {
        if (config.getAppId() == null || config.getAppId().isEmpty()) {
            throw new FeishuException("未配置飞书 App ID，请在配置文件中设置");
        }
        if (config.getAppSecret() == null || config.getAppSecret().isEmpty()) {
            throw new FeishuException("未配置飞书 App Secret，请在配置文件中设置");
        }
    }
}
