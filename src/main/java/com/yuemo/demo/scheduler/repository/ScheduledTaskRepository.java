package com.yuemo.demo.scheduler.repository;

import com.yuemo.demo.scheduler.entity.ScheduledTask;
import com.yuemo.demo.scheduler.entity.TaskExecutionHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class ScheduledTaskRepository {

    private static final String TASK_DB_FILE = "workspace/data/tasks.db";
    private static final String EXECUTION_DB_FILE = "workspace/data/task_executions.db";

    private final Map<String, ScheduledTask> taskStore = new ConcurrentHashMap<>();
    private final Map<String, List<TaskExecutionHistory>> executionStore = new ConcurrentHashMap<>();
    private final Map<String, Integer> taskIdCounter = new ConcurrentHashMap<>();

    public ScheduledTaskRepository() {
        loadTasks();
    }

    private void loadTasks() {
        try {
            Files.createDirectories(Paths.get("workspace/data"));
            if (Files.exists(Paths.get(TASK_DB_FILE))) {
                List<String> lines = Files.readAllLines(Paths.get(TASK_DB_FILE));
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    ScheduledTask task = parseTask(line);
                    if (task != null) {
                        taskStore.put(task.getId(), task);
                    }
                }
            }
            log.info("加载了 {} 个定时任务", taskStore.size());
        } catch (IOException e) {
            log.error("加载任务失败", e);
        }
    }

    private void saveTasks() {
        try {
            StringBuilder sb = new StringBuilder();
            for (ScheduledTask task : taskStore.values()) {
                sb.append(serializeTask(task)).append("\n");
            }
            Files.writeString(Paths.get(TASK_DB_FILE), sb.toString());
        } catch (IOException e) {
            log.error("保存任务失败", e);
        }
    }

    public ScheduledTask save(ScheduledTask task) {
        if (task.getId() == null) {
            task.setId(generateTaskId());
            task.setCreatedAt(LocalDateTime.now());
        }
        taskStore.put(task.getId(), task);
        saveTasks();
        return task;
    }

    public Optional<ScheduledTask> findById(String id) {
        return Optional.ofNullable(taskStore.get(id));
    }

    public List<ScheduledTask> findByUserId(String userId) {
        return taskStore.values().stream()
                .filter(t -> userId.equals(t.getUserId()))
                .collect(Collectors.toList());
    }

    public List<ScheduledTask> findActiveTasks() {
        return taskStore.values().stream()
                .filter(t -> t.getStatus() == ScheduledTask.TaskStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    public List<ScheduledTask> findAll() {
        return new ArrayList<>(taskStore.values());
    }

    public void delete(String id) {
        taskStore.remove(id);
        executionStore.remove(id);
        saveTasks();
    }

    public void update(ScheduledTask task) {
        taskStore.put(task.getId(), task);
        saveTasks();
    }

    public void saveExecution(TaskExecutionHistory execution) {
        executionStore.computeIfAbsent(execution.getTaskId(), k -> new ArrayList<>()).add(execution);
        saveExecutions();
    }

    public List<TaskExecutionHistory> findExecutionsByTaskId(String taskId) {
        return executionStore.getOrDefault(taskId, new ArrayList<>());
    }

    public List<TaskExecutionHistory> findRecentExecutions(String taskId, int limit) {
        List<TaskExecutionHistory> executions = executionStore.getOrDefault(taskId, new ArrayList<>());
        int size = executions.size();
        if (size <= limit) {
            return executions;
        }
        return executions.subList(size - limit, size);
    }

    private String generateTaskId() {
        String prefix = "task_";
        int count = taskIdCounter.merge("counter", 1, Integer::sum);
        return prefix + System.currentTimeMillis() + "_" + count;
    }

    private String serializeTask(ScheduledTask task) {
        return String.join("|",
                safe(task.getId()),
                safe(task.getName()),
                safe(task.getDescription()),
                safe(task.getCronExpression()),
                safe(task.getTaskType().name()),
                safe(task.getUserId()),
                safe(task.getChannelType()),
                safe(task.getTriggerMessage()),
                safe(task.getStatus().name()),
                safe(task.getCreatedAt()),
                safe(task.getLastRunAt()),
                safe(task.getNextRunAt()),
                String.valueOf(task.getExecutionCount())
        );
    }

    private ScheduledTask parseTask(String line) {
        try {
            String[] parts = line.split("\\|");
            if (parts.length < 12) return null;

            return ScheduledTask.builder()
                    .id(parts[0])
                    .name(parts[1])
                    .description(parts[2])
                    .cronExpression(parts[3])
                    .taskType(ScheduledTask.TaskType.valueOf(parts[4]))
                    .userId(parts[5])
                    .channelType(parts[6])
                    .triggerMessage(parts[7])
                    .status(ScheduledTask.TaskStatus.valueOf(parts[8]))
                    .createdAt(parseDateTime(parts[9]))
                    .lastRunAt(parseDateTime(parts[10]))
                    .nextRunAt(parseDateTime(parts[11]))
                    .executionCount(parts.length > 12 ? Integer.parseInt(parts[12]) : 0)
                    .build();
        } catch (Exception e) {
            log.warn("解析任务失败: {}", line, e);
            return null;
        }
    }

    private void saveExecutions() {
        // 简化实现，不持久化执行历史到磁盘
    }

    private String safe(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    private LocalDateTime parseDateTime(String str) {
        if (str == null || str.isEmpty()) return null;
        try {
            return LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }
}