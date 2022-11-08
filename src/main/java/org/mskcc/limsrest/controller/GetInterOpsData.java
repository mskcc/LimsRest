package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.interops.GetInterOpsDataTask;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/")
public class GetInterOpsData {
    private static final Log log = LogFactory.getLog(GetInterOpsData.class);
    private final ConnectionLIMS conn;

    public GetInterOpsData(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/getInterOpsData")
    public ResponseEntity<List<Map<String, String>>> getInterOps(@RequestParam(value = "runId") String runId) {
        log.info("Starting /getInterOpsData " + runId);
        try {
            GetInterOpsDataTask task = new GetInterOpsDataTask(runId, conn);
            List<Map<String, String>> interOps = task.execute();
            return ResponseEntity.ok(interOps);
        } catch (Exception e) {
            log.error(String.format("Error while retrieving results for getInterOpsData run id: %s", runId), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}