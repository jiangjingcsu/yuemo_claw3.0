package com.yuemo.demo.memory.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserContext {
    private String userId;
    private String currentSessionId;
    private LocalDateTime lastActiveTime;
    private String metadata;
}
