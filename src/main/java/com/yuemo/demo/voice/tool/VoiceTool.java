package com.yuemo.demo.voice.tool;

import com.yuemo.demo.memory.SessionContextHolder;
import com.yuemo.demo.tool.AgentTool;
import com.yuemo.demo.voice.VoiceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.File;

@Slf4j
@Component
public class VoiceTool implements AgentTool {

    private final VoiceService voiceService;

    public VoiceTool(VoiceService voiceService) {
        this.voiceService = voiceService;
    }

    @Tool(description = "将语音转换为文字")
    public String speechToText(
            @ToolParam(description = "音频文件路径") String audioFilePath,
            @ToolParam(description = "语言代码，如 zh-CN, en-US", required = false) String language) {
        try {
            File audioFile = new File(audioFilePath);
            if (!audioFile.exists()) {
                return "错误：音频文件不存在 - " + audioFilePath;
            }

            String result = voiceService.speechToText(audioFile, language);
            log.info("语音转文字完成: {}", result);
            return "语音识别结果：\n\n" + result;
        } catch (Exception e) {
            log.error("语音转文字失败", e);
            return "错误：语音转文字失败 - " + e.getMessage();
        }
    }

    @Tool(description = "将文字转换为语音文件")
    public String textToSpeech(
            @ToolParam(description = "要转换的文字内容") String text,
            @ToolParam(description = "语音名称，如 zh-CN-female, en-US-male", required = false) String voice) {
        try {
            if (text == null || text.trim().isEmpty()) {
                return "错误：文本内容不能为空";
            }

            File audioFile = voiceService.textToSpeech(text, voice);
            if (audioFile != null && audioFile.exists()) {
                log.info("文字转语音完成: {}", audioFile.getAbsolutePath());
                return "语音文件已生成！\n\n" +
                       "文件路径: " + audioFile.getAbsolutePath() + "\n" +
                       "文本长度: " + text.length() + " 字符";
            } else {
                return "错误：语音生成失败";
            }
        } catch (Exception e) {
            log.error("文字转语音失败", e);
            return "错误：文字转语音失败 - " + e.getMessage();
        }
    }

    @Tool(description = "将 Base64 编码的音频数据保存为文件")
    public String saveAudioFromBase64(
            @ToolParam(description = "Base64 编码的音频数据") String base64Data,
            @ToolParam(description = "音频格式，如 wav, mp3, m4a") String format) {
        try {
            if (base64Data == null || base64Data.trim().isEmpty()) {
                return "错误：Base64 数据不能为空";
            }

            String filePath = voiceService.saveAudioFile(base64Data, format);
            if (filePath != null) {
                log.info("音频文件已保存: {}", filePath);
                return "音频文件已保存！\n\n文件路径: " + filePath;
            } else {
                return "错误：保存音频文件失败";
            }
        } catch (Exception e) {
            log.error("保存音频文件失败", e);
            return "错误：保存音频文件失败 - " + e.getMessage();
        }
    }

    @Tool(description = "将音频文件转换为 Base64 编码")
    public String audioToBase64(
            @ToolParam(description = "音频文件路径") String audioFilePath) {
        try {
            File audioFile = new File(audioFilePath);
            if (!audioFile.exists()) {
                return "错误：音频文件不存在 - " + audioFilePath;
            }

            String base64 = voiceService.audioToBase64(audioFile);
            if (base64 != null) {
                log.info("音频文件已转换为 Base64: {}", audioFilePath);
                return "Base64 编码已完成！\n\n" +
                       "文件: " + audioFilePath + "\n" +
                       "Base64 长度: " + base64.length() + " 字符\n\n" +
                       "Base64 数据（前100字符）:\n" +
                       base64.substring(0, Math.min(100, base64.length())) + "...";
            } else {
                return "错误：音频文件转换失败";
            }
        } catch (Exception e) {
            log.error("音频转 Base64 失败", e);
            return "错误：音频转 Base64 失败 - " + e.getMessage();
        }
    }

    @Tool(description = "获取语音服务状态")
    public String getVoiceServiceStatus() {
        try {
            return "语音服务状态：\n\n" +
                   "✅ 服务正常\n" +
                   "STT 提供商: simulated\n" +
                   "TTS 提供商: simulated\n" +
                   "默认语言: zh-CN\n" +
                   "采样率: 16000 Hz";
        } catch (Exception e) {
            log.error("获取语音服务状态失败", e);
            return "错误：获取语音服务状态失败 - " + e.getMessage();
        }
    }

    @Tool(description = "处理语音消息（接收用户语音后自动调用）")
    public String processVoiceMessage(
            @ToolParam(description = "语音文件路径或 Base64 数据") String voiceInput,
            @ToolParam(description = "是否为 Base64 编码") boolean isBase64,
            @ToolParam(description = "语言代码", required = false) String language) {
        try {
            String audioPath;

            if (isBase64) {
                audioPath = voiceService.saveAudioFile(voiceInput, "wav");
                if (audioPath == null) {
                    return "错误：保存语音文件失败";
                }
            } else {
                audioPath = voiceInput;
            }

            String text = voiceService.speechToText(new File(audioPath), language);
            return "语音已识别：\n\n" + text;
        } catch (Exception e) {
            log.error("处理语音消息失败", e);
            return "错误：处理语音消息失败 - " + e.getMessage();
        }
    }
}