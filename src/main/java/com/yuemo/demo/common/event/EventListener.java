package com.yuemo.demo.common.event;

/**
 * 事件监听器接口
 *
 * <p>事件订阅者的函数式接口，通过 {@link EventBus#subscribe(String, EventListener)} 注册。
 * 当对应类型的事件发布时，EventBus 会调用此接口的实现。</p>
 *
 * @see EventBus 事件总线
 */
@FunctionalInterface
public interface EventListener {

    /**
     * 处理事件
     *
     * @param event 收到的事件，不可为null
     */
    void onEvent(Event event);
}
