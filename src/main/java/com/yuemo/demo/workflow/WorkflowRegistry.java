package com.yuemo.demo.workflow;

import com.yuemo.demo.workflow.definition.WorkflowDefinition;
import com.yuemo.demo.workflow.definition.WorkflowDefinition.StepType;
import com.yuemo.demo.workflow.definition.WorkflowDefinition.Trigger;
import com.yuemo.demo.workflow.definition.WorkflowDefinition.TriggerType;
import com.yuemo.demo.workflow.definition.WorkflowStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Slf4j
@Component
public class WorkflowRegistry {

    private static final String WORKFLOWS_DIR = "workspace/workflows";

    private final Map<String, WorkflowDefinition> workflows = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadWorkflows();
    }

    public void loadWorkflows() {
        try {
            Files.createDirectories(Paths.get(WORKFLOWS_DIR));
            Path dir = Paths.get(WORKFLOWS_DIR);

            if (!Files.exists(dir)) {
                log.info("工作流目录不存在: {}", WORKFLOWS_DIR);
                return;
            }

            try (Stream<Path> paths = Files.walk(dir)) {
                paths.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                        .forEach(this::loadWorkflowFile);
            }

            log.info("工作流加载完成，共 {} 个工作流", workflows.size());
        } catch (IOException e) {
            log.error("加载工作流失败", e);
        }
    }

    private void loadWorkflowFile(Path file) {
        try {
            String content = Files.readString(file);
            WorkflowDefinition workflow = parseYamlWorkflow(content, file.getFileName().toString());
            if (workflow != null && workflow.getId() != null) {
                workflows.put(workflow.getId(), workflow);
                log.info("加载工作流: {} ({})", workflow.getName(), workflow.getId());
            }
        } catch (Exception e) {
            log.error("加载工作流文件失败: {}", file, e);
        }
    }

    private WorkflowDefinition parseYamlWorkflow(String yaml, String filename) {
        try {
            Map<String, Object> data = parseSimpleYaml(yaml);

            WorkflowDefinition.WorkflowDefinitionBuilder builder = WorkflowDefinition.builder();

            builder.id(getString(data, "id", filename.replace(".yaml", "").replace(".yml", "")));
            builder.name(getString(data, "name", "未命名工作流"));
            builder.description(getString(data, "description", ""));
            builder.version(getString(data, "version", "1.0"));

            Trigger trigger = parseTrigger(data);
            builder.trigger(trigger);

            List<WorkflowStep> steps = parseSteps(data);
            builder.steps(steps);

            return builder.build();
        } catch (Exception e) {
            log.error("解析工作流 YAML 失败: {}", filename, e);
            return null;
        }
    }

    private Trigger parseTrigger(Map<String, Object> data) {
        Object triggerObj = data.get("trigger");
        if (triggerObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> triggerMap = (Map<String, Object>) triggerObj;
            return Trigger.builder()
                    .type(TriggerType.valueOf(getString(triggerMap, "type", "MANUAL").toUpperCase()))
                    .expression(getString(triggerMap, "expression", ""))
                    .channelType(getString(triggerMap, "channel", ""))
                    .build();
        }
        return Trigger.builder()
                .type(TriggerType.MANUAL)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<WorkflowStep> parseSteps(Map<String, Object> data) {
        List<WorkflowStep> steps = new ArrayList<>();
        Object stepsObj = data.get("steps");

        if (stepsObj instanceof List) {
            List<?> stepsList = (List<?>) stepsObj;
            int index = 0;
            for (Object stepObj : stepsList) {
                if (stepObj instanceof Map) {
                    Map<String, Object> stepMap = (Map<String, Object>) stepObj;
                    WorkflowStep step = WorkflowStep.builder()
                            .id(getString(stepMap, "id", "step_" + index))
                            .name(getString(stepMap, "name", "步骤 " + index))
                            .type(parseStepType(getString(stepMap, "type", "TOOL")))
                            .config(new HashMap<>(stepMap))
                            .nextStepId(getString(stepMap, "next", null))
                            .errorStepId(getString(stepMap, "on_error", null))
                            .build();
                    steps.add(step);
                    index++;
                }
            }
        }

        return steps;
    }

    private StepType parseStepType(String type) {
        try {
            return StepType.valueOf(type.toUpperCase());
        } catch (Exception e) {
            return StepType.TOOL;
        }
    }

    private Map<String, Object> parseSimpleYaml(String yaml) {
        Map<String, Object> result = new HashMap<>();
        List<String> lines = yaml.lines().toList();

        List<String> currentSection = null;
        String currentKey = null;
        List<Map<String, Object>> currentList = null;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            if (line.contains(":")) {
                String key = line.substring(0, line.indexOf(":")).trim();
                String value = line.substring(line.indexOf(":") + 1).trim();

                if (value.isEmpty()) {
                    currentKey = key;
                    currentSection = new ArrayList<>();
                    result.put(key, currentSection);
                } else {
                    result.put(key, unwrapYamlValue(value));
                }
            }
        }

        return result;
    }

    private Object unwrapYamlValue(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        if (value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.length() - 1);
        }
        if (value.equals("true")) return true;
        if (value.equals("false")) return false;
        if (value.matches("\\d+")) return Integer.parseInt(value);
        if (value.matches("\\d+\\.\\d+")) return Double.parseDouble(value);
        return value;
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    public WorkflowDefinition getWorkflow(String id) {
        return workflows.get(id);
    }

    public List<WorkflowDefinition> getAllWorkflows() {
        return new ArrayList<>(workflows.values());
    }

    public void registerWorkflow(WorkflowDefinition workflow) {
        workflows.put(workflow.getId(), workflow);
        log.info("注册工作流: {} ({})", workflow.getName(), workflow.getId());
    }

    public void unregisterWorkflow(String id) {
        workflows.remove(id);
        log.info("注销工作流: {}", id);
    }

    public void reloadWorkflows() {
        workflows.clear();
        loadWorkflows();
    }
}