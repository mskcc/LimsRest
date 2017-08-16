package org.mskcc.limsrest.limsapi;

import java.util.ArrayList;
import com.fasterxml.jackson.annotation.*;

public class ProjectSummary {
   private ArrayList<RequestSummary> summaryRequests;
   private ArrayList<RequestDetailed> detailedRequests;
   private String cmoProjectId;
   private String cmoProposalTitle;
   private String cmoStudyType;
   private String cmoFinalProjectTitle;
   private String cmoProjectBrief;

   private String projectDesc;
   private String projectName;
   private String projectNotes;
   private String studyName;
   private String groupLeader;
   private String projectId;
   private String restStatus;

   private long cmoMeetingDiscussionDate;
   
   public ProjectSummary(){

     this("UNKNOWN");
   }

   public ProjectSummary( String project){
        summaryRequests = new ArrayList<>();
        detailedRequests = new ArrayList();
        projectId = project;
   }
   public void setRestStatus(String s){
         this.restStatus = s;
   }

   public ArrayList<RequestSummary> getSummaryRequests(){
        return summaryRequests;

   }

   public ArrayList<RequestDetailed> getDetailedRequests(){
       return detailedRequests;
   }

   public void addRequestSummary(RequestSummary rs){
        summaryRequests.add(rs);

   }

   public void addRequest(RequestDetailed rd){
     detailedRequests.add(rd);
   }

   public void setCmoProjectId(String cmoProjectId){
    this.cmoProjectId = cmoProjectId;
  }
   public void setCmoProposalTitle(String cmoProposalTitle){
    this.cmoProposalTitle = cmoProposalTitle;
  }
   public void setCmoStudyType(String cmoStudyType){
    this.cmoStudyType = cmoStudyType;
  }
   public void setCmoFinalProjectTitle(String cmoFinalProjectTitle){
    this.cmoFinalProjectTitle = cmoFinalProjectTitle;
  }
   public void setCmoProjectBrief(String cmoProjectBrief){
    this.cmoProjectBrief = cmoProjectBrief;
  }
   public void setProjectDesc(String projectDesc){
    this.projectDesc = projectDesc;
  }

   public void setProjectName(String projectName){
    this.projectName = projectName;
  }
   public void setProjectNotes(String projectNotes){

    this.projectNotes = projectNotes;
  }

  public void setStudyName(String studyName){
    this.studyName = studyName;
  }
   public void setGroupLeader(String groupLeader){
    this.groupLeader = groupLeader;
  }
   public void setProjectId(String projectId){
    this.projectId = projectId;
  }
   
   public void setCmoMeetingDiscussionDate(long cmoMeetingDiscussionDate){
    this.cmoMeetingDiscussionDate = cmoMeetingDiscussionDate;
  }  


   @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public String getCmoProjectId(){
    return cmoProjectId;
  }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public String getCmoProposalTitle(){
    return cmoProposalTitle;
  }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getCmoStudyType(){
     return cmoStudyType;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getCmoFinalProjectTitle(){
     return cmoFinalProjectTitle;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getCmoProjectBrief(){
     return cmoProjectBrief;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getProjectDesc(){
     return projectDesc;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getProjectName(){
     return projectName;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getProjectNotes(){
     return projectNotes;
   }

     @JsonInclude(JsonInclude.Include.NON_EMPTY)
     public String getStudyName(){
       return studyName;
     }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getGroupLeader(){
     return groupLeader;
   }
   
   public String getProjectId(){
     return projectId;
   }
   
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public long getCmoMeetingDiscussionDate(){
     return cmoMeetingDiscussionDate;
   }
   
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public String getRestStatus(){
    return this.restStatus;
  }

  public static ProjectSummary errorMessage(String e){
    ProjectSummary ps = new ProjectSummary("ERROR: " + e);
    
    ps.setRestStatus(e);
    return ps;
  }


}
