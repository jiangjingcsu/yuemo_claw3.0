package com.yuemo.demo.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间查询工具
 *
 * <p>提供当前系统时间查询功能，供 AI 智能体调用。</p>
 */
@Slf4j
@Component
public class TimeTool implements AgentTool {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取当前时间
     *
     * @param timezone 时区或时间格式（可选，当前未使用）
     * @return 格式化的当前时间字符串
     */
    @Tool(description = "获取当前时间")
    public String getTime(@ToolParam(description = "时区或时间格式，可选") String timezone) {
        log.info("调用时间工具, 时区/格式: {}", timezone);

        LocalDateTime now = LocalDateTime.now();
        String formattedTime = now.format(FORMATTER);

        if (timezone != null && !timezone.isEmpty()) {
            return String.format("当前时间（%s）：%s", timezone, formattedTime);
        }

        return String.format("当前系统时间：%s", formattedTime);
    }
}
