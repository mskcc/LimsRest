package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionPoolLIMS;
import org.mskcc.limsrest.limsapi.GetSampleManifestTask;
import org.mskcc.limsrest.limsapi.SampleManifest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/")
public class GetSampleManifest {
    private static Log log = LogFactory.getLog(GetSampleManifest.class);
    private final ConnectionPoolLIMS conn;
    private final GetSampleManifestTask task = new GetSampleManifestTask();

    public GetSampleManifest(ConnectionPoolLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/api/getSampleManifest")
    public List<SampleManifest> getContent(@RequestParam(value="igoSampleId") String[] igoIds, HttpServletRequest request) {
        log.info("Sample Manifest client IP:" + request.getRemoteAddr());
        log.info("Sample Manifest IGO IDs:" + Arrays.toString(igoIds));
        if (igoIds.length > 10) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Maximum 10 samples per query, you sent:" + igoIds.length);
        }
        // TODO whiteList Whitelists.sampleMatches()
        task.init(igoIds);

        Future<Object> r = conn.submitTask(task);

        try {
            SampleManifestResult result = (SampleManifestResult) r.get();
            if (result.error == null) {
                log.info("Returning n rows: " + result.smList.size());
                return result.smList;
            } else {
                log.error("Sample Manifest generation failed with error: " + result.error);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, result.error);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Sample manifest exception.", e);
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