
package org.mskcc.limsrest.limsapi;


import com.velox.api.datarecord.AlreadyExists;
import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


/**
 * A queued task that takes a sample id and a flowcellid, lane pair and if no sample has a barcode conflict with the new lane, reassigns the pool to that lane
 * 
 * @author Aaron Gabow
 * 
 */
@Service
public class AddPoolToLane  extends LimsTask 
{
  String flowcell; 
  String sampleId;
  String removeSampleId;
  String igoUser;
  Long laneId;
  boolean force;


  public void init(String flowcell, String sampleId, String removeSampleId, String igoUser, Long lane, boolean force){ 
    this.flowcell = flowcell;
    this.sampleId = sampleId;
    this.removeSampleId = removeSampleId;
    this.laneId = lane;
    this.igoUser = igoUser; 
    this.force = force;
  }
 //execute the velox call
@PreAuthorize("hasRole('ADMIN')")
@Override
 public Object execute(VeloxConnection conn){
  try { 
     List<DataRecord> lanes = dataRecordManager.queryDataRecords("FlowCellLane", "RelatedRecord134 = '" + flowcell +  "' and LaneNum = '" + laneId + "'", user);
    
    if(lanes.size() != 1){
       return "ERROR: This service must match exactly one flowcell, lane pair";
    } 
    DataRecord lane = lanes.get(0);
    
    List<DataRecord> removeList = dataRecordManager.queryDataRecords("Sample", "SampleId = '" + removeSampleId + "'", user);
    if(removeList.size() != 1 && !removeSampleId.equals( "NULL")){
        return "ERROR: This service must match exactly one remove sample";
    } 
          
    LinkedList<List<DataRecord>> listOfLane = new LinkedList<>();
    listOfLane.add(lanes);
    if(!removeSampleId.equals( "NULL")){
        dataRecordManager.removeChildren(removeList, listOfLane, user);
    }

    List<DataRecord> sampleList = dataRecordManager.queryDataRecords("Sample", "SampleId = '" + sampleId + "'", user);
    if(sampleList.size() != 1){
        return "ERROR: This service must match exactly one sample";
    }
    HashMap<String, DataRecord> barcode2Sample = new HashMap<>();

    List<DataRecord> ancestorSamples = sampleList.get(0).getAncestorsOfType("Sample", user);
    for(DataRecord sample : ancestorSamples){
      DataRecord[] barcodes = sample.getChildrenOfType("IndexBarcode", user);
      if(barcodes.length != 0){
         try{ barcode2Sample.put(barcodes[0].getStringVal("IndexTag", user), sample); } catch(NullPointerException npe){} 
      }
    }

    List<DataRecord> ancestorLaneSamples = lane.getAncestorsOfType("Sample", user);
    for(DataRecord sample : ancestorLaneSamples){
       DataRecord[] barcodes = sample.getChildrenOfType("IndexBarcode", user);
       if(barcodes.length != 0 && !force){
         String barcodeValue  = barcodes[0].getStringVal("IndexTag", user);
         try{
             if(barcode2Sample.containsKey(barcodeValue)){
                DataRecord poolSample = barcode2Sample.get(barcodeValue);
                //if(!sample.getStringVal("SampleId", user).equals(poolSample.getStringVal("SampleId", user))){
                //   return "ERROR: Sample " + sample.getStringVal("SampleId", user) + " has the sample barcode as Sample " + poolSample.getStringVal("SampleId", user) + " cannot assign the pool to new lane";
               // }
             }
          } catch(NullPointerException npe){}
        }
    }
    try{
       sampleList.get(0).addChild(lane, user);
    } catch(AlreadyExists ae){
      return "ERROR: The pool already is in the lane";
    }


    dataRecordManager.storeAndCommit(igoUser  + " put pool " + sampleId + " in lane " + laneId, user);


  } catch (Throwable e) {
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
           return "ERROR IN ADDING POOL TO LANE: " + e.getMessage() + "TRACE: " + sw.toString();   
  
  }

  return "Pool "+ sampleId + " in lane " + laneId; 
 }

}
