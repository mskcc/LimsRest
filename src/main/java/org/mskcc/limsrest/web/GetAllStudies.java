package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.ListStudies;
import org.mskcc.limsrest.limsapi.ProjectSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

@RestController
public class GetAllStudies {
    private final Log log = LogFactory.getLog(GetAllStudies.class);
    private final ConnectionQueue connQueue;
    private final ListStudies task = new ListStudies();

    public GetAllStudies(ConnectionQueue connQueue) {
        this.connQueue = connQueue;
    }

    @GetMapping("/getAllStudies")
    public List<ProjectSummary> getContent(@RequestParam(value = "cmoOnly", defaultValue = "NULL") String cmoOnly) {
        task.init(cmoOnly);
        Future<Object> result = connQueue.submitTask(task);
        log.info("Starting /getAllStudies");
        try {
            return (List<ProjectSummary>) result.get();
        } catch (Exception e) {
            ProjectSummary errorProj = new ProjectSummary();
            errorProj.setCmoProjectId(e.getMessage());
            List<ProjectSummary> ps = new LinkedList<>();
            ps.add(errorProj);
            return ps;
        }
    }
}