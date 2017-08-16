
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

import org.mskcc.limsrest.staticstrings.Messages;

import com.velox.api.datamgmtserver.DataMgmtServer;
import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sapioutils.client.standalone.VeloxStandalone;
import com.velox.sapioutils.client.standalone.VeloxStandaloneException;
import com.velox.sapioutils.client.standalone.VeloxStandaloneManagerContext;
import com.velox.sapioutils.client.standalone.VeloxExecutable;

import com.velox.sloan.cmo.utilities.SloanCMOUtils;


/**
 * A queued task that takes a list of request id and map of values and returns them 
 * 
 * @author Aaron Gabow
 * 
 */
@Service
public class GetRequest  extends LimsTask 
{
  String igoUser;
  String[] requestIds;
  String[]  possibleRequestFields;

  public void init(String igoUser, String[] requestIds, String[] requestFields){
    this.igoUser = igoUser;
    this.requestIds = requestIds; 
    this.possibleRequestFields = requestFields;
  }
 //execute the velox call

@PreAuthorize("hasRole('ADMIN')")
@Override
 public Object execute(VeloxConnection conn){
  LinkedList<RequestDetailed> rds = new LinkedList<>();
 
   try {
    StringBuilder sb = new StringBuilder();
    sb.append("(");
    for(String r : requestIds){
        sb.append("'");
        sb.append(r);
        sb.append("',");
    }
    if(sb.length() > 1){
       sb.setLength(sb.length() - 1);
    }
    sb.append(")");
    List<DataRecord> matchedRequests = dataRecordManager.queryDataRecords("Request", "RequestId in " + sb.toString(), user);
    if(matchedRequests.size() == 0){
       throw new LimsException("No Request record in the lims matches the ids: " + sb.toString());
    }
     
    for(DataRecord request : matchedRequests){
       RequestDetailed rd = new RequestDetailed();
       annotateRequestDetailed(rd, request);
       rds.push(rd);
    }
  } catch (Throwable e) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      RequestDetailed rd = new RequestDetailed();
      rd.setRequestType(Messages.ERROR_IN + " GetBanked: " + e.getMessage());
      rds.addFirst(rd);
      return rds; 
  
  }

  return rds; 
 }

}
