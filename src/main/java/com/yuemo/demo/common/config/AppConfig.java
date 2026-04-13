package com.yuemo.demo.common.config;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class AppConfig {

    private Map<String, Object> feishu = new HashMap<>();
    private Map<String, Object> dingtalk = new HashMap<>();
    private DatabaseConfig database = new DatabaseConfig();

    @Data
    public static class DatabaseConfig {
        private String path = "workspace/data/memory.db";
    }

    public AppConfig() {
    }
}