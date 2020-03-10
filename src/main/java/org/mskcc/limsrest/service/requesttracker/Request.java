package org.mskcc.limsrest.service.requesttracker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.stream.Collectors;

public class Request {
    private static Log log = LogFactory.getLog(Request.class);

    private String requestId;
    private String bankedSampleId;
    private Map<String, Object> metaData;
    private boolean igoComplete;
    private Map<String, SampleStageTracker> stages;
    private List<SampleTracker> samples;

    public Request(String requestId, String bankedSampleId) {
        this.requestId = requestId;
        this.bankedSampleId = bankedSampleId;
        this.igoComplete = false;        // Request is incomplete - must have a mostRecentDelvieryDate
        this.samples = new ArrayList<>();
        this.stages = new TreeMap<>(new StatusTrackerConfig.StageComp());
        this.metaData = new HashMap<>();
    }

    public Map<String, Object> getMetaData() {
        return metaData;
    }

    public void setMetaData(Map<String, Object> metaData) {
        this.metaData = metaData;
    }

    public boolean isIgoComplete() {
        return igoComplete;
    }

    public void setIgoComplete(boolean igoComplete) {
        this.igoComplete = igoComplete;
    }

    public Map<String, SampleStageTracker> getStages() {
        return stages;
    }

    public void setStages(Map<String, SampleStageTracker> stages) {
        this.stages = stages;
    }

    public void addStage(String stageName, SampleStageTracker stage) {
        if (this.stages.containsKey(stageName)) {
            log.warn(String.format("Overriding stage: %s recorded for record: %s", stageName, this.requestId));
        }
        this.stages.put(stageName, stage);
    }

    public List<SampleTracker> getSamples() {
        return samples;
    }

    public void setSamples(List<SampleTracker> samples) {
        this.samples = samples;
    }

    public void addSampleTracker(SampleTracker tracker) {
        this.samples.add(tracker);
    }

    /**
     * Calculates the stages from its internal state of stages
     */
    public void calculateStages() {
        Map<String, SampleStageTracker> stages;
        for (SampleTracker tracker : samples) {
            stages = tracker.getStages();
            for (Map.Entry<String, SampleStageTracker> entry : stages.entrySet()) {

            }
        }
    }

    public Map<String, Object> toApiResponse() {
        Map<String, Object> apiResponse = new HashMap<>();

        apiResponse.put("requestId", this.requestId);
        apiResponse.put("bankedSampleId", this.bankedSampleId);
        apiResponse.put("igoComplete", this.igoComplete);
        apiResponse.put("metaData", this.metaData);
        apiResponse.put("samples", this.samples.stream().map(tracker -> tracker.toApiResponse()).collect(Collectors.toList()));
        apiResponse.put("stages", this.stages.values().stream().map(
                stage -> stage.toApiResponse()
        ).collect(Collectors.toList()));

        return apiResponse;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
