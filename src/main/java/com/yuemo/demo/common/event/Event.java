package com.yuemo.demo.common.event;

/**
 * 事件接口
 *
 * <p>系统中所有事件的顶层抽象接口。
 * 事件用于模块间的解耦通信，发布者无需知道订阅者的存在。</p>
 *
 * <p>事件通过 {@link EventBus} 发布和订阅，
 * 典型场景包括：通道消息接收、Agent 响应完成等。</p>
 *
 * @see BaseEvent 事件基类
 * @see EventBus 事件总线
 */
public interface Event {

    /**
     * 获取事件类型标识
     *
     * <p>用于事件订阅者按类型过滤事件，
     * 如 "channel.message.received"。</p>
     *
     * @return 事件类型字符串，不可为null
     */
    String getType();

    /**
     * 获取事件发生时间戳
     *
     * @return 事件创建时的毫秒时间戳
     */
    long getTimestamp();
}
