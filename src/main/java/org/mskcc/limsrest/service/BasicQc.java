package org.mskcc.limsrest.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Setter;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
@Setter
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
    private Integer numOfComments;

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

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Long getCreateDate() {
        return this.createDate;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRun() {
        return this.run;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSampleName() {
        return this.sampleName;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getQcStatus() {
        return this.qcStatus;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRestStatus() {
        return this.restStatus;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getFileLocation() {
        return this.fileLocation;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Long getTotalReads() {
        return this.totalReads;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Integer getNumOfcomments() {
        return this.numOfComments;
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