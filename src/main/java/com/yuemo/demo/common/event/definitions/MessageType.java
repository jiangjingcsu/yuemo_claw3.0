package com.yuemo.demo.common.event.definitions;

/**
 * 消息类型枚举
 *
 * <p>定义系统支持的消息类型，用于标识消息内容的格式。
 * 作为公共类型，被 MessageContext 和 Channel 层共同使用。</p>
 */
public enum MessageType {

    /**
     * 纯文本消息
     */
    TEXT,

    /**
     * 图片消息
     */
    IMAGE,

    /**
     * 文件消息
     */
    FILE,

    /**
     * 富文本消息（包含格式化内容）
     */
    RICH_TEXT,

    /**
     * 未知类型消息
     */
    UNKNOWN
}
