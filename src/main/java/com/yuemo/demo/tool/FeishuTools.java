package com.yuemo.demo.tool;

import com.yuemo.demo.channel.ChannelType;
import com.yuemo.demo.common.event.definitions.ChannelEvents;
import com.yuemo.demo.common.event.definitions.MessageContext;
import com.yuemo.demo.common.event.definitions.MessageType;
import com.yuemo.demo.common.event.EventBus;
import com.yuemo.demo.memory.SessionContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.File;

@Slf4j
@Component
public class FeishuTools implements AgentTool {

    private static final String FEISHU_BOT_ID = "assistant";
    private static final String FEISHU_BOT_NAME = "飞书助手";

    private final EventBus eventBus;

    public FeishuTools(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Tool(description = "发送文件到飞书，将本地文件作为附件发送给指定用户")
    public String sendFileToFeishu(
            @ToolParam(description = "要发送的文件路径") String filePath,
            @ToolParam(description = "接收者的飞书 openId（可选，不填则自动发给当前对话用户）", required = false) String userId) {
        String targetOpenId = userId != null && !userId.trim().isEmpty() 
                ? userId.trim() 
                : SessionContextHolder.getCurrentUserId();
        
        if (targetOpenId == null || targetOpenId.isEmpty()) {
            return "错误：无法获取用户 openId，请检查是否通过飞书机器人发送消息";
        }
        
        log.info("发送文件到飞书 - File: {}, UserId: {}", filePath, targetOpenId);

        File file = new File(filePath);
        if (!file.exists()) {
            return "错误：文件不存在 - " + filePath;
        }

        if (!file.isFile()) {
            return "错误：路径不是文件 - " + filePath;
        }

        if (file.length() > 30 * 1024 * 1024) {
            return "错误：文件大小超过 30MB 限制 - " + filePath;
        }

        try {
            MessageContext context = new MessageContext();
            context.setChannelType(ChannelType.FEISHU);
            context.setUserId(FEISHU_BOT_ID);
            context.setUserName(FEISHU_BOT_NAME);
            context.setTargetUserId(targetOpenId);
            context.setMessageType(MessageType.FILE);
            context.setContent(filePath);

            eventBus.publish(ChannelEvents.createToolMessageRequestEvent(context));

            return "成功：文件已发送到飞书 - " + file.getName() + "\n接收者: " + targetOpenId;
        } catch (Exception e) {
            log.error("发送文件到飞书失败", e);
            return "错误：发送文件失败 - " + e.getMessage();
        }
    }

    @Tool(description = "发送文本消息到飞书")
    public String sendTextToFeishu(
            @ToolParam(description = "消息内容") String content,
            @ToolParam(description = "接收者的飞书 openId（可选，不填则自动发给当前对话用户）", required = false) String openId) {
        String targetOpenId = openId != null && !openId.trim().isEmpty() 
                ? openId.trim() 
                : SessionContextHolder.getCurrentUserId();
        
        if (targetOpenId == null || targetOpenId.isEmpty()) {
            return "错误：无法获取用户 openId，请检查是否通过飞书机器人发送消息";
        }
        
        log.info("发送文本到飞书 - OpenId: {}, Content: {}", targetOpenId, content);

        try {
            MessageContext context = MessageContext.text(
                    ChannelType.FEISHU,
                    FEISHU_BOT_ID,
                    FEISHU_BOT_NAME,
                    content
            );
            context.setTargetUserId(targetOpenId);

            eventBus.publish(ChannelEvents.createToolMessageRequestEvent(context));

            return "成功：消息已发送到飞书 - 接收者: " + targetOpenId;
        } catch (Exception e) {
            log.error("发送文本到飞书失败", e);
            return "错误：发送消息失败 - " + e.getMessage();
        }
    }
}
