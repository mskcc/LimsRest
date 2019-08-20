package org.mskcc.limsrest.service;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BasicQc {
    private String alias;
    private String run;
    private String sampleName;
    private String qcStatus;
    private String restStatus;
    private String fileLocation;
    private Long totalReads;
    private Long createDate;
    private HashMap<Long, String> statusEvents;

    public BasicQc() {
        restStatus = "SUCCESS";
        statusEvents = new HashMap<>();
    }

    public void putStatusEvent(Long date, String newStatus) {
        statusEvents.put(date, newStatus);
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getAlias() {
        return this.alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Long getCreateDate() {
        return this.createDate;
    }

    public void setCreateDate(long createDate) {
        this.createDate = createDate;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRun() {
        return this.run;
    }

    public void setRun(String run) {
        this.run = run;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSampleName() {
        return this.sampleName;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getQcStatus() {
        return this.qcStatus;
    }

    public void setQcStatus(String qcStatus) {
        this.qcStatus = qcStatus;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRestStatus() {
        return this.restStatus;
    }

    public void setRestStatus(String restStatus) {
        this.restStatus = restStatus;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getFileLocation() {
        return this.fileLocation;
    }

    public void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Long getTotalReads() {
        return this.totalReads;
    }

    public void setTotalReads(Long reads) {
        this.totalReads = reads;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<QcStatusEvent> getReviewedDates() {
        LinkedList<QcStatusEvent> reviewedDates = new LinkedList<>();
        for (Map.Entry<Long, String> eventPair : statusEvents.entrySet()) {
            reviewedDates.add(new QcStatusEvent(eventPair.getKey(), eventPair.getValue()));
        }
        Collections.sort(reviewedDates);
        return reviewedDates;
    }
}