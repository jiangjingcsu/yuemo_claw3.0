package com.yuemo.demo.memory.file;

import com.yuemo.demo.memory.SessionContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Slf4j
@Service
public class MemoryFileService {

    private static final String MEMORY_DIR = "workspace/memory";
    private static final String DAILY_DIR = MEMORY_DIR + "/daily";
    private static final String NOTES_DIR = MEMORY_DIR + "/notes";
    private static final String PREFERENCES_DIR = MEMORY_DIR + "/preferences";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Map<String, Object> memoryCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            createDirectories();
            log.info("记忆文件系统初始化完成");
            log.info("  - 日记目录: {}", DAILY_DIR);
            log.info("  - 笔记目录: {}", NOTES_DIR);
            log.info("  - 偏好目录: {}", PREFERENCES_DIR);
        } catch (IOException e) {
            log.error("记忆文件系统初始化失败", e);
        }
    }

    private void createDirectories() throws IOException {
        Files.createDirectories(Paths.get(DAILY_DIR));
        Files.createDirectories(Paths.get(NOTES_DIR));
        Files.createDirectories(Paths.get(PREFERENCES_DIR));
    }

    public String getCurrentUserId() {
        String userId = SessionContextHolder.getCurrentUserId();
        return userId != null ? userId : "default";
    }

    public String getTodayNotePath() {
        String dateStr = LocalDate.now().format(FILE_DATE_FORMATTER);
        return DAILY_DIR + "/" + dateStr + ".md";
    }

    public String readTodayNote() {
        return readNote(getTodayNotePath());
    }

    public String readNote(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                return Files.readString(path);
            }
            return "";
        } catch (IOException e) {
            log.error("读取笔记失败: {}", filePath, e);
            return "";
        }
    }

    public void writeTodayNote(String content) {
        writeNote(getTodayNotePath(), content);
    }

    public void writeNote(String filePath, String content) {
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
            log.info("写入笔记成功: {}", filePath);
        } catch (IOException e) {
            log.error("写入笔记失败: {}", filePath, e);
        }
    }

    public void appendToTodayNote(String content) {
        String existing = readTodayNote();
        String separator = existing.isEmpty() ? "" : "\n\n";
        writeTodayNote(existing + separator + content);
    }

    public List<String> listDailyNotes() {
        return listFiles(DAILY_DIR, ".md");
    }

    public List<String> listNotes() {
        return listFiles(NOTES_DIR, ".md");
    }

    private List<String> listFiles(String directory, String extension) {
        List<String> files = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(directory))) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(extension))
                    .map(p -> p.toString().replace("\\", "/"))
                    .forEach(files::add);
        } catch (IOException e) {
            log.error("列出文件失败: {}", directory, e);
        }
        Collections.sort(files, Collections.reverseOrder());
        return files;
    }

    public String searchInNotes(String keyword) {
        StringBuilder result = new StringBuilder();
        List<String> allNotes = listNotes();
        allNotes.addAll(listDailyNotes());

        for (String notePath : allNotes) {
            String content = readNote(notePath);
            if (content.toLowerCase().contains(keyword.toLowerCase())) {
                result.append("### ").append(notePath).append("\n\n");
                result.append(content).append("\n\n---\n\n");
            }
        }

        return result.length() > 0 ? result.toString() : "未找到包含 '" + keyword + "' 的内容";
    }

    public Map<String, Object> getUserPreferences() {
        String userId = getCurrentUserId();
        String prefFile = PREFERENCES_DIR + "/" + userId + "_preferences.json";

        try {
            Path path = Paths.get(prefFile);
            if (Files.exists(path)) {
                String json = Files.readString(path);
                return parseJsonToMap(json);
            }
        } catch (IOException e) {
            log.error("读取用户偏好失败: {}", prefFile, e);
        }

        return new HashMap<>();
    }

    public void saveUserPreferences(Map<String, Object> preferences) {
        String userId = getCurrentUserId();
        String prefFile = PREFERENCES_DIR + "/" + userId + "_preferences.json";

        try {
            String json = mapToJson(preferences);
            Files.writeString(Paths.get(prefFile), json);
            log.info("保存用户偏好成功: {}", prefFile);
        } catch (IOException e) {
            log.error("保存用户偏好失败: {}", prefFile, e);
        }
    }

    public void updatePreference(String key, Object value) {
        Map<String, Object> prefs = getUserPreferences();
        prefs.put(key, value);
        saveUserPreferences(prefs);
    }

    public Object getPreference(String key) {
        Map<String, Object> prefs = getUserPreferences();
        return prefs.get(key);
    }

    public String generateDailyNoteTemplate() {
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DATE_FORMATTER);
        String dayOfWeek = today.getDayOfWeek().toString();

        return String.format("""
# %s (%s)

## 天气

## 今日要点

## AI 对话摘要



## 明日计划

## 标签
#日记
""", dateStr, dayOfWeek);
    }

    public void createTodayNoteIfNotExists() {
        String todayPath = getTodayNotePath();
        if (!Files.exists(Paths.get(todayPath))) {
            writeTodayNote(generateDailyNoteTemplate());
            log.info("创建今日日记: {}", todayPath);
        }
    }

    private Map<String, Object> parseJsonToMap(String json) {
        Map<String, Object> map = new HashMap<>();
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
            if (!json.isEmpty()) {
                String[] pairs = json.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split(":");
                    if (kv.length == 2) {
                        String key = kv[0].trim().replace("\"", "");
                        String value = kv[1].trim().replace("\"", "");
                        map.put(key, value);
                    }
                }
            }
        }
        return map;
    }

    private String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value);
            }
            i++;
        }
        sb.append("}");
        return sb.toString();
    }
}