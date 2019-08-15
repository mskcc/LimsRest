package org.mskcc.limsrest.service;

public class DeliveryEmail {
    String projectTitle;
    String requestIDLIMS;
    String mergeID;
    String investigatorEmail;
    String sequencingApplication; // display name "Project Applications"

    String errorMessage = null;

    public DeliveryEmail(String requestIDLIMS, String projectTitle, String mergeID, String investigatorEmail, String sequencingApplication) {
        this.projectTitle = projectTitle;
        this.requestIDLIMS = requestIDLIMS;
        this.mergeID = mergeID;
        this.investigatorEmail = investigatorEmail;
        this.sequencingApplication = sequencingApplication;
    }

    public DeliveryEmail(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public String getRequestIDLIMS() {
        return requestIDLIMS;
    }

    public void setRequestIDLIMS(String requestIDLIMS) {
        this.requestIDLIMS = requestIDLIMS;
    }

    public String getMergeID() {
        return mergeID;
    }

    public void setMergeID(String mergeID) {
        this.mergeID = mergeID;
    }

    public String getInvestigatorEmail() {
        return investigatorEmail;
    }

    public void setInvestigatorEmail(String investigatorEmail) {
        this.investigatorEmail = investigatorEmail;
    }

    public String getSequencingApplication() {
        return sequencingApplication;
    }

    public void setSequencingApplication(String sequencingApplication) {
        this.sequencingApplication = sequencingApplication;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}