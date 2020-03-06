package org.mskcc.limsrest.service.requesttracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Stage extends Tracker {
    String stage;
    int startingSamples;
    int endingSamples;

    public Boolean getComplete() {
        return complete;
    }

    Boolean complete;
    List<Step> steps;
    public Stage(String stage, int startingSamples, int endingSamples, Long startTime, Long updateTime) {
        this.stage = stage;
        this.startingSamples = startingSamples;
        this.endingSamples = endingSamples;
        this.startTime = startTime;
        this.updateTime = updateTime;

        steps = new ArrayList<>();
    }

    public String getStage() {
        return stage;
    }

    public void addStartingSample(Integer count) {
        this.startingSamples += count;
    }

    public void addEndingSample(Integer count) {
        this.endingSamples += count;
    }

    public void addStep(Step step) {
        steps.add(step);
    }

    public Map<String, Object> toApiResponse() {
        Map<String, Object> apiMap = new HashMap<>();
        apiMap.put("stage", this.stage);
        apiMap.put("startingSamples", this.startingSamples);
        apiMap.put("endingSamples", this.endingSamples);
        apiMap.put("startTime", this.startTime);
        apiMap.put("updateTime", this.updateTime);
        apiMap.put("complete", this.complete);

        return apiMap;
    }
}
