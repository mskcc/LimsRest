package org.mskcc.limsrest.service;

import java.util.ArrayList;
import com.fasterxml.jackson.annotation.*;
import lombok.Setter;

@Setter
public class RequestDetailed {
   private ArrayList<SampleSummary> samples;
   private String projectName;
   private String applications;
   private String clinicalCorrelative;
   private String costCenter;
   private String fundNumber;
   private String contactName;
   private String dataAnalyst;
   private String dataDeliveryType;
   private String cmoProjectId;
   private String cmoContactName;
   private String cmoPiName;
   private String cmoPiEmail;
   private String requestId;
   private String furthest;  
   private String investigator;
   private String irbWaiverComments;
   private String dataAccessEmails;
   private String qcAccessEmails;
   private String pi;
   private String projectNotes;
   private String group;
   private String groupLeader;
   private String investigatorEmail;
   private String irbId;
   private String piEmail;
   private String projectManager;
   private String requestDescription;
   private String requestDetails;
   private String room;
   private String requestType;
   private String sampleType;
   private String status;
   private String telephoneNum;
   private String tatFromProcessing;
   private String tatFromReceiving;
   private String servicesRequested;
   private String studyId;
   private String communicationNotes;
   private String analysisType;
   private long completedDate;
   private long deliveryDate;
   private long recievedDate;
   private long investigatorDate;
   private long inprocessDate;
   private long ilabsRequestDate;
   private long irbDate;
   private long samplesReceivedDate;
   private boolean fastqRequested;

   public RequestDetailed(){
     this("UNKNOWN");
   }

   public RequestDetailed( String request){
        samples = new ArrayList<>();
        requestId = request;
        investigator = "UNKNOWN";
   }
   public void setReceivedDate(long recievedDate){
	this.recievedDate = recievedDate;
   }
   public ArrayList<SampleSummary> getSamples(){
    return samples;
  }
   public String getRequestId(){
    return requestId;
  }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getApplications(){
	return applications;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getClinicalCorrelative(){
	return clinicalCorrelative;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getCostCenter(){
	return costCenter;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getFundNumber(){
	return fundNumber;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getContactName(){
	return contactName;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getDataAnalyst(){
	return dataAnalyst;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getDataDeliveryType(){
	return dataDeliveryType;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getAnalysisType(){ return analysisType; }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getCmoContactName(){
     return cmoContactName;
  }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getCmoPiName(){
     return cmoPiName;
  }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getCmoPiEmail(){
     return cmoPiEmail;
  }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getCmoProjectId(){
	return cmoProjectId;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getInvestigator(){
	return investigator;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getIrbWaiverComments(){
	return irbWaiverComments;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getDataAccessEmails(){ return dataAccessEmails; }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getQcAccessEmails(){ return qcAccessEmails; }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getPi(){
	return pi;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getProjectNotes(){
	return projectNotes;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getGroup(){
	return group;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getGroupLeader(){
	return groupLeader;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getInvestigatorEmail(){
	return investigatorEmail;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getIrbId(){
	return irbId;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getPiEmail(){
	return piEmail;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getProjectName(){
    return projectName;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getProjectManager(){
	return projectManager;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getRequestDescription(){
	return requestDescription;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getRequestDetails(){
	return requestDetails;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getRoom(){
	return room;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getRequestType(){
	return requestType;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getSampleType(){
	return sampleType;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getStatus(){
	return status;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getFurthestSample(){
     return furthest;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getTelephoneNum(){
     return telephoneNum;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getTatFromProcessing(){
	return tatFromProcessing;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getTatFromReceiving(){
	return tatFromReceiving;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getServicesRequested(){
	return servicesRequested;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getStudyId(){
	return studyId;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getCommunicationNotes(){
	return communicationNotes;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public long getCompletedDate(){
	return completedDate;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public long getDeliveryDate(){
	return deliveryDate;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public long getReceivedDate(){
	return recievedDate;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public long getInvestigatorDate(){
	return investigatorDate;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public long getInprocessDate(){
	return inprocessDate;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public long getIlabsRequestDate(){
	return ilabsRequestDate;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public long getIrbDate(){
     return irbDate;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public long getSamplesReceivedDate(){
	return samplesReceivedDate;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public boolean getFastqRequested(){
	return fastqRequested;
   }
  public static RequestDetailed errorMessage(String e){
    RequestDetailed rd = new RequestDetailed("ERROR");
    rd.setInvestigator(e);
    return rd;
  }
}
