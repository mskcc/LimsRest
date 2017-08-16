package org.mskcc.limsrest.web;

import java.util.concurrent.Future;
import java.util.LinkedList;
import java.util.Set;
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
public class GetTimelines {

    private final ConnectionQueue connQueue; 
    private final GetProjectHistory task;
    private Log log = LogFactory.getLog(GetTimelines.class);
   
    public GetTimelines(ConnectionQueue connQueue, GetProjectHistory getHistory){
        this.connQueue = connQueue;
        this.task = getHistory;
    }



    @RequestMapping("/getTimeline")
    public LinkedList<HistoricalEvent> getContent(@RequestParam(value="project") String[] project) {
        LinkedList<HistoricalEvent> timeline = new LinkedList<>();
        for(int i = 0; i < project.length; i++){
            Whitelists wl = new Whitelists();
            if(!wl.requestMatches(project[i])){
                return timeline;                 
            }
        }
       log.info("Starting get Timeline " + project[0]);
       task.init(project);
       Future<Object> result = connQueue.submitTask(task);
       try{
         timeline = new LinkedList((Set<HistoricalEvent>)result.get());
       } catch(Exception e){
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         e.printStackTrace(pw);
         log.info(e.getMessage() + " TRACE: " + sw.toString());
       }
       log.info("Completed timeline");
       return timeline;
   }

}

