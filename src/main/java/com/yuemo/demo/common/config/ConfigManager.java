package com.yuemo.demo.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Slf4j
@Component
public class ConfigManager {

    private static final String CONFIG_FILE = "workspace/config.json";
    private final ObjectMapper objectMapper;
    private AppConfig config;

    public ConfigManager() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        loadConfig();
    }

    public AppConfig getConfig() {
        return config;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getFeishuConfigMap() {
        return config.getFeishu();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getDingTalkConfigMap() {
        return config.getDingtalk();
    }

    public void saveConfig() {
        try {
            Path configPath = Paths.get(CONFIG_FILE);
            Path parentDir = configPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            objectMapper.writeValue(configPath.toFile(), config);
            log.info("配置已保存到: {}", CONFIG_FILE);
        } catch (IOException e) {
            log.error("保存配置失败", e);
            throw new RuntimeException("保存配置失败", e);
        }
    }

    private void loadConfig() {
        try {
            File configFile = new File(CONFIG_FILE);
            log.info("尝试加载配置文件: {}, 文件存在: {}", configFile.getAbsolutePath(), configFile.exists());

            if (configFile.exists()) {
                config = objectMapper.readValue(configFile, AppConfig.class);
                log.info("配置已从文件加载: {}", CONFIG_FILE);
            } else {
                config = new AppConfig();
                saveConfig();
                log.info("创建新的默认配置文件: {}", CONFIG_FILE);
            }
        } catch (IOException e) {
            log.warn("加载配置失败，使用默认配置", e);
            config = new AppConfig();
        }
    }

    @SuppressWarnings("unchecked")
    public void updateFeishuConfig(Map<String, Object> feishuConfig) {
        config.setFeishu(feishuConfig);
        saveConfig();
    }

    @SuppressWarnings("unchecked")
    public void updateDingTalkConfig(Map<String, Object> dingTalkConfig) {
        config.setDingtalk(dingTalkConfig);
        saveConfig();
    }

    public void reloadConfig() {
        loadConfig();
        log.info("配置已重新加载");
    }
}