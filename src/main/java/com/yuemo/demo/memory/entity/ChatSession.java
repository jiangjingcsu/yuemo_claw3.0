package com.yuemo.demo.memory.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatSession {
    private String id;
    private String userId;
    private String sessionTitle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isActive;
    private String metadata;
}
