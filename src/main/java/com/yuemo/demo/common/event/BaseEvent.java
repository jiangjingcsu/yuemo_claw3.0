package com.yuemo.demo.common.event;

import lombok.Getter;

/**
 * 事件基类
 *
 * <p>提供事件的通用属性实现，包括事件类型和时间戳。
 * 所有具体事件类均应继承此类。</p>
 *
 * @see Event 事件接口
 */
@Getter
public abstract class BaseEvent implements Event {

    private final String type;
    private final long timestamp;

    protected BaseEvent(String type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }
}
