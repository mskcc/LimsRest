package org.mskcc.limsrest.web;

import java.util.concurrent.Future;


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
public class GetProjectDetailed {

    private final ConnectionQueue connQueue; 
    private final GetProjectDetails task;
    private Log log = LogFactory.getLog(GetProjectDetailed.class);
   
    public GetProjectDetailed( ConnectionQueue connQueue, GetProjectDetails project){
        this.connQueue = connQueue;
        this.task = project;
    }



    @RequestMapping("/getProjectDetailed")
    public ProjectSummary getContent(@RequestParam(value="project") String project) {
       Whitelists wl = new Whitelists();
       if(!wl.requestMatches(project)){
              ProjectSummary eSum = new ProjectSummary();
              eSum.setRestStatus( "FAILURE: project is not using a valid format");
                return eSum;
        }
       log.info("Getting project detailed for " + project);
       task.init(project);
       Future<Object> result = connQueue.submitTask(task);
       ProjectSummary ps = new ProjectSummary();
       try{
         ps = (ProjectSummary)result.get();
       } catch(Exception e){
         ps.setCmoProjectId(e.getMessage());
       }
       return ps;
   }

   
}

