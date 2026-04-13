package com.yuemo.demo.channel.feishu.business;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuemo.demo.channel.feishu.exception.FeishuException;
import com.yuemo.demo.channel.feishu.model.FeishuEvent;
import com.yuemo.demo.common.event.definitions.MessageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 飞书事件解析器
 *
 * <p>负责将飞书推送的原始 JSON 事件数据解析为 {@link FeishuEvent} 对象，
 * 并提取消息相关的结构化数据（发送者、消息内容等）。</p>
 *
 * <p>注意：当前飞书通道使用 WebSocket 长连接模式接收事件，
 * 消息解析已由 {@link com.yuemo.demo.channel.feishu.FeishuChannel} 直接处理。
 * 此解析器保留用于 Webhook 模式的兼容和后续扩展。</p>
 *
 * @see FeishuEvent 飞书事件数据模型
 */
@Slf4j
@Component
public class FeishuEventParser {

    private final ObjectMapper objectMapper;

    public FeishuEventParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 解析飞书事件请求体
     *
     * <p>从原始 JSON 字符串中提取事件元信息（schema、eventType），
     * 并保留原始请求体供后续解析使用。</p>
     *
     * @param requestBody 飞书推送的原始 JSON 请求体，不可为null或空
     * @return 解析后的事件对象
     * @throws FeishuException JSON 解析失败时抛出
     */
    public FeishuEvent parseEvent(String requestBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(requestBody);
            FeishuEvent event = new FeishuEvent();

            event.setSchema(jsonNode.path("schema").asText());
            event.setEventType(jsonNode.path("header").path("event_type").asText());
            event.setEventBody(requestBody);

            log.info("解析飞书事件 - Schema: {}, EventType: {}",
                    event.getSchema(), event.getEventType());

            return event;
        } catch (Exception e) {
            log.error("解析飞书事件失败", e);
            throw new FeishuException("解析飞书事件失败", e);
        }
    }

    /**
     * 从事件中提取消息数据
     *
     * <p>解析事件原始请求体，提取发送者 openId、消息ID、消息类型和消息内容，
     * 并填充到事件对象中。</p>
     *
     * @param event 飞书事件对象，必须已设置 eventBody，不可为null
     * @throws FeishuException 提取失败时抛出
     */
    public void extractMessageData(FeishuEvent event) {
        try {
            JsonNode jsonNode = objectMapper.readTree(event.getEventBody());
            JsonNode eventNode = jsonNode.path("event");
            JsonNode senderNode = eventNode.path("sender");
            JsonNode messageNode = eventNode.path("message");

            event.setOpenId(senderNode.path("sender_id").path("open_id").asText());
            event.setMessageId(messageNode.path("message_id").asText());

            String messageTypeStr = messageNode.path("message_type").asText();
            try {
                event.setMessageType(MessageType.valueOf(messageTypeStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("未知的消息类型: {}, 使用默认类型 TEXT", messageTypeStr);
                event.setMessageType(MessageType.TEXT);
            }

            event.setMessageContent(messageNode.path("content").asText());

            log.info("提取消息数据 - OpenId: {}, MessageId: {}, Type: {}",
                    event.getOpenId(), event.getMessageId(), event.getMessageType());
        } catch (Exception e) {
            log.error("提取消息数据失败", e);
            throw new FeishuException("提取消息数据失败", e);
        }
    }

    /**
     * 解析消息文本内容
     *
     * <p>根据消息类型解析消息内容，当前仅支持 TEXT 类型。
     * 其他类型返回 null。</p>
     *
     * @param messageType 消息类型
     * @param content     消息原始 JSON 内容字符串
     * @return 解析后的文本内容，不支持的消息类型返回null
     */
    public String parseTextContent(MessageType messageType, String content) {
        try {
            if (MessageType.TEXT.equals(messageType)) {
                JsonNode contentNode = objectMapper.readTree(content);
                String text = contentNode.path("text").asText();
                log.debug("解析文本内容成功: {}", text);
                return text;
            }
            log.warn("不支持的消息类型: {}", messageType);
            return null;
        } catch (Exception e) {
            log.error("解析消息内容失败", e);
            return null;
        }
    }
}
