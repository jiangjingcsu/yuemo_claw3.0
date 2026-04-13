package com.yuemo.demo.channel;

import com.yuemo.demo.common.event.definitions.MessageContext;

/**
 * 消息通道接口
 *
 * <p>定义消息通道的统一抽象，所有通道实现（飞书、钉钉、Swing等）均需实现此接口。
 * 通道负责与外部系统建立连接、收发消息，是系统与外部通信的唯一入口。</p>
 *
 * <p>生命周期：initialize() → start() → [运行中] → stop()</p>
 *
 * <p>设计原则：</p>
 * <ul>
 *   <li>Channel 接口使用 MessageContext，与具体消息模型解耦</li>
 *   <li>Channel 层内部实现负责 MessageContext → 平台消息格式的转换</li>
 *   <li>Bot 账户信息由具体 Channel 实现提供</li>
 * </ul>
 *
 * @see AbstractChannel 抽象基类，提供通用实现
 * @see ChannelType 通道类型枚举
 */
public interface Channel {

    /**
     * 获取通道类型
     *
     * @return 通道类型枚举值
     */
    ChannelType getChannelType();

    /**
     * 获取 Bot 用户ID
     *
     * <p>返回当前通道的 Bot 用户标识，用于发送消息时标识发送者。</p>
     *
     * @return Bot 用户ID
     */
    String getBotUserId();

    /**
     * 获取 Bot 用户名
     *
     * @return Bot 用户名
     */
    String getBotUserName();

    /**
     * 初始化通道
     *
     * <p>在通道启动前调用，用于加载配置、初始化客户端等。
     * 此方法不应阻塞，耗时操作应放在 start() 中。</p>
     */
    void initialize();

    /**
     * 启动通道
     *
     * <p>建立与外部系统的连接，开始接收消息。
     * 调用前必须先调用 initialize()。</p>
     */
    void start();

    /**
     * 停止通道
     *
     * <p>断开与外部系统的连接，释放资源。
     * 调用后通道不再接收和发送消息。</p>
     */
    void stop();

    /**
     * 发送消息到通道
     *
     * <p>接收 MessageContext，转换为目标平台的格式发送。</p>
     *
     * @param context 消息上下文，不可为null
     * @throws IllegalStateException 通道未运行时调用
     */
    void sendMessage(MessageContext context);

    /**
     * 检查通道是否正在运行
     *
     * @return true 表示通道已启动且正在运行
     */
    boolean isRunning();
}
