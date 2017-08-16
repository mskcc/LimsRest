
package org.mskcc.limsrest.limsapi;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.*;

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

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
 * A queued task that finds the barcode sequence for a give barcode id 
 * 
 * @author Aaron Gabow
 * 
 */
@Service
public class FindBarcodeSequence  extends LimsTask 
{
  
  private Log log = LogFactory.getLog(FindBarcodeSequence.class);
  private String barcodeId;

  public void init(String barcodeId){
     this.barcodeId = barcodeId;
  }
  
 //execute the velox call
@PreAuthorize("hasRole('READ')")
@Override
 public Object execute(VeloxConnection conn){
// private void runProgram(User apiUser, DataRecordManager dataRecordManager) {
  String seq = "";

  try { 
    List<DataRecord> indexList = dataRecordManager.queryDataRecords("IndexAssignment", "IndexId = '" + barcodeId + "'", user);
    for(DataRecord i: indexList){
      seq = i.getStringVal("IndexTag", user);
    }
  } catch (Throwable e) {
     StringWriter sw = new StringWriter();
     PrintWriter pw = new PrintWriter(sw);
     e.printStackTrace(pw);
     log.info( "ERROR IN SETTING REQUEST STATUS: " + e.getMessage() + "TRACE: " + sw.toString());
  

 }

  return seq; 
 }

}
