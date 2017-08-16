
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
 * A queued task that takes a request id and returns all some request information and all the sample-level qc information for all runs
 * 
 * @author Aaron Gabow
 * 
 */
public class DebugTask  extends LimsTask 
{


  public void init(String project){
  }
 //put it in the completion service
 @Override
 public Object call() throws Exception {
   Object result; 


    velox_conn.open();
    boolean isConn = false;
    try {
      if (velox_conn.isConnected()) {
        isConn = true;
        user = velox_conn.getUser();
        dataRecordManager = velox_conn.getDataRecordManager();
        dataMgmtServer = velox_conn.getDataMgmtServer();
      }
      result = VeloxStandalone.run(velox_conn, this);
    }
     finally {
        if(velox_conn != null){
            velox_conn.close();
        }
    }

  
    if(velox_conn == null){
        ((RequestSummary)result).setInvestigator("Null conn");
    }
    else if(!isConn ){
        ((RequestSummary)result).setInvestigator("Bad conn");
    }
   return result;
 }

  @Override
  public Object execute(VeloxConnection conn) {
    RequestSummary rs = new RequestSummary("Please work");
    return rs;
  }


}
