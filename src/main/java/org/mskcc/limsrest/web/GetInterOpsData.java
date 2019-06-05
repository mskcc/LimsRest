package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.PromoteBanked;
import org.mskcc.limsrest.limsapi.interops.GetInterOpsDataTask;
import org.mskcc.util.Constants;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

@RestController
public class GetInterOpsData {
    private static final Log log = LogFactory.getLog(GetInterOpsData.class);

    private final ConnectionQueue connQueue;
    private final GetInterOpsDataTask task;

    public GetInterOpsData(ConnectionQueue connQueue, GetInterOpsDataTask task) {
        this.connQueue = connQueue;
        this.task = task;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping("/getInterOpsData")
    public ResponseEntity<List<Map<String, String>>> getInterOps(@RequestParam(value = "runId") String runId) {
        List<Map<String, String>> interOps = new ArrayList<>();

        try {
            task.init(runId);
            Future<Object> result = connQueue.submitTask(task);
            interOps = (List<Map<String, String>>) result.get();
        } catch (Exception e) {
            log.error(String.format("Error while retrieving results for getInterOpsData run id: %s", runId), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).header(Constants.ERROR, e.getMessage()).body(interOps);
        }

        return ResponseEntity.ok(interOps);
    }
}
