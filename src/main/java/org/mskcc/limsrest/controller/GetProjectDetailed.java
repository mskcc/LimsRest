package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetProjectDetailsTask;
import org.mskcc.limsrest.service.ProjectSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class GetProjectDetailed {
    private static Log log = LogFactory.getLog(GetProjectDetailed.class);
    private final ConnectionLIMS conn;

    public GetProjectDetailed(ConnectionLIMS conn){
        this.conn = conn;
    }

    @GetMapping("/getProjectDetailed")
    public ProjectSummary getContent(@RequestParam(value="project") String project) {
       if (!Whitelists.requestMatches(project)){
              ProjectSummary eSum = new ProjectSummary();
              eSum.setRestStatus( "FAILURE: project is not using a valid format");
                return eSum;
       }
       log.info("/getProjectDetailed " + project);
       GetProjectDetailsTask task = new GetProjectDetailsTask(project, conn);
       ProjectSummary ps = task.execute();
       return ps;
   }
}