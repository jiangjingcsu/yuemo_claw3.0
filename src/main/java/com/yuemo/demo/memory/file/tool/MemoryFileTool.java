package com.yuemo.demo.memory.file.tool;

import com.yuemo.demo.memory.SessionContextHolder;
import com.yuemo.demo.memory.file.MemoryFileService;
import com.yuemo.demo.tool.AgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MemoryFileTool implements AgentTool {

    private final MemoryFileService memoryFileService;

    public MemoryFileTool(MemoryFileService memoryFileService) {
        this.memoryFileService = memoryFileService;
    }

    @Tool(description = "读取今日日记")
    public String readTodayNote() {
        try {
            String userId = SessionContextHolder.getCurrentUserId();
            memoryFileService.createTodayNoteIfNotExists();
            String content = memoryFileService.readTodayNote();
            return "今日日记内容：\n\n" + content;
        } catch (Exception e) {
            log.error("读取今日日记失败", e);
            return "错误：读取今日日记失败 - " + e.getMessage();
        }
    }

    @Tool(description = "写入今日日记内容")
    public String writeTodayNote(@ToolParam(description = "日记内容") String content) {
        try {
            memoryFileService.createTodayNoteIfNotExists();
            memoryFileService.writeTodayNote(content);
            return "日记已保存";
        } catch (Exception e) {
            log.error("写入今日日记失败", e);
            return "错误：写入日记失败 - " + e.getMessage();
        }
    }

    @Tool(description = "追加内容到今日日记")
    public String appendToTodayNote(@ToolParam(description = "追加的内容") String content) {
        try {
            memoryFileService.createTodayNoteIfNotExists();
            memoryFileService.appendToTodayNote(content);
            return "内容已追加到今日日记";
        } catch (Exception e) {
            log.error("追加日记内容失败", e);
            return "错误：追加日记内容失败 - " + e.getMessage();
        }
    }

    @Tool(description = "搜索笔记中的关键词")
    public String searchNotes(@ToolParam(description = "搜索关键词") String keyword) {
        try {
            String results = memoryFileService.searchInNotes(keyword);
            return "搜索结果：\n\n" + results;
        } catch (Exception e) {
            log.error("搜索笔记失败", e);
            return "错误：搜索笔记失败 - " + e.getMessage();
        }
    }

    @Tool(description = "读取指定路径的笔记文件")
    public String readNote(@ToolParam(description = "笔记文件路径，如 workspace/memory/notes/my_note.md") String filePath) {
        try {
            String content = memoryFileService.readNote(filePath);
            if (content.isEmpty()) {
                return "笔记文件不存在或为空: " + filePath;
            }
            return "笔记内容：\n\n" + content;
        } catch (Exception e) {
            log.error("读取笔记失败: {}", filePath, e);
            return "错误：读取笔记失败 - " + e.getMessage();
        }
    }

    @Tool(description = "写入或创建笔记文件")
    public String writeNote(
            @ToolParam(description = "笔记文件路径") String filePath,
            @ToolParam(description = "笔记内容") String content) {
        try {
            memoryFileService.writeNote(filePath, content);
            return "笔记已保存到: " + filePath;
        } catch (Exception e) {
            log.error("写入笔记失败: {}", filePath, e);
            return "错误：写入笔记失败 - " + e.getMessage();
        }
    }

    @Tool(description = "列出所有日记文件")
    public String listDailyNotes() {
        try {
            var notes = memoryFileService.listDailyNotes();
            if (notes.isEmpty()) {
                return "没有找到日记文件";
            }
            StringBuilder sb = new StringBuilder("日记文件列表：\n\n");
            for (String note : notes) {
                sb.append("- ").append(note).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("列出日记失败", e);
            return "错误：列出日记失败 - " + e.getMessage();
        }
    }

    @Tool(description = "列出所有笔记文件")
    public String listNotes() {
        try {
            var notes = memoryFileService.listNotes();
            if (notes.isEmpty()) {
                return "没有找到笔记文件";
            }
            StringBuilder sb = new StringBuilder("笔记文件列表：\n\n");
            for (String note : notes) {
                sb.append("- ").append(note).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("列出笔记失败", e);
            return "错误：列出笔记失败 - " + e.getMessage();
        }
    }

    @Tool(description = "保存用户偏好设置")
    public String savePreference(
            @ToolParam(description = "偏好名称") String name,
            @ToolParam(description = "偏好值") String value) {
        try {
            memoryFileService.updatePreference(name, value);
            return "偏好已保存: " + name + " = " + value;
        } catch (Exception e) {
            log.error("保存偏好失败", e);
            return "错误：保存偏好失败 - " + e.getMessage();
        }
    }

    @Tool(description = "获取用户偏好设置")
    public String getPreference(@ToolParam(description = "偏好名称") String name) {
        try {
            Object value = memoryFileService.getPreference(name);
            if (value == null) {
                return "未找到偏好: " + name;
            }
            return name + " = " + value;
        } catch (Exception e) {
            log.error("获取偏好失败", e);
            return "错误：获取偏好失败 - " + e.getMessage();
        }
    }
}