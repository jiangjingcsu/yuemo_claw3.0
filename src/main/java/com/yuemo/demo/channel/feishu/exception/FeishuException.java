package com.yuemo.demo.channel.feishu.exception;

/**
 * 飞书业务异常
 *
 * <p>封装飞书通道中所有业务异常，统一异常处理入口。
 * 支持错误码和错误消息的组合，便于问题定位和分类处理。</p>
 *
 * <p>使用场景：</p>
 * <ul>
 *   <li>配置校验失败（如 AppId 为空）</li>
 *   <li>API 调用失败（如消息发送返回错误码）</li>
 *   <li>长连接建立失败</li>
 * </ul>
 */
public class FeishuException extends RuntimeException {

    /**
     * 错误码，可能为null（无错误码时）
     */
    private final String errorCode;

    /**
     * 构造方法（仅消息）
     *
     * @param message 错误描述信息
     */
    public FeishuException(String message) {
        super(message);
        this.errorCode = null;
    }

    /**
     * 构造方法（消息 + 原始异常）
     *
     * @param message 错误描述信息
     * @param cause   原始异常，不可为null
     */
    public FeishuException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    /**
     * 构造方法（错误码 + 消息）
     *
     * @param errorCode 飞书 API 返回的错误码
     * @param message   错误描述信息
     */
    public FeishuException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 构造方法（错误码 + 消息 + 原始异常）
     *
     * @param errorCode 飞书 API 返回的错误码
     * @param message   错误描述信息
     * @param cause     原始异常，不可为null
     */
    public FeishuException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误码
     *
     * @return 错误码字符串，无错误码时返回null
     */
    public String getErrorCode() {
        return errorCode;
    }
}
