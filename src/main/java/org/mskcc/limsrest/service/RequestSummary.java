package org.mskcc.limsrest.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
@Setter
public class RequestSummary {
    private List<SampleSummary> samples;
    private List<Long> deliveryDates;
    private Long recentDeliveryDate;
    private Long completedDate;
    private String cmoProjectId;
    private String requestId;
    private String requestType;
    private String recipe; // This is a sample level field, try to show at the request level
    private String investigator;
    private String pi;
    private String investigatorEmail;
    private String piEmail;
    private String projectManager;
    private String analysisType;
    private boolean analysisRequested;
    private Boolean isCmoRequest = Boolean.FALSE;
    private long recordId;
    private Long receivedDate;
    private Long dueDate;
    private Short sampleNumber;
    private String restStatus;
    private String specialDelivery;
    private String labHeadEmail;
    private String qcAccessEmail;
    private String dataAccessEmails;
    private Boolean isIgoComplete;
    private List<String> runFolders;
    private String requestName;

    public RequestSummary() {
        this("UNKNOWN");
    }

    public RequestSummary(String request) {
        samples = new ArrayList<>();
        deliveryDates = new LinkedList<>();
        requestId = request;
        investigator = "UNKNOWN";
        restStatus = "SUCCESS";
    }

    public Boolean getIsCmoRequest() { return isCmoRequest; }

    public void setIsCmoRequest(Boolean cmoRequest) {
        if (cmoRequest == null)
            isCmoRequest = Boolean.FALSE;
        else {
            this.isCmoRequest = cmoRequest;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    public List<String> getRunFolders() {
        return runFolders;
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public String getDataAccessEmails() {
        return dataAccessEmails;
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public Long getReceivedDate() {
        return receivedDate;
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public Long getDueDate() {
        return dueDate;
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public String getQcAccessEmail() {
        return qcAccessEmail;
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public String getLabHeadEmail() {
        return labHeadEmail;
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public Boolean getIsIgoComplete() {
        return this.isIgoComplete;
    }

    public List<SampleSummary> getSamples() {
        return samples;
    }

    public void addSample(SampleSummary s) {
        samples.add(s);
    }

    public void addDeliveryDate(Long date) {
        deliveryDates.add(date);
    }

    public List<Long> getDeliveryDate() {
        Collections.sort(deliveryDates);
        return deliveryDates;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Long getCompletedDate() {
        return completedDate;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Long getRecentDeliveryDate() {
        return recentDeliveryDate;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public boolean getAnalysisRequested() {
        return analysisRequested;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getAnalysisType() { return analysisType; }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public long getRecordId() {
        return recordId;
    }

    public String getRequestId() {
        return requestId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getCmoProject() {
        return cmoProjectId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getPi() {
        return pi;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getInvestigator() {
        return investigator;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getPiEmail() {
        return piEmail;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getProjectManager() {
        return projectManager;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getInvestigatorEmail() {
        return investigatorEmail;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Short getSampleNumber() {
        return sampleNumber;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSpecialDelivery() {
        return specialDelivery;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRequestType() {
        return requestType;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRecipe() {
        return recipe;
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public String getRequestName() {
        return this.requestName;
    }

    public String getRestStatus() {
        return this.restStatus;
    }

    public static RequestSummary errorMessage(String e) {
        RequestSummary rs = new RequestSummary("ERROR");
        SampleSummary ss = new SampleSummary();
        ss.addRequest(e);
        rs.addSample(ss);
        return rs;
    }
}