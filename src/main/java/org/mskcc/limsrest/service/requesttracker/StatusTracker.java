package org.mskcc.limsrest.service.requesttracker;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic class for monitoring the status of a LIMS entity (E.g. sample/stage)
 *
 * @author David Streid
 */
@Getter @Setter
public class StatusTracker {
    protected String stage;
    protected Integer size;           // How many records
    protected Boolean complete;
    protected Long startTime;
    protected Long updateTime;

    public Map<String, Object> toApiResponse() {
        Map<String, Object> apiMap = new HashMap<>();
        apiMap.put("totalSamples", this.size);
        apiMap.put("stage", this.stage);
        apiMap.put("complete", this.complete);
        apiMap.put("startTime", this.startTime);
        apiMap.put("updateTime", this.updateTime);

        return apiMap;
    }
}
