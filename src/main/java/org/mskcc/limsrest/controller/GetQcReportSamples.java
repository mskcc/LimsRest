package org.mskcc.limsrest.controller;

import io.swagger.annotations.ApiOperation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetQcReportSamplesTask;
import org.mskcc.limsrest.service.QcReportSampleList;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

// Endpoint to return QC Report data based on Request and InvestigatorSampleIds
// Expects existing request, will throw no exception if request does not exist in LIMS

@RestController
@RequestMapping("/")
public class GetQcReportSamples {
    private static Log log = LogFactory.getLog(GetProjectQc.class);
    private final ConnectionLIMS conn;

    public GetQcReportSamples(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @RequestMapping("/getQcReportSamples")
    @ApiOperation(tags="/getQcReportSamples", httpMethod = "GET", notes = "Get QC report table samples, pathology samples and QC attachment record ids for request and investigator ids.", value = "Get QC Report Samples, Pathology samples and QC attachments.")
    public QcReportSampleList getQcReportSamples(@RequestParam(value = "request", required = true) String requestId, @RequestParam(value = "samples", required = true) List<String> otherSampleIds) {
        log.info("Starting get /getQcReportSamples for request: " + requestId);
        if (!Whitelists.requestMatches(requestId)) {
            log.error("FAILURE: requestId is not using a valid format.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FAILURE: requestId is not using a valid format.");
        }

        GetQcReportSamplesTask task = new GetQcReportSamplesTask(otherSampleIds, requestId, conn);
        QcReportSampleList rsl = task.execute();

        return rsl;
    }
}