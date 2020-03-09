package org.mskcc.limsrest.service.requesttracker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SampleStageTracker extends StageTracker {
    private static Log log = LogFactory.getLog(SampleStageTracker.class);
    Integer endingSamples;
    List<Step> steps;
    public SampleStageTracker(String stage, Integer startingSamples, Integer endingSamples, Long startTime, Long updateTime) {
        setStage(stage);
        setSize(startingSamples);   // The stage has a size equal to the number of samples it contains

        this.endingSamples = endingSamples;
        this.startTime = startTime;
        this.updateTime = updateTime;
        this.steps = new ArrayList<>();
    }

    public Integer getEndingSamples() {
        return endingSamples;
    }

    public String getStage() {
        return stage;
    }

    public void addStartingSample(Integer count) {
        if (count == null) {
            log.error("Failed to add a null-count size to the startingSamples of Stage");
            return;
        }
        super.size += count;
    }

    public void addEndingSample(Integer count) {
        if (count == null) {
            log.error("Failed to add a null-count size to the endingSample of Stage");
            return;
        }
        this.endingSamples += count;
    }

    public void addStep(Step step) {
        steps.add(step);
    }

    public void updateStage(StageTracker stageTracker) {
        // Integer size = tracker.getSize();

        // Update the start & end times of the stage
        // addStartingSample(size);
        // setSize(getSize()+size);

        Long startTime = stageTracker.getStartTime();
        Long updateTime = stageTracker.getUpdateTime();
        if (this.startTime > startTime) {
            setStartTime(startTime);
        }
        if (this.updateTime < updateTime) {
            setUpdateTime(updateTime);
        }
    }

    public Map<String, Object> toApiResponse() {
        Map<String, Object> apiMap = super.toApiResponse();
        apiMap.put("completedSamples", this.endingSamples);

        return apiMap;
    }
}
