package com.yuemo.demo.tool;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;

@Slf4j
@Component
public class BrowserTools implements AgentTool {

    private Playwright playwright;
    private Browser browser;
    private Page page;
    private String currentUrl;
    private static final String SCREENSHOT_DIR = "workspace/screenshots";
    private static final int DEFAULT_TIMEOUT = 30000;

    @PostConstruct
    public void init() {
        try {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(java.util.Arrays.asList("--no-sandbox", "--disable-setuid-sandbox")));
            page = browser.newPage();
            log.info("Playwright 浏览器初始化成功");
        } catch (Exception e) {
            log.error("Playwright 浏览器初始化失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        close();
    }

    public void close() {
        try {
            if (page != null) {
                page.close();
            }
            if (browser != null) {
                browser.close();
            }
            if (playwright != null) {
                playwright.close();
            }
            log.info("Playwright 浏览器已关闭");
        } catch (Exception e) {
            log.error("关闭 Playwright 浏览器时出错", e);
        }
    }

    @Tool(description = "导航到指定URL")
    public String browserNavigate(
            @ToolParam(description = "要导航的URL") String url) {
        if (!ensureBrowserReady()) {
            return "错误：浏览器未初始化，请检查 Playwright 是否正确安装";
        }

        try {
            log.info("导航到: {}", url);
            Response response = page.navigate(url);

            currentUrl = page.url();

            StringBuilder result = new StringBuilder();
            result.append("导航成功！\n");
            result.append("URL: ").append(currentUrl).append("\n");
            result.append("状态码: ").append(response != null ? response.status() : "N/A").append("\n");

            String title = page.title();
            if (title != null && !title.isEmpty()) {
                result.append("页面标题: ").append(title).append("\n");
            }

            return result.toString();
        } catch (Exception e) {
            log.error("导航失败: {}", url, e);
            return "错误：导航失败 - " + e.getMessage();
        }
    }

    @Tool(description = "点击页面上的元素")
    public String browserClick(
            @ToolParam(description = "要点击的元素选择器（CSS选择器或xpath）") String selector) {
        if (!ensureBrowserReady()) {
            return "错误：浏览器未初始化";
        }

        try {
            log.info("点击元素: {}", selector);
            page.click(selector);

            currentUrl = page.url();

            return "点击成功！\n" +
                   "元素: " + selector + "\n" +
                   "当前URL: " + currentUrl + "\n" +
                   "页面标题: " + page.title();
        } catch (Exception e) {
            log.error("点击元素失败: {}", selector, e);
            return "错误：点击元素失败 - " + e.getMessage();
        }
    }

    @Tool(description = "在输入框中输入文本")
    public String browserType(
            @ToolParam(description = "输入框选择器") String selector,
            @ToolParam(description = "要输入的文本") String text) {
        if (!ensureBrowserReady()) {
            return "错误：浏览器未初始化";
        }

        try {
            log.info("输入文本: {} -> {}", selector, text);
            page.fill(selector, text);

            return "输入成功！\n" +
                   "选择器: " + selector + "\n" +
                   "输入内容: " + text;
        } catch (Exception e) {
            log.error("输入文本失败: {} -> {}", selector, text, e);
            return "错误：输入文本失败 - " + e.getMessage();
        }
    }

    @Tool(description = "截取当前页面截图")
    public String browserScreenshot(
            @ToolParam(description = "截图保存路径（可选，默认保存到 workspace/screenshots）") String filePath) {
        if (!ensureBrowserReady()) {
            return "错误：浏览器未初始化";
        }

        try {
            java.nio.file.Files.createDirectories(Paths.get(SCREENSHOT_DIR));
            String screenshotPath = (filePath == null || filePath.trim().isEmpty())
                    ? SCREENSHOT_DIR + "/screenshot_" + System.currentTimeMillis() + ".png"
                    : filePath;

            log.info("截图保存到: {}", screenshotPath);
            byte[] screenshotBytes = page.screenshot();
            java.nio.file.Files.write(Paths.get(screenshotPath), screenshotBytes);

            return "截图成功！\n" +
                   "保存路径: " + screenshotPath + "\n" +
                   "图片大小: " + screenshotBytes.length + " bytes\n" +
                   "页面标题: " + page.title();
        } catch (Exception e) {
            log.error("截图失败", e);
            return "错误：截图失败 - " + e.getMessage();
        }
    }

    @Tool(description = "获取当前页面的HTML内容")
    public String browserGetContent() {
        if (!ensureBrowserReady()) {
            return "错误：浏览器未初始化";
        }

        try {
            String content = page.content();
            currentUrl = page.url();

            StringBuilder result = new StringBuilder();
            result.append("获取页面内容成功！\n");
            result.append("当前URL: ").append(currentUrl).append("\n");
            result.append("页面标题: ").append(page.title()).append("\n");
            result.append("内容长度: ").append(content.length()).append(" 字符\n\n");

            if (content.length() > 2000) {
                result.append("--- HTML 内容 (前2000字符) ---\n");
                result.append(content, 0, 2000);
                result.append("\n... (内容已截断)");
            } else {
                result.append("--- HTML 内容 ---\n");
                result.append(content);
            }

            return result.toString();
        } catch (Exception e) {
            log.error("获取页面内容失败", e);
            return "错误：获取页面内容失败 - " + e.getMessage();
        }
    }

    @Tool(description = "等待指定时间（毫秒）")
    public String browserWait(
            @ToolParam(description = "等待时间（毫秒）") int milliseconds) {
        if (!ensureBrowserReady()) {
            return "错误：浏览器未初始化";
        }

        try {
            log.info("等待 {} 毫秒", milliseconds);
            page.waitForTimeout(milliseconds);
            return "等待完成: " + milliseconds + " 毫秒";
        } catch (Exception e) {
            return "错误：等待失败 - " + e.getMessage();
        }
    }

    @Tool(description = "执行 JavaScript 代码")
    public String browserEvaluate(
            @ToolParam(description = "要执行的 JavaScript 代码") String script) {
        if (!ensureBrowserReady()) {
            return "错误：浏览器未初始化";
        }

        try {
            log.info("执行脚本: {}", script);
            Object result = page.evaluate(script);
            return "脚本执行成功！\n结果: " + (result != null ? result.toString() : "null");
        } catch (Exception e) {
            log.error("执行脚本失败: {}", script, e);
            return "错误：脚本执行失败 - " + e.getMessage();
        }
    }

    @Tool(description = "获取当前浏览器状态")
    public String browserStatus() {
        try {
            StringBuilder status = new StringBuilder();
            status.append("浏览器状态：\n\n");

            if (browser != null && browser.isConnected()) {
                status.append("✅ 浏览器: 已连接\n");
            } else {
                status.append("❌ 浏览器: 未连接\n");
            }

            if (page != null) {
                status.append("✅ 页面: 已创建\n");
                if (currentUrl != null) {
                    status.append("当前URL: ").append(currentUrl).append("\n");
                }
                status.append("页面标题: ").append(page.title()).append("\n");
            } else {
                status.append("❌ 页面: 未创建\n");
            }

            status.append("截图目录: ").append(SCREENSHOT_DIR).append("\n");

            return status.toString();
        } catch (Exception e) {
            return "错误：获取状态失败 - " + e.getMessage();
        }
    }

    private boolean ensureBrowserReady() {
        return browser != null && browser.isConnected() && page != null;
    }
}