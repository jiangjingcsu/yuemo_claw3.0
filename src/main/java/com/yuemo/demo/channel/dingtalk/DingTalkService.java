package com.yuemo.demo.channel.dingtalk;

import com.yuemo.demo.channel.dingtalk.config.DingTalkConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class DingTalkService {

    public boolean sendTextMessage(DingTalkConfig config, String content,
                                   java.util.List<String> atMobiles, boolean isAtAll) {
        String webhook = config.getRobotWebhook();
        if (webhook == null || webhook.isEmpty()) {
            log.error("钉钉机器人 Webhook 未配置");
            return false;
        }

        try {
            String atMobilesStr = atMobiles != null ?
                    String.join("\",\"", atMobiles) : "";

            String jsonBody = String.format(
                    "{\"msgtype\":\"text\",\"text\":{\"content\":\"%s\"},\"at\":{\"atMobiles\":[\"%s\"],\"isAtAll\":%b}}",
                    content.replace("\"", "\\\""),
                    atMobilesStr,
                    isAtAll
            );

            return doPost(webhook, jsonBody);
        } catch (Exception e) {
            log.error("发送钉钉文本消息失败", e);
            return false;
        }
    }

    public boolean sendMarkdownMessage(DingTalkConfig config, String title, String markdown) {
        String webhook = config.getRobotWebhook();
        if (webhook == null || webhook.isEmpty()) {
            log.error("钉钉机器人 Webhook 未配置");
            return false;
        }

        try {
            String jsonBody = String.format(
                    "{\"msgtype\":\"markdown\",\"markdown\":{\"title\":\"%s\",\"text\":\"%s\"}}",
                    title.replace("\"", "\\\""),
                    markdown.replace("\"", "\\\"")
            );

            return doPost(webhook, jsonBody);
        } catch (Exception e) {
            log.error("发送钉钉 Markdown 消息失败", e);
            return false;
        }
    }

    private boolean doPost(String webhookUrl, String jsonBody) {
        HttpURLConnection connection = null;
        try {
            URL url = new URI(webhookUrl).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            String responseBody;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            responseCode >= 200 && responseCode < 300
                                    ? connection.getInputStream()
                                    : connection.getErrorStream(),
                            StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                responseBody = response.toString();
            }

            log.info("钉钉 Webhook 响应 - Code: {}, Body: {}", responseCode, responseBody);
            return responseCode >= 200 && responseCode < 300;
        } catch (Exception e) {
            log.error("钉钉 Webhook 请求失败", e);
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}