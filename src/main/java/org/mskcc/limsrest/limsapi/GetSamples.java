
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
 * A queued task that takes a request id and returns all some request information and some sample information 
 * 
 * @author Aaron Gabow
 * 
 */
@Service
public class GetSamples  extends LimsTask 
{
  
  private Log log = LogFactory.getLog(GetSamples.class);

  protected String[] projects;

  public void init(final String[] projects){
    if(projects != null)
        this.projects = projects.clone();
  }

 //execute the velox call
@PreAuthorize("hasRole('READ')")
@Override
 public Object execute(VeloxConnection conn){
// private void runProgram(User apiUser, DataRecordManager dataRecordManager) {
  List<RequestSummary> rss = new LinkedList<>();
  HashSet<String> known = new HashSet<>();
  
  

  try { 
    if(projects == null){
      throw new Exception("Unable to get project information with no project specified");
    }
    StringBuffer sb = new StringBuffer();
    sb.append("(");
    for(int i = 0; i < projects.length; i++){
        sb.append("'");
        sb.append(projects[i]);
        sb.append("'");
        if(i < projects.length - 1){
            sb.append(",");
        }
    }
    sb.append(")");
    List<DataRecord> requestList = dataRecordManager.queryDataRecords("Request", "RequestId in " + sb.toString()  , user);
    List<Map<String, Object>> requestFields = dataRecordManager.getFieldsForRecords(requestList, user);
    List<List<Map<String, Object>>> descendantSampleFields =  dataRecordManager.getFieldsForDescendantsOfType(requestList, "Sample",  user);
    for(int i = 0;i < requestFields.size(); i++){
        String project = (String)(requestFields.get(i).get("RequestId"));
        String igoBase = project.split("_")[0];
        RequestSummary rs = new RequestSummary(project);
        annotateRequestSummary(rs, requestFields.get(i));
        for(Map<String, Object> sampleFields : descendantSampleFields.get(i)){
                String igoId = (String)sampleFields.get("SampleId"); 
                String altId = (String)sampleFields.get("OtherSampleId");
                if(known.contains(altId)){
                     continue;
                }
                if(!igoId.startsWith(igoBase)){
                    continue;
                }
                SampleSummary ss = new SampleSummary();
                annotateSampleSummary(ss, sampleFields);
                rs.addSample(ss);

        }
        rss.add(rs);
    }

  } catch (Throwable e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);

    log.info(e.getMessage() +  " " + sw.toString());
    rss.add(RequestSummary.errorMessage(e.getMessage())); 
  }

  return rss;
 }

}
