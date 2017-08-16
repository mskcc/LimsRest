
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
import com.velox.api.servermanager.PickListConfig;
import com.velox.api.servermanager.PickListManager;



/**
 * A queued task that takes a pick list name and returns the possible values 
 * 
 * @author Aaron Gabow
 * 
 */
public class GetPickList  extends LimsTask 
{
  private String picklist;
  

  public void init(String picklist){
    this.picklist = picklist;
  }

 //execute the velox call
 @Override
 public Object execute(VeloxConnection conn){
// private void runProgram(User apiUser, DataRecordManager dataRecordManager) {
  List<String> values = new LinkedList<String>();
  try { 
    PickListManager picklister = dataMgmtServer.getPickListManager(user);
    PickListConfig pickConfig = picklister.getPickListConfig(picklist);
    if(pickConfig != null){
        values = pickConfig.getEntryList();
    }
  }catch(Throwable e){}  
 
  if(!values.equals("Exemplar Sample Type")){
    return values;
  } else{
    String[] blacklist = {"cDNA", "cDNA Library", "Plasma"};
    values.removeAll(Arrays.asList(blacklist));
    return values;
  }
 }
}
