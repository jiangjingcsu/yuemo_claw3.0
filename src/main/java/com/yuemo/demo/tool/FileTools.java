package com.yuemo.demo.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 文件操作工具
 *
 * <p>提供文件读写、编辑和目录列表功能，供 AI 智能体调用。</p>
 */
@Slf4j
@Component
public class FileTools implements AgentTool {

    /**
     * 读取文件内容
     *
     * @param filePath 要读取的文件路径
     * @return 文件内容，读取失败时返回错误信息
     */
    @Tool(description = "读取文件内容，支持文本文件")
    public String readFile(
            @ToolParam(description = "要读取的文件路径") String filePath) {
        log.info("读取文件: {}", filePath);

        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return "错误：文件不存在 - " + filePath;
            }

            String content = Files.readString(path);
            return "文件内容:\n" + content;
        } catch (IOException e) {
            log.error("读取文件失败", e);
            return "错误：读取文件失败 - " + e.getMessage();
        }
    }

    /**
     * 写入文件内容
     *
     * <p>如果文件存在则覆盖，不存在则创建（含父目录）。</p>
     *
     * @param filePath 要写入的文件路径
     * @param content  要写入的文件内容
     * @return 操作结果
     */
    @Tool(description = "写入文件内容，如果文件存在则覆盖")
    public String writeFile(
            @ToolParam(description = "要写入的文件路径") String filePath,
            @ToolParam(description = "要写入的文件内容") String content) {
        log.info("写入文件: {}", filePath);

        try {
            Path path = Paths.get(filePath);

            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return "成功：文件已写入 - " + filePath;
        } catch (IOException e) {
            log.error("写入文件失败", e);
            return "错误：写入文件失败 - " + e.getMessage();
        }
    }

    /**
     * 编辑文件（查找替换）
     *
     * @param filePath    要编辑的文件路径
     * @param searchText  要查找的文本
     * @param replaceText 要替换的文本
     * @return 操作结果
     */
    @Tool(description = "编辑文件，在指定位置插入或替换内容")
    public String editFile(
            @ToolParam(description = "要编辑的文件路径") String filePath,
            @ToolParam(description = "要查找的文本") String searchText,
            @ToolParam(description = "要替换的文本") String replaceText) {
        log.info("编辑文件: {}", filePath);

        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return "错误：文件不存在 - " + filePath;
            }

            String content = Files.readString(path);
            if (!content.contains(searchText)) {
                return "错误：文件中未找到指定文本 - " + searchText;
            }

            String newContent = content.replace(searchText, replaceText);
            Files.writeString(path, newContent, StandardOpenOption.TRUNCATE_EXISTING);

            return "成功：文件已编辑 - " + filePath;
        } catch (IOException e) {
            log.error("编辑文件失败", e);
            return "错误：编辑文件失败 - " + e.getMessage();
        }
    }

    /**
     * 列出目录内容
     *
     * @param dirPath 要列出的目录路径
     * @return 目录内容列表
     */
    @Tool(description = "列出指定目录的文件和文件夹")
    public String listDirectory(
            @ToolParam(description = "要列出的目录路径") String dirPath) {
        log.info("列出目录: {}", dirPath);

        try {
            File dir = new File(dirPath);
            if (!dir.exists() || !dir.isDirectory()) {
                return "错误：目录不存在 - " + dirPath;
            }

            File[] files = dir.listFiles();
            if (files == null || files.length == 0) {
                return "目录为空: " + dirPath;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("目录内容 (").append(dirPath).append("):\n");

            for (File file : files) {
                String type = file.isDirectory() ? "[DIR] " : "[FILE]";
                sb.append(String.format("%s %s\n", type, file.getName()));
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("列出目录失败", e);
            return "错误：列出目录失败 - " + e.getMessage();
        }
    }
}
