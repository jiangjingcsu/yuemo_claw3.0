package com.yuemo.demo.channel.model;

import com.yuemo.demo.channel.ChannelType;
import com.yuemo.demo.common.event.definitions.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    private String messageId;

    private ChannelType channelType;

    private String userId;

    private String targetUserId;

    private String userName;

    private String content;

    private MessageType messageType;

    private LocalDateTime timestamp;

    public static ChatMessage createTextMessage(ChannelType channelType,
                                                String userId, String userName, String content) {
        return ChatMessage.builder()
                .channelType(channelType)
                .userId(userId)
                .userName(userName)
                .content(content)
                .messageType(MessageType.TEXT)
                .timestamp(LocalDateTime.now())
                .build();
    }
}