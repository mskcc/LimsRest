
package org.mskcc.limsrest.limsapi;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


/**
 * A queued task that finds the barcode sequence for a give barcode id 
 * 
 * @author Aaron Gabow
 * 
 */
@Service
public class GetBarcodeInfo  extends LimsTask 
{
  
  private Log log = LogFactory.getLog(GetBarcodeInfo.class); 
  private HashMap<String, String> type2Short = new HashMap<String, String>();
 //execute the velox call
@PreAuthorize("hasRole('READ')")
@Override
 public Object execute(VeloxConnection conn){
// private void runProgram(User apiUser, DataRecordManager dataRecordManager) {
  List<BarcodeSummary> bcodes = new LinkedList<>();  
  
  try { 

    List<DataRecord> indexList = dataRecordManager.queryDataRecords("IndexAssignment", "IndexType != 'IDT_TRIM' ORDER BY IndexType, IndexId", user);
    for(DataRecord i: indexList){
      try{ bcodes.add(new BarcodeSummary(i.getStringVal("IndexType", user), i.getStringVal("IndexId", user), i.getStringVal("IndexTag", user))); } catch(NullPointerException npe){}
    }
  } catch (Throwable e) {
     StringWriter sw = new StringWriter();
     PrintWriter pw = new PrintWriter(sw);
     e.printStackTrace(pw);
     log.info( "ERROR IN SETTING REQUEST STATUS: " + e.getMessage() + "TRACE: " + sw.toString());
  

 }

  return bcodes;
 }

}
