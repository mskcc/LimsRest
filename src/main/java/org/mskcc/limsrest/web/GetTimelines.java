package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.GetProjectHistory;
import org.mskcc.limsrest.limsapi.HistoricalEvent;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.Future;

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
            if(!Whitelists.requestMatches(project[i])){
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

