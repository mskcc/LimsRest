package org.mskcc.limsrest.service.requesttracker;

public class Tracker {
    protected String stage;
    protected Integer size;           // How many records
    protected Boolean complete;
    protected Long startTime;
    protected Long updateTime;

    public Tracker() {
        this.complete = Boolean.TRUE;       // Stage will be complete until found to not be complete
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
}
