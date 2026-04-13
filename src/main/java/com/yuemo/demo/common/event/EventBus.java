package com.yuemo.demo.common.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 事件总线
 *
 * <p>基于发布-订阅模式的进程内事件总线，用于模块间的解耦通信。
 * 发布者通过 {@link #publish(Event)} 发布事件，
 * 订阅者通过 {@link #subscribe(String, EventListener)} 注册监听器。</p>
 *
 * <p>特性：</p>
 * <ul>
 *   <li>线程安全：使用 ConcurrentHashMap + CopyOnWriteArrayList</li>
 *   <li>同步调用：事件在发布者线程中同步分发</li>
 *   <li>异常隔离：单个监听器异常不影响其他监听器</li>
 * </ul>
 *
 * @see Event 事件接口
 * @see EventListener 事件监听器
 */
@Slf4j
@Component
public class EventBus {

    private final ConcurrentHashMap<String, List<EventListener>> listeners = new ConcurrentHashMap<>();

    public void subscribe(String eventType, EventListener listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
        log.debug("事件监听器已注册 - 类型: {}", eventType);
    }

    public void unsubscribe(String eventType, EventListener listener) {
        List<EventListener> list = listeners.get(eventType);
        if (list != null) {
            list.remove(listener);
            log.debug("事件监听器已移除 - 类型: {}", eventType);
        }
    }

    public void publish(Event event) {
        List<EventListener> list = listeners.get(event.getType());
        if (list == null || list.isEmpty()) {
            log.debug("无监听器 - 事件类型: {}", event.getType());
            return;
        }

        log.debug("发布事件 - 类型: {}, 监听器数: {}", event.getType(), list.size());
        for (EventListener listener : list) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                log.error("事件监听器执行异常 - 类型: {}", event.getType(), e);
            }
        }
    }
}
