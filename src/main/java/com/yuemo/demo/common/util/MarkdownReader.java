package com.yuemo.demo.common.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Markdown 文件读写工具
 *
 * <p>提供 workspace 目录下 Markdown 文件的读取、写入和列表功能。
 * 用于系统提示词管理界面。</p>
 */
@Slf4j
public class MarkdownReader {

    private static final String WORKSPACE_DIR = "workspace";

    /**
     * 获取 workspace 目录下所有 Markdown 文件名
     *
     * @return 文件名列表，目录不存在时返回空列表
     */
    public static List<String> getMarkdownFiles() {
        List<String> files = new ArrayList<>();
        Path workspacePath = Paths.get(WORKSPACE_DIR);

        if (!Files.exists(workspacePath) || !Files.isDirectory(workspacePath)) {
            log.warn("workspace 目录不存在: {}", workspacePath.toAbsolutePath());
            return files;
        }

        try (Stream<Path> paths = Files.list(workspacePath)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".md"))
                 .forEach(p -> files.add(p.getFileName().toString()));
        } catch (IOException e) {
            log.error("列出 Markdown 文件失败", e);
        }

        return files;
    }

    /**
     * 读取 Markdown 文件内容
     *
     * @param fileName 文件名（仅文件名，不含目录路径）
     * @return 文件内容字符串，文件不存在时返回提示信息
     */
    public static String readMarkdownFile(String fileName) {
        Path filePath = Paths.get(WORKSPACE_DIR, fileName);

        if (!Files.exists(filePath)) {
            log.warn("文件不存在: {}", filePath.toAbsolutePath());
            return "文件不存在: " + fileName;
        }

        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            log.error("读取 Markdown 文件失败: {}", fileName, e);
            return "读取文件失败: " + e.getMessage();
        }
    }

    /**
     * 写入 Markdown 文件内容
     *
     * <p>如果文件存在则覆盖，不存在则创建（含父目录）。</p>
     *
     * @param fileName 文件名（仅文件名，不含目录路径）
     * @param content  要写入的内容
     * @return true 表示写入成功
     */
    public static boolean writeMarkdownFile(String fileName, String content) {
        Path filePath = Paths.get(WORKSPACE_DIR, fileName);

        try {
            Path parentDir = filePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            Files.writeString(filePath, content);
            log.info("文件写入成功: {}", filePath.toAbsolutePath());
            return true;
        } catch (IOException e) {
            log.error("写入 Markdown 文件失败: {}", fileName, e);
            return false;
        }
    }
}
