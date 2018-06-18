
package org.mskcc.limsrest.limsapi;


import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;



/**
 * A queued task that takes a request id and returns all some request information and some sample information 
 * 
 * @author Aaron Gabow
 * 
 */
@Service
public class SetRequestStatus  extends LimsTask 
{
  private Log log = LogFactory.getLog(SetRequestStatus.class);


 //execute the velox call
@PreAuthorize("hasRole('ADMIN')")
@Override
 public Object execute(VeloxConnection conn){
// private void runProgram(User apiUser, DataRecordManager dataRecordManager) {
  LinkedList<String> results = new LinkedList<String>(); 
  

  try { 
    //List<DataRecord> requestList = dataRecordManager.queryDataRecords("Request", "CompletedDate is null", user);
    //List<DataRecord> requestList = dataRecordManager.queryDataRecords("Request", "FurthestSample = ''", user);
    List<DataRecord> requestList = dataRecordManager.queryDataRecords("Request", "CompletedDate is null and FurthestSample not like '%Illumina Sequencing Analysis' and FurthestSample != 'Processing Completed'", user);

    for(DataRecord r: requestList){
      String status = "";
      String requestName = r.getStringVal("RequestId", user);
      StringBuilder breakdown = new StringBuilder();


      HashSet<DataRecord> visitedSamples = new HashSet<>();
      LinkedList<DataRecord> fringe = new LinkedList<>();
      //BFS to find that samples status.
      fringe.addAll(Arrays.asList(r.getChildrenOfType("Sample", user)));
      DataRecord current = null;
      HashMap<String, Integer> status2Count = new HashMap<>();
      double total = 0.0;
      while(fringe.size() > 0){
          current = fringe.poll();
          if( visitedSamples.contains(current)){
               continue;
          }
          try{
            status = current.getStringVal("ExemplarSampleStatus", user);
            if(!status2Count.containsKey(status)){
                status2Count.put(status, 0);
            }

          } catch(Exception e){
             continue;
          }
          DataRecord[] childrenSamples = current.getChildrenOfType("Sample", user);
          if(childrenSamples.length == 0){
            status2Count.put(status, status2Count.get(status) + 1);
            total += 1.0;
          }
          else if(childrenSamples.length == 1){
                DataRecord child = childrenSamples[0];
                String childReq = "";
                status = "";
                try{ childReq = child.getStringVal("RequestId", user); } catch(Exception e){}
                try{ status = current.getStringVal("ExemplarSampleStatus", user); } catch(Exception e){}
                    if(!status2Count.containsKey(status)){
                       status2Count.put(status, 0);
                    }
                    if((!childReq.equals("") && !childReq.equals(requestName))){
                        if(!status2Count.containsKey("Moved on to request " + childReq)){
                            status2Count.put("Moved on to request " + childReq, 0);
                        }
                        status2Count.put("Moved on to request " + childReq, status2Count.get("Moved on to request " + childReq) + 1);
                        total += 1.0;
                    }
                    else{
                        fringe.addFirst(child);
                    }

          }
          else{
                //in the case of multiple aliquots from a single sample, we want to get the least furthest along that doesn't fail. This is a quick and dirty approximation
                DataRecord leastProcessedChild = childrenSamples[0];
                int min = Integer.MAX_VALUE;
                status = "";
                for(DataRecord child : childrenSamples){
                    try{ status = child.getStringVal("ExemplarSampleStatus", user); } catch(Exception e){}
                    if(status.startsWith("Failed") || status.startsWith("Turned Off")){
                         continue;
                    }
                    List<DataRecord> childDesc = child.getDescendantsOfType("Sample", user);
                    if(childDesc.size() < min){
                        min = childDesc.size();
                        leastProcessedChild = child;

                    }
                }
                String childReq = "";
                status = "";
                try{ childReq = leastProcessedChild.getStringVal("RequestId", user); } catch(Exception e){}
                try{ status = leastProcessedChild.getStringVal("ExemplarSampleStatus", user); } catch(Exception e){}
                if(!status2Count.containsKey(status)){
                    status2Count.put(status, 0);
                } 
                if((!childReq.equals("") && !childReq.equals(requestName))){
                     if(!status2Count.containsKey("Moved on to request " + childReq)){
                          status2Count.put("Moved on to request " + childReq, 0);
                     }
                     status2Count.put("Moved on to request " + childReq, status2Count.get("Moved on to request " + childReq) + 1);
                } else{
                    fringe.addFirst(leastProcessedChild);

                }
          }
          visitedSamples.add(current);

      }
     for(Map.Entry<String, Integer> entry : status2Count.entrySet()){
        if(entry.getValue() != 0){
            breakdown.append(entry.getKey());
            breakdown.append(": ");
            breakdown.append( String.format( "%.2f", entry.getValue()/total));
            breakdown.append(", ");
        }

     }
     if(breakdown.length() > 2){
        breakdown.setLength(breakdown.length() - 2);
     }
     r.setDataField("FurthestSample", breakdown.toString(), user);
     dataRecordManager.storeAndCommit("Status updated by bicapi using SetRequestStatus", user);
     results.add(requestName + ":" + breakdown.toString());
    }
  } catch (Throwable e) {
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
         results.add("ERROR IN SETTING REQUEST STATUS: " + e.getMessage() + "TRACE: " + sw.toString());   
         return results;  
  }

  return results;
 }

}
