package org.mskcc.limsrest.limsapi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;

import com.velox.api.datamgmtserver.DataMgmtServer;
import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sapioutils.client.standalone.VeloxStandalone;
import com.velox.sapioutils.client.standalone.VeloxStandaloneException;
import com.velox.sapioutils.client.standalone.VeloxStandaloneManagerContext;
import com.velox.sapioutils.client.standalone.VeloxTask;

/**
 * Find all samples associated with a project/request 
 *     */
public class GetProjectDetails extends LimsTask {
  private Log log = LogFactory.getLog(GetProjectDetails.class);

  protected String project;


  public void init(String project){
    this.project = project;
  }

 //execute the velox call
 @Override
 public Object execute(VeloxConnection conn){
// private void runProgram(User apiUser, DataRecordManager dataRecordManager) {
   ProjectSummary ps = new ProjectSummary();
   RequestDetailed rd = new RequestDetailed(project);

   try {
     List<DataRecord> limsRequestList = dataRecordManager.queryDataRecords("Request", "RequestId = '" + project  +"'", user);
     for(DataRecord r: limsRequestList){
        annotateRequestDetailed(rd, r);
        List<DataRecord> parents = r.getParentsOfType("Project", user);
        if(parents.size() > 0){
           annotateProjectSummary(ps, parents.get(0)) ;
        }
        ps.addRequest(rd);
     }
        
   } catch (Throwable e) {
       log.info(e.getMessage());
        rd = RequestDetailed.errorMessage(e.getMessage());
        ps.addRequest(rd);
   }
 
    return ps;
  }
}
