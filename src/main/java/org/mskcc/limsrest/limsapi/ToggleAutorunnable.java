
package org.mskcc.limsrest.limsapi;


import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;


/**
 * A queued task that takes a request id and a true/false status and sets the BicAutorunnable field 
 * 
 * @author Aaron Gabow
 * 
 */
@Service
public class  ToggleAutorunnable  extends LimsTask 
{
  String requestId;
  String status;
  String comment;
  String igoUser;

  public void init(String requestId, String status, String comment, String igoUser){
   this.requestId = requestId;
   this.status = status.toLowerCase();
   this.comment = comment;
   this.igoUser = igoUser;
}

 //execute the velox call
@PreAuthorize("hasRole('ADMIN')")
@Override
 public Object execute(VeloxConnection conn){
// private void runProgram(User apiUser, DataRecordManager dataRecordManager) {
  LinkedList<String> statuses = new LinkedList<>();  

  try { 
    if(!status.equals("true") && !status.equals("false")){
       return "ERROR IN SETTING AUTORUNNABLE STATUS: Must be a value of true or false";
    }
    List<DataRecord> requests = null;
    if(requestId.equals("ALL")){
      requests = dataRecordManager.queryDataRecords("Request", "BicAutorunnable is null", user);
    }  else {
      requests = dataRecordManager.queryDataRecords("Request", "RequestId = '" + requestId + "'", user);
    }
    for(DataRecord request : requests){
       if(status.equals("true")){
         request.setDataField("BicAutorunnable", Boolean.TRUE, user);
       }
       else if(status.equals("false")){
         request.setDataField("BicAutorunnable", Boolean.FALSE, user);
       }
       if(!requestId.equals("ALL") && comment != null){
         String currentReadMe = comment;
         try{currentReadMe = request.getStringVal("ReadMe", user); } catch(NullPointerException npe){}
         if(!currentReadMe.equals(comment)){
            currentReadMe = currentReadMe + "\n---------\n" + comment;
         }
         request.setDataField("ReadMe", currentReadMe, user);
          
       }
       statuses.add(requestId + ":" + status + ":" + comment);
     }
    String extraInfo = "";
    if(igoUser != null){
       extraInfo = "User: " + igoUser + " ";
    }
    dataRecordManager.storeAndCommit(extraInfo +  "BicAutorunnable updated to " + status + " for request " + requestId , user);
  } catch (Throwable e) {
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
         String results = "ERROR IN SETTING AUTORUN STATUS: " + e.getMessage() + "TRACE: " + sw.toString();   
          statuses.add(results);  
  }

  return statuses;
 }

}
