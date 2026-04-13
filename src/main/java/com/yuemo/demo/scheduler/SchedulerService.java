package com.yuemo.demo.scheduler;

import com.yuemo.demo.agent.AgentService;
import com.yuemo.demo.channel.ChannelManager;
import com.yuemo.demo.channel.ChannelType;
import com.yuemo.demo.channel.feishu.FeishuChannel;
import com.yuemo.demo.common.event.EventBus;
import com.yuemo.demo.common.event.definitions.ChannelEvents;
import com.yuemo.demo.common.event.definitions.MessageContext;
import com.yuemo.demo.common.event.definitions.MessageType;
import com.yuemo.demo.memory.SessionContextHolder;
import com.yuemo.demo.scheduler.entity.ScheduledTask;
import com.yuemo.demo.scheduler.entity.TaskExecutionHistory;
import com.yuemo.demo.scheduler.event.TaskEvents;
import com.yuemo.demo.scheduler.repository.ScheduledTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
public class SchedulerService {

    private final ScheduledTaskRepository taskRepository;
    private final TaskScheduler taskScheduler;
    private final EventBus eventBus;
    private final AgentService agentService;

    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public SchedulerService(ScheduledTaskRepository taskRepository,
                            TaskScheduler taskScheduler,
                            EventBus eventBus,
                            AgentService agentService) {
        this.taskRepository = taskRepository;
        this.taskScheduler = taskScheduler;
        this.eventBus = eventBus;
        this.agentService = agentService;
    }

    @PostConstruct
    public void init() {
        log.info("初始化定时任务调度服务...");
        loadAndScheduleTasks();
        log.info("定时任务调度服务初始化完成");
    }

    @PreDestroy
    public void destroy() {
        log.info("关闭定时任务调度服务...");
        scheduledTasks.values().forEach(future -> future.cancel(false));
        scheduledTasks.clear();
    }

    private void loadAndScheduleTasks() {
        List<ScheduledTask> activeTasks = taskRepository.findActiveTasks();
        for (ScheduledTask task : activeTasks) {
            scheduleTask(task);
        }
        log.info("已加载并调度 {} 个活跃任务", activeTasks.size());
    }

    public ScheduledTask createTask(ScheduledTask task) {
        ScheduledTask savedTask = taskRepository.save(task);
        if (savedTask.getStatus() == ScheduledTask.TaskStatus.ACTIVE) {
            scheduleTask(savedTask);
        }
        log.info("创建定时任务: {} (ID: {})", savedTask.getName(), savedTask.getId());
        return savedTask;
    }

