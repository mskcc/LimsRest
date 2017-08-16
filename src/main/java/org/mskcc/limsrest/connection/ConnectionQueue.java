package org.mskcc.limsrest.connection;

import java.util.concurrent.*;
import com.velox.sapioutils.client.standalone.VeloxConnection;

import org.mskcc.limsrest.limsapi.LimsTask;

public class ConnectionQueue{

  private final ExecutorCompletionService<Object> taskList;
  private final ExecutorService taskExecutor;
  private final VeloxConnection connection;

  public ConnectionQueue(String host, String port, String user, String password, String guid){
   taskExecutor = Executors.newFixedThreadPool(1);
   taskList = new ExecutorCompletionService<Object>(taskExecutor);
   connection = new VeloxConnection(host, Integer.parseInt(port), guid, user, password);


}
  public Future<Object> submitTask(LimsTask task){
    //multiple queries will contain connection at a time, but does not matter as long as only one query opens the connection at a time
    
     task.setVeloxConnection(connection);
     return taskList.submit(task);
  }


  public void cleanup(){
   taskExecutor.shutdown();
   try{
       if(connection != null && connection.isConnected()){
          connection.close();
       }
    }catch(Exception e){}
     
  }
}
