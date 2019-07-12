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
    private final ConnectionQueue connQueue;
    private final GetSampleManifestTask task;
    private Log log = LogFactory.getLog(GetSampleManifest.class);

    public GetSampleManifest(ConnectionQueue connQueue, GetSampleManifestTask task) {
        this.connQueue = connQueue;
        this.task = task;
    }

    @RequestMapping("/getSampleManifest")
    public List<SampleManifest> getContent(@RequestParam(value="igoSampleId") String[] igoIds) {
        log.info("Starting to build sample manifest for samples:" + Arrays.toString(igoIds));
        // TODO whiteList
        task.init(igoIds);

        Future<Object> result = connQueue.submitTask(task);
        try {
            Object o = result.get();
            List<SampleManifest> l = (List<SampleManifest>) o;
            log.info("Returning n rows: " + l.size());
            return l;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, igoIds + " Not Found", e);
        }
    }
}