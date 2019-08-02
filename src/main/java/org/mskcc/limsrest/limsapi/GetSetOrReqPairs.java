package org.mskcc.limsrest.limsapi;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.io.PrintWriter;
import java.io.StringWriter;

//import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Service @Deprecated
public class GetSetOrReqPairs extends LimsTask
{
  private Log log = LogFactory.getLog(GetSetOrReqPairs.class);
  protected String request;
  protected String set;
  protected String mapName;
  protected boolean isMappingQuery;

  public void init(String project, String set, String mapName){
    this.request = project;
    this.set = set;
    this.mapName = mapName;
    this.isMappingQuery = true;
  }

  public void init(String requestId, String setName)
  {
    this.request = requestId;
    this.set = setName;
    this.isMappingQuery = false;
  }

  //@PreAuthorize("hasRole('READ')")
  public Object execute(VeloxConnection conn)
  {
    LinkedList<HashMap<String, String>> listMappings = new LinkedList<>();
    HashMap<String, String> status = new HashMap<>();
    try
    {
      List<DataRecord> parent =  null;
      if(request != null){
         parent = this.dataRecordManager.queryDataRecords("Request", "RequestId = '" + request + "'", this.user);

      } else if (set != null){
         parent = this.dataRecordManager.queryDataRecords("Set", "SetName = '" + set + "'", this.user);
      } else{
            status.put("STATUS", "ERROR: Either project or set must be specified");
            listMappings.addFirst(status);
            return listMappings;
      }
      if(parent.size() != 1){
         status.put("STATUS", "ERROR: Failed to find such a set or request. Please confirm name.");
         listMappings.addFirst(status);
         return listMappings;
      }
      if(isMappingQuery){
        DataRecord[] maps = parent.get(0).getChildrenOfType("CategoryMap", user);
        for(int i = 0; i < maps.length; i++){
           HashMap<String, String> categoryMap = new HashMap<>();
           categoryMap.put("SampleName", maps[i].getStringVal("OtherSampleId", user));
           categoryMap.put("Category", maps[i].getStringVal("Category", user));
           listMappings.add(categoryMap);
        }
      }
      else{
         DataRecord[] pairs = parent.get(0).getChildrenOfType("PairingInfo", user);
         for (int i = 0; i < pairs.length; i++) {
            String tumor = pairs[i].getStringVal("TumorName", user);
            String normal = pairs[i].getStringVal("NormalName", user);
            HashMap<String, String> pairMap = new HashMap<>();
            pairMap.put("Tumor", tumor);
            pairMap.put("Normal", normal);
            listMappings.add(pairMap);
         }
      }
    }
    catch (Throwable e) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      log.info(e.getMessage());
      log.info(sw.toString());
      status.put("STATUS", e.getMessage());
      listMappings.addFirst(status);
      return listMappings;
    }

    status.put("STATUS", "SUCCESS");
    return listMappings;
  }
}
