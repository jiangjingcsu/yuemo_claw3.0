package com.yuemo.demo.common.event.definitions;

import com.yuemo.demo.common.event.BaseEvent;
import lombok.Getter;

/**
 * Agent 相关事件工厂
 *
 * <p>提供 Agent 处理消息流程中的事件定义，用于跟踪 Agent 的处理状态。</p>
 */
public class AgentEvents {

    public static final String AGENT_PROCESS_START = "agent.process.start";
    public static final String AGENT_PROCESS_COMPLETE = "agent.process.complete";
    public static final String AGENT_PROCESS_ERROR = "agent.process.error";

    private AgentEvents() {
    }

    public static AgentProcessEvent createProcessStartEvent(MessageContext context) {
        return new AgentProcessEvent(AGENT_PROCESS_START, context);
    }

    public static AgentProcessEvent createProcessCompleteEvent(MessageContext context, String response) {
        return new AgentProcessEvent(AGENT_PROCESS_COMPLETE, context, response);
    }

    public static AgentProcessEvent createProcessErrorEvent(MessageContext context, String error) {
        return new AgentProcessEvent(AGENT_PROCESS_ERROR, context, error);
    }

    @Getter
    public static class AgentProcessEvent extends BaseEvent {

        private final MessageContext context;
        private final String response;
        private final String error;

        public AgentProcessEvent(String type, MessageContext context) {
            super(type);
            this.context = context;
            this.response = null;
            this.error = null;
        }

        public AgentProcessEvent(String type, MessageContext context, String payload) {
            super(type);
            this.context = context;
            if (AGENT_PROCESS_COMPLETE.equals(type)) {
                this.response = payload;
                this.error = null;
            } else {
                this.response = null;
                this.error = payload;
            }
        }
    }
}
