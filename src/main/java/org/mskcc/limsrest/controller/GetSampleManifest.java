package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.model.SampleManifest;
import org.mskcc.limsrest.service.GetSampleManifestTask;
import org.mskcc.limsrest.service.SampleManifest;
import org.mskcc.limsrest.util.IGOTools;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/")
public class GetSampleManifest {
    private static Log log = LogFactory.getLog(GetSampleManifest.class);
    private final ConnectionLIMS conn;

    public GetSampleManifest(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/api/getSampleManifest")
    public List<SampleManifest> getContent(@RequestParam(value="igoSampleId") String[] igoIds, HttpServletRequest request) {
        log.info("/api/getSampleManifest:" + Arrays.toString(igoIds) + " IP:" + request.getRemoteAddr());

        if (igoIds.length > 10) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Maximum 10 samples per query, you sent:" + igoIds.length);
        }
        for (String igoId : igoIds) {
            if (!IGOTools.isValidIGOSampleId(igoId))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid IGO Sample ID: " + igoId);
        }

        GetSampleManifestTask sampleManifest = new GetSampleManifestTask(igoIds, conn);
        GetSampleManifestTask.SampleManifestResult result = sampleManifest.execute();
        if (result == null) {
            log.error("Sample Manifest generation failed for: " + Arrays.toString(igoIds));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        } else if (result.error == null) {
            log.info("Returning n rows: " + result.smList.size());
            return result.smList;
        } else {
            log.error("Sample Manifest generation failed with error: " + result.error);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, result.error);
        }
    }
}