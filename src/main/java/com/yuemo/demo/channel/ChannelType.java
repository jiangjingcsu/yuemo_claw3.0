package com.yuemo.demo.channel;

/**
 * 通道类型枚举
 *
 * <p>定义系统支持的所有通道类型，用于标识消息来源和路由。
 * 每个枚举值对应一种外部通信渠道。</p>
 *
 * <ul>
 *   <li>FEISHU - 飞书通道，通过 WebSocket 长连接接收事件</li>
 *   <li>DINGTALK - 钉钉通道，通过 Webhook 接收事件</li>
 *   <li>SWING - Swing 桌面界面通道，本地 GUI 交互</li>
 * </ul>
 *
 * <p>注意：Bot 账户信息由具体的 Channel 实现提供，不再放在此枚举中。</p>
 */
public enum ChannelType {

    FEISHU("FEISHU"),
    DINGTALK("DINGTALK"),
    SWING("SWING");

    private final String code;

    ChannelType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ChannelType fromCode(String code) {
        for (ChannelType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的 ChannelType: " + code);
    }
}
