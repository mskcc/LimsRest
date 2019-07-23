package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.GetSampleManifestTask;
import org.mskcc.limsrest.limsapi.SampleManifest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

@RestController
public class GetSampleManifest {
    private Log log = LogFactory.getLog(GetSampleManifest.class);
    private final ConnectionQueue connQueue;
    private final GetSampleManifestTask task;

    public GetSampleManifest(ConnectionQueue connQueue, GetSampleManifestTask task) {
        this.connQueue = connQueue;
        this.task = task;
    }

    @RequestMapping("/getSampleManifest")
    public List<SampleManifest> getContent(@RequestParam(value="igoSampleId") String[] igoIds) {
        log.info("Starting to build sample manifest for samples:" + Arrays.toString(igoIds));
        // TODO whiteList
        task.init(igoIds);

        Future<Object> r = connQueue.submitTask(task);

        try {
            SampleManifestResult result = (SampleManifestResult) r.get();
            if (result.error == null) {
                log.info("Returning n rows: " + result.smList.size());
                return result.smList;
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, result.error);
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public static class SampleManifestResult {
        List<SampleManifest> smList;
        String error = null;

        public SampleManifestResult(List<SampleManifest> smList, String error) {
            this.smList = smList;
            this.error = error;
        }
    }
}