package com.yuemo.demo.memory.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageRecord {
    private String id;
    private String sessionId;
    private String role;
    private String content;
    private Integer tokens;
    private LocalDateTime createdAt;
}
