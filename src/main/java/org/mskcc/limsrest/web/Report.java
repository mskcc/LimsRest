package org.mskcc.limsrest.web;

import java.util.concurrent.Future;
import java.util.LinkedList;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import org.mskcc.limsrest.limsapi.*;
import org.mskcc.limsrest.connection.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



@RestController
public class Report {

    private final ConnectionQueue connQueue; 
    private final GetHiseq task;
    private final GetReadyForIllumina illuminaTask;
    private Log log = LogFactory.getLog(Report.class);
   
    public Report( ConnectionQueue connQueue, GetHiseq getHiseq, GetReadyForIllumina illuminaTask){
        this.connQueue = connQueue;
        this.task = getHiseq;
        this.illuminaTask = illuminaTask;
    }



    @RequestMapping("/getHiseq")
    public LinkedList<RunSummary> getContent(@RequestParam(value="run", required=false) String run, @RequestParam(value="project", required=false) String[] projs) {

       RunSummary rs = new RunSummary("BLANK_RUN", "BLANK_REQUEST");
       LinkedList<RunSummary> runSums = new LinkedList<>();
       Whitelists wl = new Whitelists();
       if(run != null){
         if(!wl.textMatches(run)){
            runSums.add(rs);
            log.info("FAILURE: run is not a valid format");
            return runSums;
         }
         log.info("Starting get Hiseq for run " + run);
         task.init(run);
       } else if(projs != null){
           for(int i = 0; i < projs.length; i++){
                if(!wl.requestMatches(projs[i])){
                    runSums.add(rs);
                    log.info("FAILURE: project is not a valid format");
                    return runSums;
                }
           }
           log.info("Starting get Hiseq for projects");
          task.init(projs);
        }
       else{
         log.info("Starting get Hiseq with no provided data");
         runSums.add(rs);
         return runSums;
       }
       Future<Object> result = connQueue.submitTask(task);
       try{
         runSums = (LinkedList<RunSummary>)result.get();
       } catch(Exception e){
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         e.printStackTrace(pw);
         rs.setInvestigator(e.getMessage() + " TRACE: " + sw.toString());
         runSums.add(rs);
       }
       return runSums;
   }

     @RequestMapping("/getHiseqList")
    public LinkedList<RunSummary> getContent() {
       log.info("Starting get Hiseq List");
       task.init("");
       Future<Object> result = connQueue.submitTask(task);
       RunSummary rs = new RunSummary("BLANK_RUN", "BLANK_REQUEST");
       LinkedList<RunSummary> runSums = new LinkedList<>();
       try{
         runSums = (LinkedList<RunSummary>)result.get();
       } catch(Exception e){
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         e.printStackTrace(pw);
         rs.setInvestigator(e.getMessage() + " TRACE: " + sw.toString());
         runSums.add(rs);
       }
       return runSums;
   }

   @RequestMapping("/planRuns")
   public LinkedList<RunSummary> getPlan(@RequestParam(value="user") String user){
           log.info("Starting plan Runs for user " + user);
       illuminaTask.init();
       Future<Object> result = connQueue.submitTask(illuminaTask);
       RunSummary rs = new RunSummary("BLANK_RUN", "BLANK_REQUEST");
       LinkedList<RunSummary> runSums = new LinkedList<>();
       try{
         runSums = (LinkedList<RunSummary>)result.get();
       } catch(Exception e){
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         e.printStackTrace(pw);
         rs.setInvestigator(e.getMessage() + " TRACE: " + sw.toString());
         runSums.add(rs);
       }
       return runSums; 

   }
}

