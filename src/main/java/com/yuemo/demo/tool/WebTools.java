package com.yuemo.demo.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

/**
 * 网页工具
 *
 * <p>提供网页内容抓取和网络搜索功能，供 AI 智能体调用。
 * 当前 webSearch 为模拟实现，webFetch 为真实实现。</p>
 */
@Slf4j
@Component
public class WebTools implements AgentTool {

    /**
     * 抓取网页内容
     *
     * <p>获取指定 URL 的 HTML 内容，最多返回前 200 行。</p>
     *
     * @param url 要抓取的网页 URL，必须是合法的 HTTP/HTTPS 地址
     * @return 网页内容字符串，抓取失败时返回错误信息
     */
    @Tool(description = "抓取网页内容，获取指定URL的HTML内容")
    public String webFetch(
            @ToolParam(description = "要抓取的网页URL") String url) {
        log.info("抓取网页: {}", url);

        try {
            URL targetUrl = new URI(url).toURL();
            URLConnection connection = targetUrl.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null && lineCount < 200) {
                    content.append(line).append("\n");
                    lineCount++;
                }
                if (lineCount >= 200) {
                    content.append("\n... [内容已截断，只显示前200行]");
                }
            }

            return "网页内容 (" + url + "):\n" + content.toString();
        } catch (Exception e) {
            log.error("抓取网页失败", e);
            return "错误：抓取网页失败 - " + e.getMessage();
        }
    }

    /**
     * 网络搜索（模拟实现）
     *
     * <p>当前返回模拟搜索结果，需配置真实搜索 API 后才能返回实际结果。</p>
     *
     * @param query 搜索关键词
     * @param count 返回结果数量，默认10
     * @return 模拟搜索结果
     */
    @Tool(description = "模拟网络搜索，返回搜索结果摘要（需要真实搜索API）")
    public String webSearch(
            @ToolParam(description = "搜索查询关键词") String query,
            @ToolParam(description = "返回结果数量，默认10") Integer count) {
        log.info("网络搜索: {}, 结果数量: {}", query, count);

        int resultCount = (count != null && count > 0) ? count : 10;

        StringBuilder result = new StringBuilder();
        result.append("搜索结果 (查询: ").append(query).append("):\n\n");
        result.append("注意：这是模拟搜索结果。如需真实搜索功能，请配置搜索API。\n\n");

        for (int i = 1; i <= Math.min(resultCount, 5); i++) {
            result.append(i).append(". [模拟结果] 关于 \"").append(query).append("\" 的相关信息\n");
            result.append("   摘要: 这是一个模拟的搜索结果摘要...\n");
            result.append("   URL: https://example.com/result").append(i).append("\n\n");
        }

        return result.toString();
    }
}
