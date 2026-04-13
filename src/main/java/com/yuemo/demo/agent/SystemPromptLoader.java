package com.yuemo.demo.agent;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SystemPromptLoader {

    private static final String WORKSPACE_DIR = "workspace";
    private static final String DEFAULT_EXCLUDE_DIRS = "skills,.git";
    private static final String DEFAULT_EXCLUDE_FILES = ".gitignore,README.md";
    private static final long CACHE_TTL_SECONDS = 60;

    private final Map<String, CachedPrompt> promptCache = new ConcurrentHashMap<>();
    private Instant lastScanTime = Instant.MIN;
    private List<Path> cachedFilePaths = Collections.emptyList();

    private Set<String> excludeDirs;
    private Set<String> excludeFiles;
    private boolean enabled = true;
    private String workspaceDir = WORKSPACE_DIR;

    @PostConstruct
    public void init() {
        loadConfiguration();
        log.info("SystemPromptLoader 初始化完成，排除目录: {}, 排除文件: {}", excludeDirs, excludeFiles);
    }

    public void setWorkspaceDir(String workspaceDir) {
        this.workspaceDir = workspaceDir;
        clearCache();
    }

    private void loadConfiguration() {
        excludeDirs = new HashSet<>(Arrays.asList(DEFAULT_EXCLUDE_DIRS.split(",")));
        excludeFiles = new HashSet<>(Arrays.asList(DEFAULT_EXCLUDE_FILES.split(",")));

        String customExcludeDirs = System.getProperty("system-prompt.exclude-dirs");
        if (customExcludeDirs != null && !customExcludeDirs.isEmpty()) {
            excludeDirs.addAll(Arrays.asList(customExcludeDirs.split(",")));
        }

        String customExcludeFiles = System.getProperty("system-prompt.exclude-files");
        if (customExcludeFiles != null && !customExcludeFiles.isEmpty()) {
            excludeFiles.addAll(Arrays.asList(customExcludeFiles.split(",")));
        }

        String enabledProp = System.getProperty("system-prompt.enabled");
        if (enabledProp != null) {
            enabled = Boolean.parseBoolean(enabledProp);
        }

        log.info("动态提示词加载配置: enabled={}, excludeDirs={}, excludeFiles={}",
                enabled, excludeDirs, excludeFiles);
    }

    public String getSystemPrompt() {
        if (!enabled) {
            log.info("动态提示词加载已禁用，使用默认提示词");
            return getDefaultSystemPrompt();
        }

        try {
            List<Path> markdownFiles = getMarkdownFiles();
            if (markdownFiles.isEmpty()) {
                log.warn("未找到任何 Markdown 文件，使用默认提示词");
                return getDefaultSystemPrompt();
            }

            StringBuilder fullPrompt = new StringBuilder();
            for (Path filePath : markdownFiles) {
                String content = readFileContent(filePath);
                if (content != null && !content.isEmpty()) {
                    fullPrompt.append(content).append("\n\n");
                    log.debug("加载提示词文件: {}", filePath.getFileName());
                }
            }

            String result = fullPrompt.toString().trim();
            if (result.isEmpty()) {
                log.warn("所有提示词文件内容为空，使用默认提示词");
                return getDefaultSystemPrompt();
            }

            log.info("系统提示词加载成功，共加载 {} 个文件", markdownFiles.size());
            return result;

        } catch (Exception e) {
            log.error("加载系统提示词失败，使用默认提示词", e);
            return getDefaultSystemPrompt();
        }
    }

    public List<Path> getMarkdownFiles() {
        if (!enabled) {
            return Collections.emptyList();
        }

        if (isCacheValid()) {
            log.debug("使用缓存的 Markdown 文件列表，共 {} 个文件", cachedFilePaths.size());
            return cachedFilePaths;
        }

        Path workspacePath = Paths.get(workspaceDir);
        if (!Files.exists(workspacePath) || !Files.isDirectory(workspacePath)) {
            log.warn("workspace 目录不存在或不是目录: {}", workspacePath.toAbsolutePath());
            return Collections.emptyList();
        }

        List<Path> foundFiles = new ArrayList<>();

        try {
            Files.walkFileTree(workspacePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String dirName = dir.getFileName().toString();
                    if (excludeDirs.contains(dirName)) {
                        log.debug("跳过排除的目录: {}", dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String fileName = file.getFileName().toString();

                    if (!fileName.endsWith(".md")) {
                        return FileVisitResult.CONTINUE;
                    }

                    if (excludeFiles.contains(fileName)) {
                        log.debug("跳过排除的文件: {}", file);
                        return FileVisitResult.CONTINUE;
                    }

                    foundFiles.add(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    log.warn("访问文件失败: {}", file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("扫描 workspace 目录失败", e);
            return Collections.emptyList();
        }

        foundFiles.sort(Comparator.comparing(Path::getFileName));
        cachedFilePaths = Collections.unmodifiableList(foundFiles);
        lastScanTime = Instant.now();

        log.info("扫描完成，找到 {} 个 Markdown 文件", foundFiles.size());
        return cachedFilePaths;
    }

    public List<String> getLoadedFileNames() {
        return getMarkdownFiles().stream()
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.toList());
    }

    private boolean isCacheValid() {
        return !cachedFilePaths.isEmpty()
                && lastScanTime.isAfter(Instant.now().minusSeconds(CACHE_TTL_SECONDS));
    }

    private String readFileContent(Path filePath) {
        String key = filePath.toAbsolutePath().toString();

        CachedPrompt cached = promptCache.get(key);
        if (cached != null && cached.isValid()) {
            return cached.content;
        }

        try {
            String content = Files.readString(filePath);
            promptCache.put(key, new CachedPrompt(content, Instant.now()));
            return content;
        } catch (IOException e) {
            log.error("读取文件失败: {}", filePath, e);
            return null;
        }
    }

    public void clearCache() {
        promptCache.clear();
        cachedFilePaths = Collections.emptyList();
        lastScanTime = Instant.MIN;
        log.info("提示词缓存已清除");
    }

    public void refresh() {
        clearCache();
        getMarkdownFiles();
        log.info("提示词缓存已刷新");
    }

    public String getDefaultSystemPrompt() {
        return """
                你是一个智能助手，基于 Spring AI Alibaba 构建。

                当用户提出问题时，你应该主动思考是否需要调用工具来更准确地回答。

                你的能力通过技能系统（Skills）扩展：
                - 技能定义位于 workspace/skills/ 目录
                - AI 可以使用 read_skill 工具读取技能详情
                - 使用 search_skills 工具搜索可用技能
                - 具体任务通过 ShellTool 执行 Python 脚本

                对于简单的事实性问题（如常识、通用知识），可以直接回答。
                对于需要执行操作、实时数据或多步骤协作的请求，请调用技能。

                注意：如果 workspace/skills/ 目录下的技能不可用，请提醒用户配置必要的环境。
                """;
    }

    public String getFeishuOpenId() {
        String openId = System.getProperty("feishu.user.openid");
        if (openId == null || openId.isEmpty()) {
            openId = System.getenv("FEISHU_USER_OPENID");
        }

        if (openId != null && !openId.isEmpty()) {
            log.debug("获取到飞书用户 openId: {}", openId);
        } else {
            log.debug("未配置飞书用户 openId");
        }

        return openId;
    }

    public void setExcludeDirs(Set<String> excludeDirs) {
        this.excludeDirs = excludeDirs;
        clearCache();
    }

    public void setExcludeFiles(Set<String> excludeFiles) {
        this.excludeFiles = excludeFiles;
        clearCache();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            clearCache();
        }
    }

    private static class CachedPrompt {
        final String content;
        final Instant timestamp;

        CachedPrompt(String content, Instant timestamp) {
            this.content = content;
            this.timestamp = timestamp;
        }

        boolean isValid() {
            return timestamp.isAfter(Instant.now().minusSeconds(CACHE_TTL_SECONDS));
        }
    }
}