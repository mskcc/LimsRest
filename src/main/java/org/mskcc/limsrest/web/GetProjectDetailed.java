package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionPoolLIMS;
import org.mskcc.limsrest.limsapi.GetProjectDetails;
import org.mskcc.limsrest.limsapi.ProjectSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Future;

@RestController
@RequestMapping("/")
public class GetProjectDetailed {
    private static Log log = LogFactory.getLog(GetProjectDetailed.class);
    private final ConnectionPoolLIMS conn;
    private final GetProjectDetails task = new GetProjectDetails();
   
    public GetProjectDetailed(ConnectionPoolLIMS conn){
        this.conn = conn;
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
       Future<Object> result = conn.submitTask(task);
       ProjectSummary ps = new ProjectSummary();
       try{
         ps = (ProjectSummary)result.get();
       } catch(Exception e){
         ps.setCmoProjectId(e.getMessage());
       }
       return ps;
   }
}