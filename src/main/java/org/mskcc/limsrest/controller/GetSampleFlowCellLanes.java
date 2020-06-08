package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.GetSampleFlowCellLanesTask;
import org.mskcc.limsrest.service.interops.GetInterOpsDataTask;
import org.mskcc.util.Constants;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/")
public class GetSampleFlowCellLanes {
    private static final Log log = LogFactory.getLog(org.mskcc.limsrest.controller.GetSampleFlowCellLanes.class);

    private final ConnectionLIMS conn;

    public GetSampleFlowCellLanes(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/getSampleFlowCellLanes")
    public ResponseEntity<Map<String, Object>> getInterOps(@RequestParam(value = "projects") List<String> projects) {
        Map<String, Object> sampleMappings = new HashMap<>();

        log.info("Starting get /getSampleFlowCellLanes. Projects: " + projects.toString());
        try {
            GetSampleFlowCellLanesTask task = new GetSampleFlowCellLanesTask(projects, this.conn);
            List<Map<String, Object>> projectList = task.execute();
            sampleMappings.put("projects", projectList);
        } catch (Exception e) {
            log.error(String.format("Error while retrieving results for getInterOpsData run id: %s", projects.toString()), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).header(Constants.ERROR, e.getMessage()).body(new HashMap<>());
        }

        return ResponseEntity.ok(sampleMappings);
    }
}
