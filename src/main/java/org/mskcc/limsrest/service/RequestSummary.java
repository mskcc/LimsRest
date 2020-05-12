package org.mskcc.limsrest.service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;

import com.fasterxml.jackson.annotation.*;

public class RequestSummary {
    private ArrayList<SampleSummary> samples;
    private LinkedList<Long> deliveryDates;
    private String cmoProjectId;
    private String requestId;
    private String requestType;
    private String investigator;
    private String pi;
    private String investigatorEmail;
    private String piEmail;
    private String projectManager;
    private String analysisType;
    private boolean pipelinable;
    private boolean analysisRequested;
    private long recordId;
    private Integer sampleNumber;
    private String restStatus;
    private String specialDelivery;

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

    public void setRestStatus(String s) {
        this.restStatus = s;
    }

    public void setSpecialDelivery(String s) {
        this.specialDelivery = s;
    }

    public ArrayList<SampleSummary> getSamples() {
        return samples;
    }

    public void addSample(SampleSummary s) {
        samples.add(s);
    }

    public void addDeliveryDate(Long date) {
        deliveryDates.add(date);
    }

    public void setAnalysisRequested(boolean req) {
        this.analysisRequested = req;
    }

    public void setAnalysisType(String s) { this.analysisType = s;  }

    public void setRecordId(long id) {
        this.recordId = id;
    }

    public void setRequestType(String s) {
        this.requestType = s;
    }

    public void setCmoProject(String proj) {
        cmoProjectId = proj;
    }

    public void setPi(String name) {
        pi = name;
    }

    public void setProjectManager(String name) {
        projectManager = name;
    }

    public void setAutorunnable(boolean pipelinable) {
        this.pipelinable = pipelinable;
    }

    public void setInvestigator(String name) {
        investigator = name;
    }

    public void setPiEmail(String email) {
        piEmail = email;
    }

    public void setInvestigatorEmail(String email) {
        investigatorEmail = email;
    }

    public void setSampleNumber(Integer sampleNumber) {
        this.sampleNumber = sampleNumber;
    }

    public List<Long> getDeliveryDate() {
        Collections.sort(deliveryDates);
        return deliveryDates;
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
    public boolean getAutorunnable() {
        return pipelinable;
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
    public Integer getSampleNumber() {
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