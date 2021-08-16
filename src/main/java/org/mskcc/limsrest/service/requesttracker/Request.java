package org.mskcc.limsrest.service.requesttracker;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.mskcc.limsrest.util.StatusTrackerConfig.StageComp;

/**
 * API representation of a Request in the LIMs with tracking data
 *
 * @author David Streid
 */
@Getter @Setter
public class Request {
    private static Log log = LogFactory.getLog(Request.class);

    @Getter(AccessLevel.NONE) private String requestId;
    @Setter(AccessLevel.NONE) private Map<String, StageTracker> stages;
    private List<ProjectSample> samples;                // Tree of samples
    @Getter(AccessLevel.NONE) private Map<String, Object> metaData;               // Summary of metaData
    @Getter(AccessLevel.NONE) private Map<String, Object> summary;                // Summary of overall project status

    public Request(String requestId) {
        this.requestId = requestId;
        this.samples = new ArrayList<>();
        this.stages = new TreeMap<>(new StageComp());
        this.metaData = new HashMap<>();
    }

    /**
     * Adds a stage to the request
     *
     * @param stageName
     * @param stage
     */
    public void addStage(String stageName, StageTracker stage) {
        if (this.stages.containsKey(stageName)) {
            log.warn(String.format("Overriding stage: %s recorded for record: %s", stageName, this.requestId));
        }
        this.stages.put(stageName, stage);
    }

    public Map<String, StageTracker> getStages() {
        Map<String, StageTracker> cloned = new TreeMap<>(new StageComp());
        for(Map.Entry<String, StageTracker> entry : this.stages.entrySet()){
            cloned.put(entry.getKey(), entry.getValue());
        }
        return cloned;

    }

    public Map<String, Object> toApiResponse() {
        Map<String, Object> apiResponse = new HashMap<>();

        apiResponse.put("requestId", this.requestId);
        apiResponse.put("summary", this.summary);
        apiResponse.put("metaData", this.metaData);
        apiResponse.put("samples", this.samples.stream().map(tracker -> tracker.toApiResponse()).collect(Collectors.toList()));
        apiResponse.put("stages", this.stages.values().stream().map(
                stage -> stage.toApiResponse(false)
        ).collect(Collectors.toList()));

        return apiResponse;
    }
}
