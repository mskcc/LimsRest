package org.mskcc.limsrest.service.requesttracker;

import java.util.HashMap;
import java.util.Map;

public class StageTracker {
    protected String stage;
    protected Integer size;           // How many records
    protected Boolean complete;
    protected Long startTime;
    protected Long updateTime;

    public StageTracker() {
        this.complete = Boolean.FALSE;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Boolean getComplete() {
        return complete;
    }

    public void setComplete(Boolean complete) {
        this.complete = complete;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

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
