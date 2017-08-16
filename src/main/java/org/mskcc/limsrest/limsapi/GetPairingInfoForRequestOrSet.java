package org.mskcc.limsrest.limsapi;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.*;


import com.velox.api.datamgmtserver.DataMgmtServer;
import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sapioutils.client.standalone.VeloxStandalone;
import com.velox.sapioutils.client.standalone.VeloxStandaloneException;
import com.velox.sapioutils.client.standalone.VeloxStandaloneManagerContext;
import com.velox.sapioutils.client.standalone.VeloxExecutable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * A queued task that takes either a project name or a set id and returns any pairing information associated with it 
 * 
 * @author Aaron Gabow
 * 
 */
public class GetPairingInfoForRequestOrSet extends LimsTask 
{

  private Log log = LogFactory.getLog(GetPairingInfoForRequestOrSet.class);

  protected String project;
  protected String setId;
  protected String mapName;
  protected boolean isMappingQuery;

  public void init(String project, String setId){
    this.project = project;
    this.setId = setId;
    this.isMappingQuery = false;
  }

  public void init(String project, String setId, String mapName){
    this.project = project;
    this.setId = setId;
    this.mapName = mapName;
    this.isMappingQuery = true;
  }

 //execute the velox call
 @Override
 public Object execute(VeloxConnection conn){
// private void runProgram(User apiUser, DataRecordManager dataRecordManager) {
  LinkedList<PairSummary> pairSummaries = new LinkedList<>();
  if(project == null && setId == null){
    return pairSummaries; 
  }
  try{
     List<DataRecord> parentList;
     if(project != null){
         parentList = dataRecordManager.queryDataRecords("Request", "RequestId = '" + project  +"'", user);
     } else{
        parentList = dataRecordManager.queryDataRecords("SampleSet", "Name = '" + setId  +"'", user); 
     }
     for(DataRecord r: parentList){
       if(isMappingQuery){
         DataRecord[] maps = r.getChildrenOfType("CategoryMap", user);
         for(int i = 0; i < maps.length; i++){ 
           PairSummary ps = new PairSummary();
           ps.setSampleName(maps[i].getStringVal("OtherSampleId", user));
           ps.setCategory(maps[i].getStringVal("Category", user));
           pairSummaries.add(ps);
         }
       } 
       else{
         DataRecord[] pairs = r.getChildrenOfType("PairingInfo", user);
         for(int i = 0; i < pairs.length; i++){
           PairSummary ps = new PairSummary();
           ps.setTumor(pairs[i].getStringVal("TumorId", user));
           ps.setNormal(pairs[i].getStringVal("NormalId", user));
           pairSummaries.add(ps);
         }
       }
    }
  } catch (Throwable e) {
   log.info(e.getMessage());
  }

  return pairSummaries;
 }

}
