package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.GenerateSampleCmoIdTask;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

        log.info("Creating task");
        task.init(sampleIgoId);

        log.info("Getting result");
        Future<Object> result = connQueue.submitTask(task);

        try {
            String correctedSampleCmoId = (String) result.get();
            return ResponseEntity.ok(correctedSampleCmoId);
        } catch (Exception e) {
            log.error(String.format("Unable to retrieve result for corrected sample cmo id for sample: %s",
                    sampleIgoId), e);

            ResponseEntity<String> responseEntity = new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
            return responseEntity;
        }
    }
}
