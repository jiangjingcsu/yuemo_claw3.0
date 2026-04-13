package com.yuemo.demo.channel.swing;

/**
 * 通道状态提供者接口
 *
 * <p>定义通道连接状态的查询接口，解耦 UI 层与具体通道实现的依赖。
 * 各通道实现此接口以提供自身的连接状态信息。</p>
 *
 * <p>设计目的：Swing 界面层不应直接依赖 FeishuChannel 等具体实现类，
 * 而是通过此接口获取状态，遵循依赖倒置原则。</p>
 */
public interface ChannelStatusProvider {

    /**
     * 获取通道显示名称
     *
     * @return 通道名称，如 "飞书长连接"、"钉钉"
     */
    String getChannelName();

    /**
     * 查询通道是否已连接
     *
     * @return true 表示连接正常
     */
    boolean isConnected();
}
