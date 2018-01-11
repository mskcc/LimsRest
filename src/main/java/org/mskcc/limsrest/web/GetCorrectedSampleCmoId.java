package org.mskcc.limsrest.web;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.GenerateSampleCmoIdTask;
import org.mskcc.limsrest.staticstrings.Constants;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Future;

@RestController
public class GetCorrectedSampleCmoId {
    private static final Log log = LogFactory.getLog(GetCorrectedSampleCmoId.class);

    private final ConnectionQueue connQueue;
    private final GenerateSampleCmoIdTask task;

    public GetCorrectedSampleCmoId(ConnectionQueue connQueue, GenerateSampleCmoIdTask generateSampleCmoIdTask) {
        this.connQueue = connQueue;
        this.task = generateSampleCmoIdTask;
    }

    @RequestMapping("/getSampleCmoId")
    public ResponseEntity<String> getSampleCmoId(@RequestParam(value = "sampleIgoId") String sampleIgoId) {
        log.info(String.format("Starting to generate sample cmo id for sample igo id: %s", sampleIgoId));

        log.info("Creating Generate sample cmo id task");
        task.init(sampleIgoId);

        log.info("Getting result of Generate sample cmo id task");
        Future<Object> result = connQueue.submitTask(task);

        try {
            String correctedSampleCmoId = (String) result.get();

            log.info(String.format("Generated CMO Sample ID: %s", correctedSampleCmoId));
            return ResponseEntity.ok(correctedSampleCmoId);
        } catch (Exception e) {
            log.error(String.format("Error while generating CMO Sample Id for sample: %s", sampleIgoId), e);

            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.add(Constants.ERRORS, String.format("Error while generating CMO Sample Id for sample: %s. Cause: " +
                    "%s", sampleIgoId, ExceptionUtils.getRootCauseMessage(e)));

            return new ResponseEntity<>(headers, HttpStatus.OK);
        }
    }
}
