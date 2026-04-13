package com.yuemo.demo.voice;

import com.yuemo.demo.memory.SessionContextHolder;
import com.yuemo.demo.voice.config.VoiceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.*;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
public class VoiceService {

    private final VoiceConfig voiceConfig;
    private static final String VOICE_DIR = "workspace/voice";
    private static final String TEMP_DIR = VOICE_DIR + "/temp";

    public VoiceService(VoiceConfig voiceConfig) {
        this.voiceConfig = voiceConfig;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(VOICE_DIR));
            Files.createDirectories(Paths.get(TEMP_DIR));
            log.info("语音服务初始化完成");
            log.info("  - 语音目录: {}", VOICE_DIR);
            log.info("  - 临时目录: {}", TEMP_DIR);
            log.info("  - STT 提供商: {}", voiceConfig.getSttProvider());
            log.info("  - TTS 提供商: {}", voiceConfig.getTtsProvider());
        } catch (IOException e) {
            log.error("语音服务初始化失败", e);
        }
    }

    public String speechToText(File audioFile) {
        return speechToText(audioFile, null);
    }

    public String speechToText(File audioFile, String language) {
        log.info("语音转文字 - 文件: {}, 语言: {}", audioFile.getName(), language);

        if (!audioFile.exists()) {
            return "错误：音频文件不存在";
        }

        String lang = language != null ? language : voiceConfig.getLanguage();

        try {
            switch (voiceConfig.getSttProvider().toLowerCase()) {
                case "simulated":
                    return simulateStt(audioFile, lang);
                case "aliyun":
                    return aliyunStt(audioFile, lang);
                case "openai":
                case "whisper":
                    return openaiStt(audioFile, lang);
                default:
                    return simulateStt(audioFile, lang);
            }
        } catch (Exception e) {
            log.error("语音识别失败", e);
            return "语音识别失败: " + e.getMessage();
        }
    }

    public File textToSpeech(String text) {
        return textToSpeech(text, null);
    }

    public File textToSpeech(String text, String voice) {
        log.info("文字转语音 - 文本长度: {}, 声音: {}", text.length(), voice);

        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        String voiceName = voice != null ? voice : voiceConfig.getDefaultVoice();

        try {
            switch (voiceConfig.getTtsProvider().toLowerCase()) {
                case "simulated":
                    return simulateTts(text, voiceName);
                case "aliyun":
                    return aliyunTts(text, voiceName);
                case "openai":
                    return openaiTts(text, voiceName);
                default:
                    return simulateTts(text, voiceName);
            }
        } catch (Exception e) {
            log.error("语音合成失败", e);
            return null;
        }
    }

    private String simulateStt(File audioFile, String language) {
        log.info("使用模拟模式进行语音识别");
        String transcript = "【模拟语音识别结果】这是一段模拟的语音转文字结果。";
        return transcript;
    }

    private String aliyunStt(File audioFile, String language) {
        log.info("使用阿里云进行语音识别");
        return "【阿里云语音识别】演示模式 - 实际需要配置阿里云 API Key";
    }

    private String openaiStt(File audioFile, String language) {
        log.info("使用 OpenAI Whisper 进行语音识别");
        return "【Whisper 语音识别】演示模式 - 实际需要配置 OpenAI API Key";
    }

    private File simulateTts(String text, String voice) {
        log.info("使用模拟模式进行语音合成");

        Path outputPath = Paths.get(TEMP_DIR, "tts_" + UUID.randomUUID() + ".wav");

        try {
            StringBuilder header = new StringBuilder();
            header.append("RIFF");
            header.append(longToBytes(36 + text.length() * 2));
            header.append("WAVE");
            header.append("fmt ");
            header.append(longToBytes(16));
            header.append(shortToBytes(1));
            header.append(shortToBytes(1));
            header.append(longToBytes(16000));
            header.append(longToBytes(32000));
            header.append(shortToBytes(2));
            header.append(shortToBytes(16));
            header.append("data");
            header.append(longToBytes(text.length() * 2));

            byte[] audioData = new byte[0];

            Files.write(outputPath, audioData);

            log.info("模拟 TTS 文件已生成: {}", outputPath);
            return outputPath.toFile();

        } catch (IOException e) {
            log.error("生成模拟 TTS 文件失败", e);
            return null;
        }
    }

    private File aliyunTts(String text, String voice) {
        log.info("使用阿里云进行语音合成");
        return simulateTts(text, voice);
    }

    private File openaiTts(String text, String voice) {
        log.info("使用 OpenAI 进行语音合成");
        return simulateTts(text, voice);
    }

    public String saveAudioFile(String base64Data, String format) {
        try {
            byte[] audioBytes = Base64.getDecoder().decode(base64Data);
            String filename = "audio_" + UUID.randomUUID() + "." + format;
            Path filePath = Paths.get(TEMP_DIR, filename);
            Files.write(filePath, audioBytes);
            log.info("音频文件已保存: {}", filePath);
            return filePath.toString();
        } catch (Exception e) {
            log.error("保存音频文件失败", e);
            return null;
        }
    }

    public String audioToBase64(File audioFile) {
        try {
            byte[] fileContent = Files.readAllBytes(audioFile.toPath());
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            log.error("读取音频文件失败", e);
            return null;
        }
    }

    public void cleanupTempFiles() {
        try {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(TEMP_DIR))) {
                for (Path entry : stream) {
                    Files.deleteIfExists(entry);
                }
            }
            log.info("临时音频文件已清理");
        } catch (IOException e) {
            log.error("清理临时文件失败", e);
        }
    }

    public String getCurrentUserId() {
        String userId = SessionContextHolder.getCurrentUserId();
        return userId != null ? userId : "default";
    }

    private byte[] longToBytes(long value) {
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = (byte) (value >> (i * 8));
        }
        return bytes;
    }

    private byte[] shortToBytes(int value) {
        byte[] bytes = new byte[2];
        for (int i = 0; i < 2; i++) {
            bytes[i] = (byte) (value >> (i * 8));
        }
        return bytes;
    }
}