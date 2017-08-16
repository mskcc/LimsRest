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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


@RestController
public class GetProjectQc {

    private final ConnectionQueue connQueue; 
    private final GetSampleQc task;
    private Log log = LogFactory.getLog(GetProjectQc.class);

    public GetProjectQc( ConnectionQueue connQueue, GetSampleQc getQc){
        this.connQueue = connQueue;
        this.task = getQc;
    }



    @RequestMapping("/getProjectQc")
        public List<RequestSummary> getContent(@RequestParam(value="project", required=false) String[] project) {
            Whitelists wl = new Whitelists();

            List<RequestSummary> rss = new LinkedList<>();
            if(project == null){
                return rss;
            }
            StringBuffer sb = new StringBuffer();
            for(int i = 0; i < project.length; i++){
              if(!wl.requestMatches(project[i])){
                  log.info("FAILURE: project is not using a valid format");
                  return rss;
               }   else{
                   sb.append(project[i]);
                   if(i < project.length - 1){
                      sb.append(",");
                   }
                }
            }
            log.info("Starting get PM project for projects: " + sb.toString());
    
            task.init(project);
            Future<Object> result = connQueue.submitTask(task);
            try{
                rss = (List<RequestSummary>)result.get();
            } catch(Exception e){
                RequestSummary rs = new RequestSummary();
                rs.setInvestigator(e.getMessage());
                rss.add(rs);
            }
            return rss;
        }


}

