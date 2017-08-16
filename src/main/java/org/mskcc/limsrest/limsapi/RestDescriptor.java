package org.mskcc.limsrest.limsapi;

import com.fasterxml.jackson.annotation.*;

public class RestDescriptor{

   String detectedError;

   public RestDescriptor(){}

   public void setDetectedError(String message){
      this.detectedError = message;
   }

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getDetectError(){
      return detectedError;
   }


}

