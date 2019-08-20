package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.mskcc.limsrest.util.Messages;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A queued task that takes a request id and map of values and sets them 
 * 
 * @author Aaron Gabow
 */
@Service
public class SetRequest extends LimsTask {
  String igoUser;
  String requestId;
  HashMap<String, Object> possibleRequestFields;

  public void init(String igoUser, String requestId, HashMap<String, Object> requestFields){
    this.igoUser = igoUser;
    this.requestId = requestId; 
    this.possibleRequestFields = requestFields;
  }
@PreAuthorize("hasRole('ADMIN')")
@Override
 public Object execute(VeloxConnection conn){
  try {
    if(requestId == null || requestId.equals("")){
       throw new LimsException("Must have a request id to set the request");
    }
    List<DataRecord> matchedRequest = dataRecordManager.queryDataRecords("Request", "RequestId = '" + requestId +  "'", user);
    if(matchedRequest.size() == 0){
       throw new Exception("No Request record in the lims matches " + requestId);
    }

    String[] allowedFields = {"ReadMe"};
    Map<String, Object> requestFields = new HashMap<>();
    for(String allowed : allowedFields){
       if(possibleRequestFields.containsKey(allowed) && !possibleRequestFields.get(allowed).equals("NULL")){
           requestFields.put(allowed, possibleRequestFields.get(allowed));
           if(allowed.equals("ReadMe")){
                StringBuilder currentReadMe = new StringBuilder();
                try{currentReadMe.append(matchedRequest.get(0).getStringVal("ReadMe", user)); } catch(NullPointerException npe){}
                if(currentReadMe.length() > 0){
                     currentReadMe.append("\n---------\n");
                }
                currentReadMe.append(possibleRequestFields.get(allowed));
                requestFields.put("ReadMe", currentReadMe.toString() );
           }
       }
    }
    matchedRequest.get(0).setFields(requestFields, user);
    Long.toString(matchedRequest.get(0).getRecordId());
    dataRecordManager.storeAndCommit("Request " + requestId  + " updated through the web service", user);

  } catch (Throwable e) {
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
           return Messages.ERROR_IN +  " SETTING REQUEST: " + e.getMessage() + "TRACE: " + sw.toString();   
  
  }
  return Messages.SUCCESS; 
 }
}