package org.mskcc.limsrest.limsapi;

import java.lang.StringBuffer;
import java.util.Calendar;
import java.util.GregorianCalendar;
import com.fasterxml.jackson.annotation.*;


public class BarcodeSummary extends RestDescriptor{
   private String name;
//   private long cardinality;
   private String id;
   private String tag;

public BarcodeSummary(String name, long count){
   this.name = name;
//   this.cardinality = count;
}


 public BarcodeSummary(String name, String id, String tag){
   this.name = name;
   this.id = id;
   this.tag = tag;
}
   
   
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public String getName(){
     return name;
  }

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public String getBarcodId(){
     return id;
  }

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public String getBarcodeTag(){
     return tag;
  }
/*
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public long getCardinality(){
     return cardinality;
  }
*/
  
}
