package com.yuemo.demo.channel.feishu.model;

import com.yuemo.demo.common.event.definitions.MessageType;
import lombok.Data;

/**
 * 飞书事件数据模型
 *
 * <p>封装飞书开放平台推送的事件数据，包括事件元信息和消息内容。
 * 用于在飞书通道内部传递和解析事件数据。</p>
 *
 * <p>事件生命周期：飞书推送 → WebSocket 接收 → FeishuEvent 封装 → 业务处理</p>
 *
 * @see com.yuemo.demo.channel.feishu.business.FeishuEventParser 事件解析器
 */
@Data
public class FeishuEvent {

    /**
     * 事件协议版本（如 "2.0" 表示 v2 协议）
     */
    private String schema;

    /**
     * 事件类型标识（如 "im.message.receive_v1" 表示消息接收事件）
     */
    private String eventType;

    /**
     * 事件原始 JSON 请求体，用于延迟解析
     */
    private String eventBody;

    /**
     * 消息发送者的 openId
     */
    private String openId;

    /**
     * 消息唯一标识
     */
    private String messageId;

    /**
     * 消息类型
     */
    private MessageType messageType;

    /**
     * 消息内容（原始 JSON 字符串）
     */
    private String messageContent;

    /**
     * 判断是否为 v2 协议事件
     *
     * @return true 表示 v2 协议（schema 为 "2.0"）
     */
    public boolean isV2Event() {
        return "2.0".equals(schema);
    }

    /**
     * 判断是否为消息接收事件
     *
     * @return true 表示为 im.message.receive_v1 事件
     */
    public boolean isMessageReceiveEvent() {
        return "im.message.receive_v1".equals(eventType);
    }
}
