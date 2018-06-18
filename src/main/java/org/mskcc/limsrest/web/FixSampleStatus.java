package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.SetSampleStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Future;

@RestController
public class FixSampleStatus{

    private final ConnectionQueue connQueue; 
    private final SetSampleStatus task;
    private Log log = LogFactory.getLog(AddPoolToFlowcellLane.class);
   
    public FixSampleStatus( ConnectionQueue connQueue, SetSampleStatus setter){
        this.connQueue = connQueue;
        this.task = setter;
    }

    @RequestMapping("/fixSampleStatus")
    public String getContent(@RequestParam(value="sample") String sample, 
                           @RequestParam(value="status") String status, @RequestParam(value="igoUser") String igoUser){
       if(!Whitelists.sampleMatches(sample))
            return "FAILURE: sample is not using a valid format";
    log.info("Starting to fix status by user " + igoUser);
       task.init(sample, status, igoUser);
       Future<Object> result = connQueue.submitTask(task);
       String returnCode = "";
       try{
         returnCode = "Status:" + (String)result.get();
       } catch(Exception e){
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
          returnCode = "ERROR IN ADDING POOL TO LANE: " + e.getMessage() + " TRACE: " + sw.toString();
       }
       return returnCode;
    }
}