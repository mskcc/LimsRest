package org.mskcc.limsrest.limsapi;

import java.lang.StringBuffer;
import java.util.Calendar;
import java.util.GregorianCalendar;
import com.fasterxml.jackson.annotation.*;


public class HistoricalEvent extends RestDescriptor  implements Comparable{
   public GregorianCalendar start;
   public GregorianCalendar end;
   String taskName;
   String requestId;

public HistoricalEvent(long startInMS, long endInMS, String requestId, String taskName){
   this.start = new GregorianCalendar();
   this.end = new GregorianCalendar();
   start.setTimeInMillis(startInMS);
   end.setTimeInMillis(endInMS);
   this.requestId = requestId;
   this.taskName = taskName;

}
  public int compCal(Calendar c1, Calendar c2){
    if(c1 == c2) return 0;
    if(c1.get(Calendar.YEAR) > c2.get(Calendar.YEAR)){
       return 1;
    }
    else if(c1.get(Calendar.YEAR) < c2.get(Calendar.YEAR)){
       return -1;
    }  else{
      if(c1.get(Calendar.MONTH) > c2.get(Calendar.MONTH)){
         return 1;
      }
      else if(c1.get(Calendar.MONTH) < c2.get(Calendar.MONTH)){
        return -1;
      }  else{
        if(c1.get(Calendar.DAY_OF_MONTH) > c2.get(Calendar.DAY_OF_MONTH)){
             return 1;
        }
        else if(c1.get(Calendar.DAY_OF_MONTH) < c2.get(Calendar.DAY_OF_MONTH)){
            return -1;
        }
        else{
           return 0;
        }
      }
    }

  }

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public String getRequestId(){
     return requestId;
  }

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public String getStartDate(){
    StringBuffer sb = new StringBuffer();
    sb.append(start.get(Calendar.YEAR));
    sb.append("-");
    sb.append(start.get(Calendar.MONTH) + 1 );
    sb.append("-");
    sb.append(start.get(Calendar.DAY_OF_MONTH));
    return sb.toString();
  }

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public String getEndDate(){
    if(end.get(Calendar.YEAR) < 1980){
      return "";
    }
    StringBuffer sb = new StringBuffer();
    sb.append(end.get(Calendar.YEAR));
    sb.append("-");
    sb.append(end.get(Calendar.MONTH) + 1);
    sb.append("-");
    sb.append(end.get(Calendar.DAY_OF_MONTH));
    return sb.toString();
  }

  @JsonIgnore
  public GregorianCalendar getStart(){
     return start;
  }
  @JsonIgnore
  public GregorianCalendar getEnd(){
    return end;
  }

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public String getTaskName(){
     return taskName;
  }

  @Override
  public int compareTo(Object o){
    if(this == o) return 0;
    final HistoricalEvent h = (HistoricalEvent) o;

    if(compCal(start, h.getStart()) == 1){
      return 1;
    }  
    else if(compCal(start, h.getStart()) == -1 ){
      return -1;
    }
    else if(compCal(end, h.getEnd()) == 1 ){
      return 1;
    }
    else if(compCal(end, h.getEnd()) == -1){
       return -1;
    }
    else{
       return taskName.compareTo(h.getTaskName());
    }
   
  }

  @Override
  public boolean equals(Object o){ 
    if(this == o) return true;
    if(o == null) return false;
    if(this.getClass() != o.getClass()) return false;
    return equals((HistoricalEvent) o);
  }
   
  public boolean equals(HistoricalEvent h){
    return (compCal(start, h.getStart()) == 0 && compCal(end, h.getEnd()) == 0 && taskName.equals(h.getTaskName()));
  }
/*
  public static void main(String args[]){
    HistoricalEvent h1 = new HistoricalEvent(1421435161000L, 1452971162000L, "by year");
    System.out.println(h1.compCal(h1.start, h1.end));
    HistoricalEvent h2 = new HistoricalEvent(1452971162000L, 1455649561000L, "by month");
    System.out.println(h2.compCal(h2.start, h2.end));
    HistoricalEvent h3 = new HistoricalEvent(1452452761000L, 1452971162000L,  "by day");
    System.out.println(h3.compCal(h3.start, h3.end));
     HistoricalEvent h4 = new HistoricalEvent(1452971162000L, 1421435161000L, "by year");
    System.out.println(h4.compCal(h4.start, h4.end));
    HistoricalEvent h5 = new HistoricalEvent(1455649561000L, 1452971162000L,  "by month");
    System.out.println(h5.compCal(h5.start, h5.end));
    HistoricalEvent h6 = new HistoricalEvent(1452971162000L, 1452452761000L,  "by day");
    System.out.println(h6.compCal(h6.start, h6.end)); 
    
    HistoricalEvent h7 = new HistoricalEvent(1421435167000L, 1452971169000L, "by year");
    System.out.println(h1.equals(h7));
    TreeSet<HistoricalEvent> hs = new TreeSet<>();
    System.out.println(hs.add(h1));
    System.out.println(hs.add(h7));
}
*/   
  
}
