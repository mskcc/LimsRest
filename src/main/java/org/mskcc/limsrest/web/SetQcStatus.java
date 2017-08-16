package org.mskcc.limsrest.web;

import java.util.concurrent.Future;
import java.util.List;
import java.util.LinkedList;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import org.mskcc.limsrest.limsapi.*;
import org.mskcc.limsrest.connection.*;

import java.io.StringWriter;
import java.io.PrintWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


@RestController
public class SetQcStatus {

    private final ConnectionQueue connQueue; 
    private final ToggleSampleQcStatus task;
    private Log log = LogFactory.getLog(SetQcStatus.class);
   
    public SetQcStatus( ConnectionQueue connQueue, ToggleSampleQcStatus toggle){
        this.connQueue = connQueue;
        this.task = toggle;
    }



    @RequestMapping("/setQcStatus")
    public String getContent(@RequestParam(value="record", required=false) String recordId, @RequestParam(value="status") String status,
                             @RequestParam(value="project", required=false) String request, @RequestParam(value="sample", required=false) String sample,
                             @RequestParam(value="run", required=false) String run, @RequestParam(value="qcType", defaultValue="Seq") String qcType,
                             @RequestParam(value="analyst", required=false) String analyst, @RequestParam(value="note", required=false) String note,
                             @RequestParam(value="user", defaultValue="") String user){
       log.info("Starting to seq Qc status to " + status + "for service" + user);
       Whitelists wl = new Whitelists();
       if(!wl.requestMatches(request)){
            return "FAILURE: The project is not using a valid format. " + wl.requestFormatText(); 
       }
       if(!wl.textMatches(status)){
            return "FAILURE: The status is not using a valid format. " + wl.textFormatText();
        }
       if(!qcType.equals("Seq") && !qcType.equals("Post")){
          return "ERROR: The only valid qc types for this service are Seq and Post";
       }
       if(qcType.equals("Seq") && recordId == null){
          return "ERROR: You must specify a valid record id";
       } else if(qcType.equals("Post") && (request == null || sample == null || run == null)){
          return "ERROR: Post-Sequencing Qc is identified with a triplet: request, sample, run";
       } 
       long record = 0;
       if(recordId != null){
          record = Long.parseLong(recordId);
       }
       task.init(record, status, request, sample, run, qcType, analyst, note);
       Future<Object> result = connQueue.submitTask(task);
       String returnCode = "";
       try{
         returnCode = "NewStatus:" + (String)result.get();
       } catch(Exception e){
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
          returnCode = "ERROR IN SETTING REQUEST STATUS: " + e.getMessage() + " TRACE: " + sw.toString();
       }
       return returnCode;
    }

}

