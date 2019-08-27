package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A queued task that takes a sample name, sees if it has reached the sequencer and then renames the sample and any child records that reference the old name,  
 * including fixing pools with the name within the concatenated pool
 *
 * @author Aaron Gabow
 */
public class RenameSample extends LimsTask {
    private static Log log = LogFactory.getLog(RenameSample.class);
    String oldSampleId;
    String igoId;
    String newSampleId;
    String newUserId;
    String requestId;
    String igoUser;
    int changeCount;


    public void init(String igoUser, String request, String igoId, String newSampleId, String newUserId) {
        this.igoUser = igoUser;
        this.igoId = igoId;
        this.newSampleId = newSampleId;
        this.newUserId = newUserId;
        this.requestId = request;
        this.changeCount = 0;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public Object execute(VeloxConnection conn) {
  //first make sure that there is a sample with the old name in the request
  //then make sure that there isn't a sample with the new name already in this request or any requests that children samples belong to
  try { 
    List<DataRecord> requestSamples = new LinkedList<>();
     DataRecord root = null;
    if(igoId != null){
         List<DataRecord> sampleList = dataRecordManager.queryDataRecords("Sample",  "SampleId = '" + igoId + "'", user);
         root = sampleList.get(0);
         try{
           oldSampleId = root.getStringVal("OtherSampleId", user);
         } catch(NullPointerException npe){
           oldSampleId = "";
         }
         List<DataRecord> requestList = root.getAncestorsOfType("Request", user);
         for(DataRecord r : requestList){
            DataRecord[] plates = r.getChildrenOfType("Plate", user);
            if(plates.length != 0){
                for(int i = 0; i < plates.length; i++){
                    DataRecord[] plateSamples = plates[i].getChildrenOfType("Sample", user);
                    for(int j = 0; j < plateSamples.length; j++){
                        try{
                            if(!igoId.equals(plateSamples[i].getStringVal("SampleId", user))){
                                requestSamples.add(plateSamples[i]);
                            }
                        } catch(NullPointerException npe){}
        
                    }
                }
            }
            DataRecord[] samps = r.getChildrenOfType("Sample", user);
            for(int i = 0; i< samps.length; i++){
                try{
                    if(!igoId.equals(samps[i].getStringVal("SampleId", user))){
                        requestSamples.add(samps[i]);
                    }
                }catch(NullPointerException npe){}
            }
            
        }
    } else{
        return "No longer supported. Must use the Igo Id for the sample";
    }
    
    if(root == null){
        return "ERROR: No sample in request " + requestId + " matches the sample name " + oldSampleId;
    }
    List<DataRecord> parentSamples = root.getAncestorsOfType("Sample", user);
  /*  if(parentSamples.size() > 0){
        return "ERROR: The requested sample must not have any ancestor Samples";
    }
    */
  /*  List<DataRecord> flowlanes = root.getDescendantsOfType("FlowCellLane", user);
    if(flowlanes.size() > 0){
        return "ERROR: The sample " + oldSampleId + " has already been sequenced. Please contact the sequence analyst about what to do now";
    }
*/
    //ideally would look through child requests too
    /*
    for(DataRecord s: requestSamples){ 
         try{
             if(newSampleId.equals(s.getStringVal("OtherSampleId", user))){
                 return "ERROR: A sample already has that name";
             } 
          } catch(NullPointerException npe){}
     }
     */
     //go through each descendant and change the other sample id, taking care to handle when pooling has concatonated the name with other samples
     HashSet<DataRecord> descendants = new HashSet<>();
     LinkedList<DataRecord> queue = new LinkedList<>();
     queue.add(root);
     while(!queue.isEmpty()){
        DataRecord c = queue.pop();
        DataRecord[] searchChildren = c.getChildren(user);
        for(int i = 0; i < searchChildren.length; i++){
          if(!descendants.contains(searchChildren[i])){
             queue.add(searchChildren[i]);
             if(!searchChildren[i].getDataTypeName().equals("SampleCMOInfoRecords")){
                descendants.add(searchChildren[i]);
             }
          } 
        }
    }
    descendants.add(root);
     for(DataRecord d: descendants){
        try{
          Map<String, Object> fields = d.getFields(user);
          if(fields.containsKey("OtherSampleId")){
               changeCount += 1;
               String possiblePooledName = ((String)fields.get("OtherSampleId"));
               String newName = possiblePooledName.replaceFirst(oldSampleId, newSampleId);
               d.setDataField("OtherSampleId", newName, user);
          }
          if(fields.containsKey("UserSampleID")){
                String possiblePooledName = ((String)fields.get("UserSampleID"));
                String newName = possiblePooledName;
                if(newUserId != null){
                    newName = possiblePooledName.replaceFirst(oldSampleId, newSampleId);
                } else{
                    newName = possiblePooledName.replaceFirst(oldSampleId, newUserId);
                }
                d.setDataField("UserSampleID", newName, user);
          }
           
        } catch(NullPointerException  npe){}
     }

     //need to fix any records that reference that aren't descendants that reference the sample id. We probably don't want to change pairing.
    dataRecordManager.storeAndCommit( "Change the previous sample name to " + newSampleId + " in request " + requestId + " by user " + igoUser, user);
  } catch (Throwable e) {
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
          log.info(e.getMessage() + " TRACE: " + sw.toString());
          return "ERROR IN RENAMING SAMPLE SAMPLE: " + e.getMessage() ;
  }

  return "SUCCESS: Changed " + changeCount + " records.";
 }
}