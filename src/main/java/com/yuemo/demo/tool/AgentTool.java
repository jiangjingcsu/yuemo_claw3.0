package com.yuemo.demo.tool;

/**
 * 智能体工具标记接口
 *
 * <p>所有可被 AI 智能体调用的工具类均需实现此接口。
 * Spring 容器会自动收集所有实现类，通过 {@link com.yuemo.demo.config.ToolRegistry}
 * 注册为 Spring AI 的 ToolCallback。</p>
 *
 * <p>工具类需配合 {@link org.springframework.ai.tool.annotation.Tool} 和
 * {@link org.springframework.ai.tool.annotation.ToolParam} 注解使用，
 * 以声明工具方法和参数的描述信息。</p>
 *
 * @see com.yuemo.demo.config.ToolRegistry 工具注册中心
 */
public interface AgentTool {
}
