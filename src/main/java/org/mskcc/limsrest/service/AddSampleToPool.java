package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.mskcc.limsrest.ConnectionLIMS;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

/**
 * A queued task that takes a sample id and a flowcellid, lane pair and if no sample has a barcode conflict with the new lane, reassigns the pool to that lane
 * 
 * @author Aaron Gabow
 */
public class AddSampleToPool {
  String pool; 
  String sampleId;
  String removePool;
  String igoUser;
  private ConnectionLIMS conn;

  public AddSampleToPool(String pool, String sampleId, String removePool, String igoUser, ConnectionLIMS conn){
    this.pool = pool;
    this.sampleId = sampleId;
    this.removePool = removePool;
    this.igoUser = igoUser;
    this.conn = conn;
  }
@PreAuthorize("hasRole('ADMIN')")
 public String execute(){
    VeloxConnection vConn = conn.getConnection();
    User user = vConn.getUser();
    DataRecordManager drm = vConn.getDataRecordManager();
  try { 
    List<DataRecord> samples = drm.queryDataRecords("Sample", "SampleId = '" + sampleId + "'", user);
    String otherId = samples.get(0).getStringVal("OtherSampleId", user);
    String userId = samples.get(0).getStringVal("UserSampleID", user);

    if(!pool.equals("NULL")){
        List<DataRecord> newPools = drm.queryDataRecords("Sample", "SampleId = '" + pool + "'", user);
        String newPoolOtherId = newPools.get(0).getStringVal("OtherSampleId", user);
        String newPoolUserId = newPools.get(0).getStringVal("UserSampleID", user);    
    
        if(newPoolOtherId.endsWith(",")){
          newPoolOtherId = newPoolOtherId;
        }
        if(newPoolUserId.endsWith(",")){
          newPoolUserId = newPoolUserId;
        }
       newPools.get(0).setDataField("OtherSampleId", otherId + "," + newPoolOtherId, user);
       newPools.get(0).setDataField("UserSampleID", userId + "," + newPoolUserId, user);
       samples.get(0).addChild(newPools.get(0), user);
 
    }
    if(!removePool.equals("NULL")){
        List<DataRecord> oldPools = drm.queryDataRecords("Sample", "SampleId = '" + removePool + "'", user);
        String oldPoolOtherId = oldPools.get(0).getStringVal("OtherSampleId", user);
        String oldPoolUserId = oldPools.get(0).getStringVal("UserSampleID", user);
        oldPoolOtherId.replace(otherId + ",", "");
        oldPoolOtherId.replace(otherId, "");
        oldPoolUserId.replace(userId + ",", "");
        oldPoolUserId.replace(userId, "");
        oldPools.get(0).setDataField("OtherSampleId", oldPoolOtherId, user);
        oldPools.get(0).setDataField("UserSampleID", oldPoolUserId, user);
        LinkedList<List<DataRecord>> listOfOldPools = new LinkedList<>();
        listOfOldPools.add(oldPools);
        drm.removeChildren(samples, listOfOldPools, user);
   }

    if(!removePool.equals("NULL") && !pool.equals("NULL")){
        List<DataRecord> nimblegens = drm.queryDataRecords("NimbleGenHybProtocol", "SampleId = '" + sampleId + "' AND Protocol2Sample = '" + removePool + "'" , user);
        for(DataRecord nimblegen : nimblegens){
            nimblegen.setDataField("Protocol2Sample", pool, user);
         }
    }

      drm.storeAndCommit("Fixing", user);
  } catch (Throwable e) {
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
           return "ERROR IN ADDING POOL TO LANE: " + e.getMessage() + "TRACE: " + sw.toString();
  }

  return "Success";
 }
}