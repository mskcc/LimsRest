package org.mskcc.limsrest.limsapi;


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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Find all studies/projects and list them 
 *     */
public class ListStudies extends LimsTask {
  private Log log = LogFactory.getLog(ListStudies.class);
  boolean cmoOnly =true; 

  public void init(String cmoOnly){
    if("false".equals(cmoOnly.toLowerCase())){
        this.cmoOnly = false; 
    }
  }

 //execute the velox call
 @Override
 public Object execute(VeloxConnection conn){
// private void runProgram(User apiUser, DataRecordManager dataRecordManager) {
   LinkedList<ProjectSummary> allProjects = new LinkedList<>();

   try {
     List<DataRecord> limsProjectList = dataRecordManager.queryDataRecords("Project", null, user);
     for(DataRecord p: limsProjectList){
        ProjectSummary ps = new ProjectSummary(p.getStringVal("ProjectId", user));
        annotateProjectSummary(ps, p) ;
        DataRecord[] requests = p.getChildrenOfType("Request", user);
        for(int i = 0; i < requests.length; i++){
          try{
            String pm = "";
            try{ pm = requests[i].getStringVal("ProjectManager", user); } catch(NullPointerException npe){}
            if(!cmoOnly || (cmoOnly && !pm.equals(""))){
               RequestSummary rs = new RequestSummary(requests[i].getStringVal("RequestId", user));     
               rs.setRecordId(requests[i].getRecordId());
               ps.addRequestSummary(rs);
            }
          } catch(NullPointerException npe){}
        }
        allProjects.add(ps);
     }
   } catch (Throwable e) {
       log.info(e.getMessage());
       ProjectSummary errorProject = new ProjectSummary("ERROR");
       errorProject.setCmoProjectId(e.getMessage());
       allProjects.add(errorProject);
   }
 
    return allProjects;
  }
}
