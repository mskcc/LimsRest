package org.mskcc.limsrest.service.requesttracker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.service.GetRequestTrackingTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Request {
    private static Log log = LogFactory.getLog(Request.class);

    private String requestId;
    private String bankedSampleId;
    private long deliveryDate;
    private long receivedDate;
    private boolean igoComplete;
    private String nameOfLaboratoryHead;
    private String nameOfInvestigator;

    public Map<String, Stage> getStages() {
        return stages;
    }

    public void setStages(Map<String, Stage> stages) {
        this.stages = stages;
    }

    public void addStage(String stageName, Stage stage){
        if(this.stages.containsKey(stageName)){
            log.warn(String.format("Overriding stage: %s recorded for record: %s", stageName, this.requestId));
        }
        this.stages.put(stageName, stage);
    }

    private Map<String, Stage> stages;
    private List<SampleTracker> samples;

    public Request(String requestId, String bankedSampleId) {
        this.requestId = requestId;
        this.igoComplete = false;
        this.samples = new ArrayList<>();
        this.stages = new HashMap<>();
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
        Map<String, Stage> stages;
        for (SampleTracker tracker : samples) {
            stages = tracker.getStages();
            for (Map.Entry<String, Stage> entry : stages.entrySet()) {

            }
        }
    }

    public void setIgoComplete(boolean igoComplete) {
        this.igoComplete = igoComplete;
    }

    public void setDeliveryDate(long deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public void setReceivedDate(long receivedDate) {
        this.receivedDate = receivedDate;
    }

    public Map<String, Object> toApiResponse() {
        Map<String, Object> apiResponse = new HashMap<>();

        // apiResponse.put("samples", this.samples);
        // apiResponse.put("isComplete", this.isComplete);
        // apiResponse.put("steps", this.steps);
        // apiResponse.put("startTime", this.startTime);
        // apiResponse.put("updateTime", this.updateTime);
        // apiResponse.put("igoComplete", isIgoComplete());
        // apiResponse.put("deliveryDate", this.deliveryDate);
        apiResponse.put("receivedDate", this.receivedDate);
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
