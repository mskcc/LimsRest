package org.mskcc.limsrest.limsapi;

import java.util.ArrayList;
import java.util.LinkedList;

import com.fasterxml.jackson.annotation.*;

public class PairSummary {
  private String tumor;
  private String normal;
  private String sampleName;
  private String category;

   public void setTumor(String tumor){
        this.tumor = tumor;

   }

   public void setNormal(String normal){ 
        this.normal = normal;
   }
   
   public void setSampleName(String sampleName){
      this.sampleName = sampleName;
   }

   public void setCategory(String category){
       this.category = category;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getSampleName(){
       return sampleName;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getCategory(){
      return category;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getTumor(){
      return tumor;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getNormal(){
     return normal;
   }

}
