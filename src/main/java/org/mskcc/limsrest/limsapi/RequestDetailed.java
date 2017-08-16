package org.mskcc.limsrest.limsapi;

import java.util.ArrayList;
import com.fasterxml.jackson.annotation.*;

public class RequestDetailed {

   private ArrayList<SampleSummary> samples;
   private String projectName;

   private String applications;
   private String bicReadme;
   private String clinicalCorrelative;
   private String costCenter;
   private String fundNumber;
   private String contactName;
   private String dataAnalyst;
   private String dataAnalystEmail;
   private String dataDeliveryType;
   private String cmoProjectId;
   private String cmoContactName;
   private String cmoPiName;
   private String cmoPiEmail;
   private String requestId;
   private String faxNumber;
   private String furthest;  
   private String investigator;
   private String irbWaiverComments;
   private String mailTo;
   private String pi;
   private String projectNotes;
   private String group;
   private String groupLeader;
   private String investigatorEmail;
   private String irbId;
   private String irbVerifier;
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



   private long completedDate;
   private long deliveryDate;
   private long partialReceivedDate;
   private long recievedDate;
   private long portalDate;
   private long portalUploadDate;
   private long investigatorDate;
   private long inprocessDate;
   private long ilabsRequestDate;
   private long irbDate;
   private long samplesReceivedDate;

   private boolean pipelinable;
   private boolean fastqRequested;
   private boolean analysisRequested;
   private Boolean highPriority;

   public RequestDetailed(){
     this("UNKNOWN");
   }

   public RequestDetailed( String request){
        samples = new ArrayList<>();
        requestId = request;
        investigator = "UNKNOWN";
   }

  public void setApplications(String applications){
	this.applications = applications;
}
   public void setBicReadme(String bicReadme){
	this.bicReadme = bicReadme;
  }
   public void setClinicalCorrelative(String clinicalCorrelative){
	this.clinicalCorrelative = clinicalCorrelative;
   }
   public void setCostCenter(String costCenter){
	this.costCenter = costCenter;
   }
   public void setFundNumber(String fundNumber){
	this.fundNumber = fundNumber;
   }
   public void setContactName(String contactName){
	this.contactName = contactName;
   }
   public void setDataAnalyst(String dataAnalyst){
	this.dataAnalyst = dataAnalyst;
   }
   public void setDataAnalystEmail(String dataAnalystEmail){
	this.dataAnalystEmail = dataAnalystEmail;
   }
   public void setDataDeliveryType(String dataDeliveryType){
	this.dataDeliveryType = dataDeliveryType;
   }
   public void setCmoContactName(String name){
        cmoContactName = name;
   }

   public void setCmoPiName(String name){
       cmoPiName = name;
   }
   public void setCmoPiEmail(String email){
       cmoPiEmail = email;
   }
   public void setCmoProjectId(String cmoProjectId){
	this.cmoProjectId = cmoProjectId;
   }
   public void setRequestId(String requestId){
	this.requestId = requestId;
   }
   public void setFaxNumber(String faxNumber){
	this.faxNumber = faxNumber;
   }
   public void setInvestigator(String investigator){
	this.investigator = investigator;
   }
   public void setIrbWaiverComments(String irbWaiverComments){
	this.irbWaiverComments = irbWaiverComments;
   }
   public void setMailTo(String mailTo){
      //if this isn't well formed, don't set
      if(mailTo.split("@").length == mailTo.split(",").length + 1){
           this.mailTo = mailTo;
      }
   }
   public void setPi(String pi){
	this.pi = pi;
   }
   public void setProjectName(String projectName){
    this.projectName = projectName;
   }

