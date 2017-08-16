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
public class GetAllStudies{

    private final ConnectionQueue connQueue; 
    private final ListStudies task;
    private Log log = LogFactory.getLog(GetAllStudies.class);

    public GetAllStudies( ConnectionQueue connQueue, ListStudies project){
        this.connQueue = connQueue;
        this.task = project;
    }



    @RequestMapping("/getAllStudies")
    public List<ProjectSummary> getContent(@RequestParam(value="cmoOnly", defaultValue="NULL") String cmoOnly) {
       List<ProjectSummary> ps = new LinkedList<>();
       task.init(cmoOnly);
       Future<Object> result = connQueue.submitTask(task);
       log.info("starting to get all studies");
       try{
         ps = (List<ProjectSummary>)result.get();
       } catch(Exception e){
         ProjectSummary errorProj = new ProjectSummary();
         errorProj.setCmoProjectId(e.getMessage());
         ps.add(errorProj);
       }
       return ps;
   }

   
}

