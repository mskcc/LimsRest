package org.mskcc.limsrest.web;

import java.util.concurrent.Future;
import java.util.List;
import java.util.LinkedList;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.mskcc.limsrest.limsapi.*;
import org.mskcc.limsrest.connection.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


@RestController
@RequestMapping("/")
public class GetProject {
    private static Log log = LogFactory.getLog(GetProject.class);
    private final ConnectionPoolLIMS conn;
    private final GetSamples task;

   
    public GetProject(ConnectionPoolLIMS conn, GetSamples getSamples){
        this.conn = conn;
        this.task = getSamples;
    }

    @GetMapping("/getPmProject")
    public List<RequestSummary> getContent(@RequestParam(value="project") String[] project, @RequestParam(value="filter", defaultValue="false") String filter) {
        List<RequestSummary> rss = new LinkedList<>();
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < project.length; i++){
           if(!Whitelists.requestMatches(project[i])){
               log.info("FAILURE: project is not using a valid format");
               return rss;
           } else{
                sb.append(project[i]);
                if(i < project.length - 1){
                    sb.append(",");
                }
           }
        }
        log.info("Starting get PM project for projects: " + sb.toString());

       task.init(project, filter.toLowerCase());
       Future<Object> result = conn.submitTask(task);
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