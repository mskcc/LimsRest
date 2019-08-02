package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.GetProjectDetails;
import org.mskcc.limsrest.limsapi.ProjectSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Future;

@RestController
public class GetProjectDetailed {
    private final Log log = LogFactory.getLog(GetProjectDetailed.class);
    private final ConnectionQueue connQueue; 
    private final GetProjectDetails task = new GetProjectDetails();
   
    public GetProjectDetailed(ConnectionQueue connQueue){
        this.connQueue = connQueue;
    }

    @GetMapping("/getProjectDetailed")
    public ProjectSummary getContent(@RequestParam(value="project") String project) {
       if (!Whitelists.requestMatches(project)){
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