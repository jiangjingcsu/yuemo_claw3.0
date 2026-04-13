package com.yuemo.demo;

import com.yuemo.demo.channel.ChannelManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 多智能体聊天系统主启动类
 *
 * <p>基于 Spring Boot 3 和 Spring AI Alibaba 构建的多智能体聊天系统。
 * 支持飞书、钉钉、Swing 桌面等多种消息通道。</p>
 *
 * <p>系统架构：</p>
 * <ul>
 *   <li>通道层（Channel）：飞书/钉钉/Swing 等消息通道</li>
 *   <li>网关层（Gateway）：消息路由、鉴权、分发</li>
 *   <li>服务层（Service）：AI 智能体处理核心</li>
 *   <li>工具层（Tool）：可被 AI 调用的各种工具</li>
 * </ul>
 */
@Slf4j
@SpringBootApplication
public class SpringAiAlibabaDemoApplication {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        log.info("启动多智能体聊天系统...");
        SpringApplication.run(SpringAiAlibabaDemoApplication.class, args);
        log.info("多智能体聊天系统启动完成！");
    }
}
