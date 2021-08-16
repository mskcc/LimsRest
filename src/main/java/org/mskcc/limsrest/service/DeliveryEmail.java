package org.mskcc.limsrest.service;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
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
}