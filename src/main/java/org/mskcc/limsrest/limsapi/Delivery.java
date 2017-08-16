package org.mskcc.limsrest.limsapi;

import java.util.List;
import java.util.LinkedList;
import com.fasterxml.jackson.annotation.*;

public class Delivery extends RestDescriptor{
   private String sampleId; 
   private String requestId;

   public Delivery(String requestId, String sampleId){
        this.requestId = requestId;
        this.sampleId = sampleId; 
   }




  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public String getRequestId(){
    return requestId;
  }
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public String getSampleId(){
    return sampleId;
  }
}
