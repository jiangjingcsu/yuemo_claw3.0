package com.yuemo.demo.common.event.definitions;

import com.yuemo.demo.channel.ChannelType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageContext {

    private String messageId;
    private ChannelType channelType;
    private String userId;
    private String userName;
    private String targetUserId;
    private String content;
    private MessageType messageType;

    public static MessageContext text(ChannelType channelType, String userId, String userName, String content) {
        MessageContext ctx = new MessageContext();
        ctx.channelType = channelType;
        ctx.userId = userId;
        ctx.userName = userName;
        ctx.content = content;
        ctx.messageType = MessageType.TEXT;
        return ctx;
    }
}