   public void setProjectNotes(String projectNotes){
	this.projectNotes = projectNotes;
   }
   public void setGroup(String group){
	this.group = group;
   }
   public void setGroupLeader(String groupLeader){
	this.groupLeader = groupLeader;
   }
   public void setInvestigatorEmail(String investigatorEmail){
	this.investigatorEmail = investigatorEmail;
   }
   public void setIrbId(String irbId){
	this.irbId = irbId;
   }
   public void setIrbVerifier(String irbVerifier){
	this.irbVerifier = irbVerifier;
   }
   public void setPiEmail(String piEmail){
	this.piEmail = piEmail;
   }
   public void setProjectManager(String projectManager){
	this.projectManager = projectManager;
   }
   public void setRequestDescription(String requestDescription){
	this.requestDescription = requestDescription;
   }
   public void setRequestDetails(String requestDetails){
	this.requestDetails = requestDetails;
   }
   public void setRoom(String room){
	this.room = room;
   }
   public void setRequestType(String requestType){
	this.requestType = requestType;
   }
   public void setSampleType(String sampleType){
	this.sampleType = sampleType;
   }
   public void setStatus(String status){
	this.status = status;
   }
   
  public void setFurthestSample(String furthest){
    this.furthest = furthest;
   }

   public void setTelephoneNum(String telephoneNum){
	this.telephoneNum = telephoneNum;
   }
   public void setTatFromProcessing(String tatFromProcessing){
	this.tatFromProcessing = tatFromProcessing;
   }
   public void setTatFromReceiving(String tatFromReceiving){
	this.tatFromReceiving = tatFromReceiving;
   }
   public void setServicesRequested(String servicesRequested){
	this.servicesRequested = servicesRequested;
   }
   public void setStudyId(String studyId){
	this.studyId = studyId;
   }
  public void setCommunicationNotes(String communicationNotes){
	this.communicationNotes = communicationNotes;
   }

   public void setCompletedDate(long completedDate){
	this.completedDate = completedDate;
   }
   public void setDeliveryDate(long deliveryDate){
	this.deliveryDate = deliveryDate;
   }
   public void setPartialReceivedDate(long partialReceivedDate){
	this.partialReceivedDate = partialReceivedDate;
   }
   public void setReceivedDate(long recievedDate){
	this.recievedDate = recievedDate;
   }
   public void setPortalDate(long portalDate){
	this.portalDate = portalDate;
   }
   public void setPortalUploadDate(long portalUploadDate){
	this.portalUploadDate = portalUploadDate;
   }
   public void setInvestigatorDate(long investigatorDate){
	this.investigatorDate = investigatorDate;
   }
   public void setInprocessDate(long inprocessDate){
	this.inprocessDate = inprocessDate;
   }
   public void setIlabsRequestDate(long ilabsRequestDate){
	this.ilabsRequestDate = ilabsRequestDate;
   }

   public void setIrbDate(long irbDate){
    this.irbDate = irbDate;
   }
   public void setSamplesReceivedDate(long samplesReceivedDate){
	this.samplesReceivedDate = samplesReceivedDate;
   }

   public void setAutorunnable(boolean pipelinable){
	this.pipelinable = pipelinable;
   }
   public void setFastqRequested(boolean fastqRequested){
	this.fastqRequested = fastqRequested;
   }
   public void setAnalysisRequested(boolean analysisRequested){
	this.analysisRequested = analysisRequested;
   }
   public void setHighPriority(boolean highPriority){
	this.highPriority = Boolean.valueOf(highPriority);
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
   public String getbicReadme(){
	return bicReadme;
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
   public String getDataAnalystEmail(){
	return dataAnalystEmail;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getDataDeliveryType(){
	return dataDeliveryType;
   }
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
   public String getFaxNumber(){
	return faxNumber;
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
   public String getMailTo(){
     return mailTo;
   }
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
   public String getIrbVerifier(){
	return irbVerifier;
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
   public long getPartialReceivedDate(){
	return partialReceivedDate;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public long getReceivedDate(){
	return recievedDate;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public long getPortalDate(){
	return portalDate;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public long getPortalUploadDate(){
	return portalUploadDate;
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
   public boolean getAutorunnable(){
	return pipelinable;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public boolean getFastqRequested(){
	return fastqRequested;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public boolean getAnalysisRequested(){
	return analysisRequested;
   }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Boolean getHighPriority(){
      return highPriority; 
    }
  public static RequestDetailed errorMessage(String e){
    RequestDetailed rd = new RequestDetailed("ERROR");
    rd.setInvestigator(e);
    return rd;
  }


}
