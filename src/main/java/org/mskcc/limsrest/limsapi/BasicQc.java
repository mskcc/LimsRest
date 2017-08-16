package org.mskcc.limsrest.limsapi;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import com.fasterxml.jackson.annotation.*;



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

  public BasicQc(){
        restStatus = "SUCCESS"; 
        statusEvents = new HashMap<>();
   }

   public void putStatusEvent(Long date, String newStatus){
        statusEvents.put(date, newStatus);
   }

   public void setAlias(String alias){
    this.alias = alias;
   }

   public void setCreateDate(long createDate){
        this.createDate = createDate;
   }

   public void setRun(String run){
        this.run = run;
   }

   public void setFileLocation(String fileLocation){
        this.fileLocation = fileLocation;
   }

   public void setSampleName(String sampleName){
        this.sampleName = sampleName;
   }


   public void setQcStatus(String qcStatus){
        this.qcStatus = qcStatus;
   }
 
   public void setRestStatus(String restStatus){
        this.restStatus = restStatus;
   }
   
   public void setTotalReads(Long reads){
        this.totalReads = reads;
   }


  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public String getAlias(){
    return this.alias;
  }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Long getCreateDate(){
       return this.createDate;
   }

 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getRun(){
        return this.run;
   }

 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getSampleName(){
        return this.sampleName;
   }


 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getQcStatus(){
        return this.qcStatus;
   }

  
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getRestStatus(){
        return this.restStatus;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getFileLocation(){
     return this.fileLocation;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public Long getTotalReads(){
    return this.totalReads;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public List<QcStatusEvent> getReviewedDates(){
    LinkedList<QcStatusEvent> reviewedDates = new LinkedList<>();
    for(Map.Entry<Long,String> eventPair :  statusEvents.entrySet()){
        reviewedDates.add(new QcStatusEvent(eventPair.getKey(), eventPair.getValue()));
    }
    Collections.sort(reviewedDates);
    return reviewedDates;
   }

}
