package net.redborder.correlation.siddhi;

import net.redborder.correlation.siddhi.exceptions.ExecutionPlanException;
import net.redborder.correlation.siddhi.exceptions.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.siddhi.core.ExecutionPlanRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.stream.output.StreamCallback;
import org.wso2.siddhi.query.api.definition.Attribute;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExecutionPlan {
    private final Logger log = LoggerFactory.getLogger(ExecutionPlan.class);

    private final List<String> inputTopics;
    private final Map<String, String> outputTopics;
    private final String plan, id;
    private ExecutionPlanRuntime executionPlanRuntime;

    public static ExecutionPlan fromMap(Map<String, Object> map) throws ExecutionPlanException {
        List<String> inputTopics = (List<String>) map.get("input");
        Map<String, String> outputTopics = (Map<String, String>) map.get("output");
        String id = (String) map.get("id");
        String plan = (String) map.get("executionPlan");

        if (inputTopics == null || outputTopics == null || id == null || plan == null) {
            throw new TransformException("required field not found");
        }

        return new ExecutionPlan(id, inputTopics, outputTopics, plan);
    }

    public ExecutionPlan(String id, List<String> inputTopics,
                         Map<String, String> outputTopics, String plan) {
        this.id = id;
        this.inputTopics = inputTopics;
        this.outputTopics = outputTopics;
        this.plan = plan;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("input", inputTopics);
        result.put("output", outputTopics);
        result.put("executionPlan", plan);
        return result;
    }

    public List<String> getInputTopics() {
        return Collections.unmodifiableList(inputTopics);
    }

    public Map<String, String> getOutputTopics() {
        return Collections.unmodifiableMap(outputTopics);
    }

    public String getId() {
        return id;
    }

    public String getPlan() {
        return this.plan;
    }

    private String getFullPlan() {
        String executionPlan = "@config(async = 'true') define stream raw_rb_flow (src string, dst string, namespace_uuid string, bytes int);";
        executionPlan += "from raw_rb_flow[namespace_uuid == '11111111'] select src, dst, bytes insert into rb_flow;";
        executionPlan += this.plan;
        return executionPlan;
    }

    public void start(SiddhiManager siddhiManager) {
        this.executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(getFullPlan());

        for (Map.Entry<String, String> entry : outputTopics.entrySet()) {
            String streamName = entry.getKey();
            List<Attribute> attributes = executionPlanRuntime.getStreamDefinitionMap().get(streamName).getAttributeList();
            StreamCallback streamCallback = new PrintCallback(getId(), streamName, attributes);
            executionPlanRuntime.addCallback(streamName, streamCallback);
        }

        executionPlanRuntime.start();
        log.info("Started execution plan with id {} and plan {}", id, getPlan());
    }

    public void stop() {
        this.executionPlanRuntime.shutdown();
        log.info("Stopped execution plan {}", id);
    }

    public InputHandler getInputHandler() {
        return executionPlanRuntime.getInputHandler("raw_rb_flow");
    }
}