package com.yuemo.demo.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * 系统工具
 *
 * <p>提供系统级操作功能，包括命令执行、休眠等，供 AI 智能体调用。</p>
 */
@Slf4j
@Component
public class SystemTools implements AgentTool {

    private final ThreadPoolTaskScheduler taskScheduler;

    public SystemTools(ThreadPoolTaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    /**
     * 执行系统命令
     *
     * <p>在独立进程中执行指定的系统命令，最多等待 30 秒获取结果。</p>
     *
     * @param command 要执行的系统命令，不可为null或空
     * @return 命令执行结果（标准输出 + 标准错误），超时或异常时返回错误信息
     */
    @Tool(description = "执行系统命令并返回输出结果")
    public String executeCommand(
            @ToolParam(description = "要执行的系统命令") String command) {
        log.info("执行系统命令: {}", command);

        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(new java.io.File(System.getProperty("user.dir")));

            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("win")) {
                pb.command("cmd", "/c", command);
            } else {
                pb.command("sh", "-c", command);
            }

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    output.append("[ERROR] ").append(line).append("\n");
                }
            }

            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return "错误：命令执行超时（30秒）\n已获取的输出:\n" + output;
            }

            int exitCode = process.exitValue();
            output.append("退出码: ").append(exitCode);

            return output.toString();
        } catch (IOException | InterruptedException e) {
            log.error("执行系统命令失败", e);
            return "错误：执行系统命令失败 - " + e.getMessage();
        }
    }

    /**
     * 休眠指定时间
     *
     * @param seconds 休眠秒数，必须大于0
     * @return 休眠结果
     */
    @Tool(description = "休眠指定秒数")
    public String sleep(
            @ToolParam(description = "休眠的秒数") int seconds) {
        log.info("休眠: {} 秒", seconds);

        if (seconds <= 0) {
            return "错误：休眠时间必须大于0";
        }

        try {
            TimeUnit.SECONDS.sleep(seconds);
            return "成功：已休眠 " + seconds + " 秒";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "休眠被中断";
        }
    }
}
