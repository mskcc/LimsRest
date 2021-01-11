package org.mskcc.limsrest.controller;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.shared.SampleManifest;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetRequestSamplesTask;
import org.mskcc.limsrest.service.GetSampleManifestTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Publishes CMO requests as package to MetaDB with NATS messaging.
 * Published package includes request ID and all samples for request.
 * @author ochoaa
 */
@ComponentScan("org.mskcc.cmo.messaging")
@RestController
@RequestMapping("/")
public class IgoNewRequestMetaDbPublisher {

    @Value("${nats.publisher_topic}")
    private String IGO_NEW_REQUEST_TOPIC;

    @Autowired
    private Gateway messagingGateway;

    private Log log = LogFactory.getLog(IgoNewRequestMetaDbPublisher.class);
    private final ConnectionLIMS conn;

    public IgoNewRequestMetaDbPublisher(ConnectionLIMS conn) throws Exception {
        this.conn = conn;
        if (messagingGateway == null) {
            log.error("Error establishing connection to NATS server, messagingGateway is NULL");
        } else {
            if (!messagingGateway.isConnected()) {
                messagingGateway.connect();
            }
        }
    }

    @GetMapping("/publishIgoRequestToMetaDb")
    public void getContent(@RequestParam(value = "requestId") String requestId,
            @RequestParam(value = "projectId") String projectId, HttpServletRequest request) {
        log.info("/publishIgoRequestToMetaDb for request: " + requestId + " " + request.getRemoteAddr());

        if (!Whitelists.requestMatches(requestId)) {
            log.error("FAILURE: requestId is not using a valid format.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FAILURE: requestId is not using a valid format.");
        }

        // fetch samples for request
        GetRequestSamplesTask.RequestSampleList sl = null;
        try {
            GetRequestSamplesTask t = new GetRequestSamplesTask(requestId, conn);
             sl = t.execute();
            if ("NOT_FOUND".equals(sl.requestId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, requestId + " Request Not Found");
            }
        } catch (Exception e) {
            log.error(e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        // construct list of igo id strings for the 'GetSampleManifestTask'
        List<String> igoIds = new ArrayList<>();
        for (GetRequestSamplesTask.RequestSample rs : sl.getSamples()) {
            igoIds.add(rs.getIgoSampleId());
        }

        // fetch list of sample manifests for request sample (igo)ids
        List<SampleManifest> sampleManifestList = null;
        GetSampleManifestTask sampleManifest = new GetSampleManifestTask((String[]) igoIds.toArray(), conn);
        GetSampleManifestTask.SampleManifestResult result = sampleManifest.execute();
        if (result == null) {
            log.error("Sample Manifest generation failed for: " + igoIds.toString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        } else if (result.error == null) {
            log.info("Returning n rows: " + result.smList.size());
            sampleManifestList = result.smList;
        } else {
            log.error("Sample Manifest generation failed with error: " + result.error);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, result.error);
        }

        // construct igo request entity to publish to metadb
        Gson gson = new Gson();
        Map<String, Object> igoRequestMap = new HashMap<>();
        igoRequestMap.put("projectId", projectId);
        igoRequestMap.put("requestId", requestId);
        igoRequestMap.put("sampleManifestList", sampleManifestList);
        String igoRequestJson = gson.toJson(igoRequestMap);
        try {
            log.debug("Publishing cmo request entity: " + igoRequestJson);
            messagingGateway.publish(IGO_NEW_REQUEST_TOPIC, igoRequestJson);
        } catch (Exception e) {
            String metadbErrorMsg = "Error during attempt to publish new request entity to MetaDB with request id: " + requestId;
            log.error(metadbErrorMsg);
            if (log.isDebugEnabled()) {
                StringBuilder builder = new StringBuilder(metadbErrorMsg);
                builder.append("\n****FAILED IGO_NEW_REQUEST MESSAGE****\n")
                        .append(igoRequestJson)
                        .append("\n****************\n");
                log.debug(builder);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, metadbErrorMsg, e);
        }
    }
}
