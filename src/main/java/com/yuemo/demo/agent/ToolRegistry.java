package com.yuemo.demo.agent;

import com.yuemo.demo.tool.AgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class ToolRegistry {

    private final List<ToolCallback> toolCallbacks;

    public ToolRegistry(List<AgentTool> agentTools) {
        log.info("开始收集 AgentTools... 共发现 {} 个工具", agentTools.size());
        this.toolCallbacks = agentTools.stream()
                .flatMap(tool -> Arrays.stream(ToolCallbacks.from(tool)))
                .peek(callback -> log.info("  - 注册工具: {} ({})",
                    callback.getToolDefinition().name(),
                    callback.getToolDefinition().description()))
                .toList();

        log.info("工具注册完成！共注册 {} 个工具", toolCallbacks.size());
    }

    public ToolCallback[] getToolCallbacksArray() {
        return toolCallbacks.toArray(new ToolCallback[0]);
    }
}