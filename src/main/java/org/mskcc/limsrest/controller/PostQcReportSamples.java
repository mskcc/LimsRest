package org.mskcc.limsrest.controller;


import io.swagger.annotations.ApiOperation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetQcReportSamplesTask;
import org.mskcc.limsrest.service.QcReportSampleList;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Endpoint to return QC Report data based on Request and InvestigatorSampleIds
// Expects existing request, will throw no exception if request does not exist in LIMS

@RestController
@RequestMapping("/")
public class PostQcReportSamples {
    private static Log log = LogFactory.getLog(GetProjectQc.class);
    private final ConnectionLIMS conn;

    public PostQcReportSamples(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @RequestMapping("/postQcReportSamples")
    @ApiOperation(tags = "/getQcReportSamples", httpMethod = "POST", notes = "Returns QC report table samples, pathology samples and QC attachment record ids for request and investigator ids.", value = "Get QC Report Samples, Pathology samples and QC attachments.")
    public QcReportSampleList getQcReportSamples(@RequestBody Map<String, String> payload) {
        log.info(payload);
        if (!payload.containsKey("requestId") || !payload.containsKey("otherSampleIds")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FAILURE: requestId and otherSampleIds must be present.");
        }

        String requestId = payload.get("requestId");
        String otherSampleIdsString = payload.get("otherSampleIds");
        List<String> otherSampleIds = Stream.of(otherSampleIdsString.split(",", -1))
                .collect(Collectors.toList());

        log.info("Starting get /postQcReportSamples for request: " + requestId);
        if (!Whitelists.requestMatches(requestId)) {
            log.error("FAILURE: requestId is not using a valid format.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FAILURE: requestId is not using a valid format.");
        }

        GetQcReportSamplesTask task = new GetQcReportSamplesTask(otherSampleIds, requestId, conn);
        QcReportSampleList rsl = task.execute();

        return rsl;
    }
}