    public ScheduledTask createCronTask(String name, String cronExpression, String userId,
                                        String channelType, String triggerMessage) {
        ScheduledTask task = ScheduledTask.builder()
                .name(name)
                .cronExpression(cronExpression)
                .taskType(ScheduledTask.TaskType.CRON)
                .userId(userId)
                .channelType(channelType)
                .triggerMessage(triggerMessage)
                .status(ScheduledTask.TaskStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .executionCount(0)
                .build();
        return createTask(task);
    }

    public ScheduledTask createOneTimeTask(String name, long delayMs, String userId,
                                           String channelType, String triggerMessage) {
        ScheduledTask task = ScheduledTask.builder()
                .name(name)
                .taskType(ScheduledTask.TaskType.ONE_TIME)
                .userId(userId)
                .channelType(channelType)
                .triggerMessage(triggerMessage)
                .status(ScheduledTask.TaskStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .executionCount(0)
                .build();
        ScheduledTask savedTask = taskRepository.save(task);

        taskScheduler.schedule(() -> executeTask(savedTask), Instant.ofEpochMilli(System.currentTimeMillis() + delayMs));
        log.info("创建一次性任务: {} (ID: {}), 延迟: {}ms", name, savedTask.getId(), delayMs);
        return savedTask;
    }

    private void scheduleTask(ScheduledTask task) {
        if (task.getCronExpression() == null || task.getCronExpression().isEmpty()) {
            log.warn("任务 {} 没有 cron 表达式，跳过调度", task.getName());
            return;
        }

        try {
            CronTrigger trigger = new CronTrigger(task.getCronExpression(), ZoneId.systemDefault());
            ScheduledFuture<?> future = taskScheduler.schedule(() -> executeTask(task), trigger);
            scheduledTasks.put(task.getId(), future);
            log.info("任务已调度: {} (cron: {})", task.getName(), task.getCronExpression());
        } catch (Exception e) {
            log.error("调度任务失败: {}", task.getName(), e);
        }
    }

    public void executeTask(ScheduledTask task) {
        log.info("执行定时任务: {} (ID: {})", task.getName(), task.getId());

        TaskExecutionHistory execution = TaskExecutionHistory.builder()
                .id("exec_" + System.currentTimeMillis())
                .taskId(task.getId())
                .taskName(task.getName())
                .startTime(LocalDateTime.now())
                .status(TaskExecutionHistory.ExecutionStatus.RUNNING)
                .build();

        eventBus.publish(new TaskEvents.TaskTriggeredEvent(task));

        try {
            SessionContextHolder.setContext("scheduled_" + task.getId(), task.getUserId());

            String response = agentService.processMessage(task.getTriggerMessage());

            execution.markSuccess(response);
            task.incrementExecutionCount();
            task.updateLastRun();

            if (task.getTaskType() == ScheduledTask.TaskType.ONE_TIME) {
                task.setStatus(ScheduledTask.TaskStatus.COMPLETED);
                cancelTaskSchedule(task.getId());
            }

            taskRepository.update(task);
            taskRepository.saveExecution(execution);

            sendTaskResult(task, response);

            eventBus.publish(new TaskEvents.TaskExecutedEvent(task, execution));

            log.info("任务执行成功: {} (ID: {})", task.getName(), task.getId());

        } catch (Exception e) {
            log.error("任务执行失败: {} (ID: {})", task.getName(), task.getId(), e);
            execution.markFailed(e.getMessage());
            task.setStatus(ScheduledTask.TaskStatus.FAILED);
            taskRepository.update(task);
            taskRepository.saveExecution(execution);

            sendTaskResult(task, "任务执行失败: " + e.getMessage());
            eventBus.publish(new TaskEvents.TaskFailedEvent(task, execution, e.getMessage()));
        } finally {
            SessionContextHolder.clear();
        }
    }

    private void sendTaskResult(ScheduledTask task, String result) {
        try {
            ChannelType channelType = ChannelType.valueOf(task.getChannelType());
            MessageContext context = new MessageContext();
            context.setChannelType(channelType);
            context.setUserId("system");
            context.setUserName("系统");
            context.setTargetUserId(task.getUserId());
            context.setContent("【定时任务: " + task.getName() + "】\n\n" + result);
            context.setMessageType(MessageType.TEXT);

            eventBus.publish(ChannelEvents.createToolMessageRequestEvent(context));
        } catch (Exception e) {
            log.error("发送任务结果失败", e);
        }
    }

    public void pauseTask(String taskId) {
        taskRepository.findById(taskId).ifPresent(task -> {
            task.setStatus(ScheduledTask.TaskStatus.PAUSED);
            taskRepository.update(task);
            cancelTaskSchedule(taskId);
            log.info("任务已暂停: {}", task.getName());
        });
    }

    public void resumeTask(String taskId) {
        taskRepository.findById(taskId).ifPresent(task -> {
            task.setStatus(ScheduledTask.TaskStatus.ACTIVE);
            taskRepository.update(task);
            scheduleTask(task);
            log.info("任务已恢复: {}", task.getName());
        });
    }

    public void cancelTask(String taskId) {
        taskRepository.findById(taskId).ifPresent(task -> {
            task.setStatus(ScheduledTask.TaskStatus.COMPLETED);
            taskRepository.update(task);
            cancelTaskSchedule(taskId);
            log.info("任务已取消: {}", task.getName());
        });
    }

    private void cancelTaskSchedule(String taskId) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }
    }

    public List<ScheduledTask> getUserTasks(String userId) {
        return taskRepository.findByUserId(userId);
    }

    public List<ScheduledTask> getAllTasks() {
        return taskRepository.findAll();
    }

    public ScheduledTask getTask(String taskId) {
        return taskRepository.findById(taskId).orElse(null);
    }

    public List<TaskExecutionHistory> getTaskHistory(String taskId) {
        return taskRepository.findRecentExecutions(taskId, 10);
    }

    public void deleteTask(String taskId) {
        cancelTaskSchedule(taskId);
        taskRepository.delete(taskId);
        log.info("任务已删除: {}", taskId);
    }

    @Scheduled(fixedDelay = 60000)
    public void checkPendingTasks() {
        List<ScheduledTask> tasks = taskRepository.findActiveTasks();
        for (ScheduledTask task : tasks) {
            if (!scheduledTasks.containsKey(task.getId()) &&
                    task.getCronExpression() != null &&
                    !task.getCronExpression().isEmpty()) {
                log.info("重新调度任务: {}", task.getName());
                scheduleTask(task);
            }
        }
    }
}