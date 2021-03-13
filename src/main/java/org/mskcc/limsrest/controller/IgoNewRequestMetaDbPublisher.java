package org.mskcc.limsrest.controller;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.model.RequestSample;
import org.mskcc.limsrest.model.RequestSampleList;
import org.mskcc.limsrest.model.SampleManifest;
import org.mskcc.limsrest.service.GetProjectDetails;
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

    private final GetProjectDetails task = new GetProjectDetails();
    private Log log = LogFactory.getLog(IgoNewRequestMetaDbPublisher.class);
    private final ConnectionLIMS conn;
    private final ConnectionPoolLIMS connPool;

    public IgoNewRequestMetaDbPublisher(ConnectionLIMS conn, ConnectionPoolLIMS connPool) throws Exception {
        this.conn = conn;
        this.connPool = connPool;
    }

    @GetMapping("/publishIgoRequestToMetaDb")
    public void getContent(@RequestParam(value = "requestId") String requestId, HttpServletRequest request) {
        log.info("/publishIgoRequestToMetaDb for request: " + requestId + " " + request.getRemoteAddr());

        if (!Whitelists.requestMatches(requestId)) {
            log.error("FAILURE: requestId is not using a valid format.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FAILURE: requestId is not using a valid format.");
        }
        // get project id from request id
        String[] parts = requestId.split("_");
        String projectId = parts[0];
        RequestSampleList requestDetails = getRequestSampleListDetails(requestId);
        List<SampleManifest> sampleManifestList = getSampleManifestListByRequestId(requestDetails);
        publishIgoNewRequestToMetaDb(projectId, requestDetails, sampleManifestList);
    }

    /**
     * Returns request metadata given a request id.
     * @param requestId
     * @return
     */
    private RequestSampleList getRequestSampleListDetails(String requestId) {
        // fetch metadata and samples for request
        RequestSampleList sl = null;
        try {
            GetRequestSamplesTask t = new GetRequestSamplesTask(requestId, conn);
             sl = t.execute();
            if ("NOT_FOUND".equals(sl.getRequestId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, requestId + " Request Not Found");
            }
        } catch (Exception e) {
            log.error(e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        return sl;
    }

    /**
     * Returns list of sample manifest instances given a request id.
     * @param requestId
     * @return
     */
    private List<SampleManifest> getSampleManifestListByRequestId(RequestSampleList sl) {
        // construct list of igo id strings for the 'GetSampleManifestTask'
        List<String> igoIds = new ArrayList<>();
        for (RequestSample rs : sl.getSamples()) {
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
        return sampleManifestList;
    }

    /**
     * Packages message for CMO MetaDB and publishes to MetaDB NATS server.
     * @param projectId
     * @param requestId
     * @param sampleManifestList
     */
    private void publishIgoNewRequestToMetaDb(String projectId, RequestSampleList requestDetails, List<SampleManifest> sampleManifestList) {
        // construct igo request entity to publish to metadb
        Gson gson = new Gson();
        Map<String, Object> igoRequestMap = gson.fromJson(gson.toJson(requestDetails), Map.class);
        // add project id and sample manifest list to returned map
        igoRequestMap.put("projectId", projectId);
        igoRequestMap.put("samples", sampleManifestList);
        String igoRequestJson = gson.toJson(igoRequestMap);
        try {
            log.debug("Publishing cmo request entity: " + igoRequestJson);
            messagingGateway.publish(IGO_NEW_REQUEST_TOPIC, igoRequestJson);
        } catch (Exception e) {
            String metadbErrorMsg = "Error during attempt to publish new request entity to MetaDB with request id: " + requestDetails.getRequestId();
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
