package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionPoolLIMS;
import org.mskcc.limsrest.limsapi.ListStudies;
import org.mskcc.limsrest.limsapi.ProjectSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/")
public class GetAllStudies {
    private static Log log = LogFactory.getLog(GetAllStudies.class);

    private final ConnectionPoolLIMS conn;
    private final ListStudies task;

    public GetAllStudies(ConnectionPoolLIMS conn, ListStudies task) {
        this.conn = conn;
        this.task = task;
    }

    @GetMapping("/getAllStudies")
    public List<ProjectSummary> getContent(@RequestParam(value = "cmoOnly", defaultValue = "NULL") String cmoOnly) {
        log.info("Starting /getAllStudies");
        task.init(cmoOnly);
        Future<Object> result = conn.submitTask(task);
        try {
            List<ProjectSummary> r = (List<ProjectSummary>) result.get();
            return r;
        } catch (Exception e) {
            ProjectSummary errorProj = new ProjectSummary();
            errorProj.setCmoProjectId(e.getMessage());
            List<ProjectSummary> ps = new LinkedList<>();
            ps.add(errorProj);
            return ps;
        }
    }
}