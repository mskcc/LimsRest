package org.mskcc.limsrest.service.requesttracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Stage {
    String stage;
    int startingSamples;
    int endingSamples;
    Long startTime;
    Long endTime;

    public Boolean getComplete() {
        return complete;
    }

    Boolean complete;
    List<Step> steps;
    public Stage(String stage, int startingSamples, int endingSamples, Long startTime, Long endTime) {
        this.stage = stage;
        this.startingSamples = startingSamples;
        this.endingSamples = endingSamples;
        this.startTime = startTime;
        this.endTime = endTime;
        this.complete = Boolean.TRUE;       // Stage will be complete until found to not be complete

        steps = new ArrayList<>();
    }

    public String getStage() {
        return stage;
    }

    public int getStartingSamples() {
        return startingSamples;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public void setComplete(Boolean complete) {
        this.complete = complete;
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
        apiMap.put("endTime", this.endTime);
        apiMap.put("complete", this.complete);

        return apiMap;
    }
}
