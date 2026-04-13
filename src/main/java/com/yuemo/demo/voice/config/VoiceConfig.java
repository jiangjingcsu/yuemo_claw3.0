package com.yuemo.demo.voice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "voice")
public class VoiceConfig {

    private boolean enabled = true;

    private String sttProvider = "simulated";

    private String ttsProvider = "simulated";

    private String sttApiKey;

    private String ttsApiKey;

    private String sttEndpoint;

    private String ttsEndpoint;

    private String defaultVoice = "zh-CN-female";

    private String language = "zh-CN";

    private int sampleRate = 16000;

    private String audioFormat = "wav";
}