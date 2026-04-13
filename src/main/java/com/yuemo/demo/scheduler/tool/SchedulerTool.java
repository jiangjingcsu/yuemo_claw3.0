package com.yuemo.demo.scheduler.tool;

import com.yuemo.demo.memory.SessionContextHolder;
import com.yuemo.demo.scheduler.SchedulerService;
import com.yuemo.demo.scheduler.entity.ScheduledTask;
import com.yuemo.demo.scheduler.entity.TaskExecutionHistory;
import com.yuemo.demo.tool.AgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SchedulerTool implements AgentTool {

    private final SchedulerService schedulerService;

    public SchedulerTool(@Lazy SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    private String getCurrentUserId() {
        String userId = SessionContextHolder.getCurrentUserId();
        return userId != null ? userId : "default";
    }

    @Tool(description = "创建定时任务（基于 Cron 表达式）")
    public String createScheduledTask(
            @ToolParam(description = "任务名称") String name,
            @ToolParam(description = "Cron 表达式，如 \"0 30 8 * * *\" 表示每天8:30执行") String cronExpression,
            @ToolParam(description = "触发时发送的消息内容") String triggerMessage,
            @ToolParam(description = "渠道类型：FEISHU, DINGTALK, SWING") String channelType) {
        try {
            ScheduledTask task = schedulerService.createCronTask(
                    name, cronExpression, getCurrentUserId(), channelType, triggerMessage);
            return "定时任务创建成功！\n\n" +
                   "任务ID: " + task.getId() + "\n" +
                   "任务名称: " + task.getName() + "\n" +
                   "执行时间: " + cronExpression + "\n" +
                   "触发消息: " + triggerMessage;
        } catch (Exception e) {
            log.error("创建定时任务失败", e);
            return "错误：创建定时任务失败 - " + e.getMessage();
        }
    }

    @Tool(description = "创建延迟执行任务（一次性）")
    public String createDelayedTask(
            @ToolParam(description = "任务名称") String name,
            @ToolParam(description = "延迟时间（秒）") int delaySeconds,
            @ToolParam(description = "触发时发送的消息内容") String triggerMessage,
            @ToolParam(description = "渠道类型：FEISHU, DINGTALK, SWING") String channelType) {
        try {
            ScheduledTask task = schedulerService.createOneTimeTask(
                    name, delaySeconds * 1000L, getCurrentUserId(), channelType, triggerMessage);
            return "延迟任务创建成功！\n\n" +
                   "任务ID: " + task.getId() + "\n" +
                   "任务名称: " + task.getName() + "\n" +
                   "延迟时间: " + delaySeconds + "秒\n" +
                   "触发消息: " + triggerMessage;
        } catch (Exception e) {
            log.error("创建延迟任务失败", e);
            return "错误：创建延迟任务失败 - " + e.getMessage();
        }
    }

    @Tool(description = "列出当前用户的所有定时任务")
    public String listScheduledTasks() {
        try {
            List<ScheduledTask> tasks = schedulerService.getUserTasks(getCurrentUserId());
            if (tasks.isEmpty()) {
                return "没有找到定时任务";
            }

            StringBuilder sb = new StringBuilder("定时任务列表：\n\n");
            for (ScheduledTask task : tasks) {
                sb.append("━━━━━━━━━━━━━━━━━━━━\n");
                sb.append("ID: ").append(task.getId()).append("\n");
                sb.append("名称: ").append(task.getName()).append("\n");
                sb.append("类型: ").append(task.getTaskType()).append("\n");
                sb.append("状态: ").append(task.getStatus()).append("\n");
                sb.append("Cron: ").append(task.getCronExpression() != null ? task.getCronExpression() : "N/A").append("\n");
                sb.append("执行次数: ").append(task.getExecutionCount()).append("\n");
                sb.append("最后执行: ").append(task.getLastRunAt() != null ? task.getLastRunAt().toString() : "从未").append("\n");
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("列出定时任务失败", e);
            return "错误：列出定时任务失败 - " + e.getMessage();
        }
    }

    @Tool(description = "获取任务的执行历史")
    public String getTaskHistory(
            @ToolParam(description = "任务ID") String taskId) {
        try {
            List<TaskExecutionHistory> history = schedulerService.getTaskHistory(taskId);
            if (history.isEmpty()) {
                return "没有找到执行历史";
            }

            StringBuilder sb = new StringBuilder("任务执行历史：\n\n");
            for (TaskExecutionHistory exec : history) {
                sb.append("━━━━━━━━━━━━━━━━━━━━\n");
                sb.append("状态: ").append(exec.getStatus()).append("\n");
                sb.append("开始时间: ").append(exec.getStartTime()).append("\n");
                sb.append("耗时: ").append(exec.getDurationMs()).append("ms\n");
                if (exec.getStatus() == TaskExecutionHistory.ExecutionStatus.SUCCESS) {
                    String result = exec.getResult();
                    if (result != null && result.length() > 100) {
                        result = result.substring(0, 100) + "...";
                    }
                    sb.append("结果: ").append(result).append("\n");
                } else if (exec.getStatus() == TaskExecutionHistory.ExecutionStatus.FAILED) {
                    sb.append("错误: ").append(exec.getErrorMessage()).append("\n");
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("获取任务历史失败", e);
            return "错误：获取任务历史失败 - " + e.getMessage();
        }
    }

    @Tool(description = "暂停定时任务")
    public String pauseScheduledTask(
            @ToolParam(description = "任务ID") String taskId) {
        try {
            schedulerService.pauseTask(taskId);
            return "任务已暂停";
        } catch (Exception e) {
            log.error("暂停任务失败", e);
            return "错误：暂停任务失败 - " + e.getMessage();
        }
    }

    @Tool(description = "恢复已暂停的定时任务")
    public String resumeScheduledTask(
            @ToolParam(description = "任务ID") String taskId) {
        try {
            schedulerService.resumeTask(taskId);
            return "任务已恢复";
        } catch (Exception e) {
            log.error("恢复任务失败", e);
            return "错误：恢复任务失败 - " + e.getMessage();
        }
    }

    @Tool(description = "取消并删除定时任务")
    public String cancelScheduledTask(
            @ToolParam(description = "任务ID") String taskId) {
        try {
            schedulerService.deleteTask(taskId);
            return "任务已删除";
        } catch (Exception e) {
            log.error("删除任务失败", e);
            return "错误：删除任务失败 - " + e.getMessage();
        }
    }

    @Tool(description = "手动立即执行定时任务")
    public String runTaskNow(
            @ToolParam(description = "任务ID") String taskId) {
        try {
            ScheduledTask task = schedulerService.getTask(taskId);
            if (task == null) {
                return "错误：任务不存在";
            }
            schedulerService.executeTask(task);
            return "任务已开始执行，请等待结果...";
        } catch (Exception e) {
            log.error("执行任务失败", e);
            return "错误：执行任务失败 - " + e.getMessage();
        }
    }
}