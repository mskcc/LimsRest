package org.mskcc.limsrest.controller;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.servlet.http.HttpServletRequest;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.model.RequestSample;
import org.mskcc.limsrest.model.RequestSampleList;
import org.mskcc.limsrest.model.SampleManifest;
import org.mskcc.limsrest.service.GetProjectDetails;
import org.mskcc.limsrest.service.GetRequestPermissionsTask;
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
@ComponentScan({"org.mskcc.cmo.messaging", "org.mskcc.cmo.common.*"})
@RestController
@RequestMapping("/")
public class IgoNewRequestMetaDbPublisher {
    private final Log log = LogFactory.getLog(IgoNewRequestMetaDbPublisher.class);

    @Value("${nats.igo_request_validator_topic}")
    private String IGO_REQUEST_VALIDATOR_TOPIC;

    @Autowired
    private Gateway messagingGateway;

    @Value("${airflow_pass}")
    private String airflow_pass;

    private final GetProjectDetails task = new GetProjectDetails();
    private final ConnectionLIMS conn;

    public IgoNewRequestMetaDbPublisher(ConnectionLIMS conn, ConnectionPoolLIMS connPool) throws Exception {
        this.conn = conn;
    }

    /**
     * Called by sloancmo.jar when the "Mark Delivery" button is clicked in the LIMS
     * @param requestId
     * @param request
     */
    @GetMapping("/publishIgoRequestToMetaDb")
    public void getContent(@RequestParam(value = "requestId") String requestId, HttpServletRequest request) {
        log.info("/publishIgoRequestToMetaDb for request: " + requestId + " " + request.getRemoteAddr());

        if (!Whitelists.requestMatches(requestId)) {
            log.error("FAILURE: requestId is not using a valid format.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FAILURE: requestId is not using a valid format.");
        }

        callAirflowDeliverPipeline(requestId);

        sendToMetaDb(requestId);
    }

    public void sendToMetaDb(String requestId) {
        RequestSampleList requestDetails = getRequestSampleListDetails(requestId);
        List<Map<String, Object>> sampleManifestList = getSampleManifestListByRequestId(requestDetails);
        // get project id from request id
        String projectId = requestId.split("_")[0];
        publishIgoNewRequestToMetaDb(projectId, requestDetails, sampleManifestList);
    }

    /**
     * Call the Airflow pipeline to deliver pipeline output for a project.
     * @param requestId
     */
    private void callAirflowDeliverPipeline(String requestId) {
        try {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            DataRecordManager drm = vConn.getDataRecordManager();
            List<DataRecord> requestList = drm.queryDataRecords("Request", "RequestId = '" + requestId + "'", user);
            DataRecord requestDataRecord = requestList.get(0);
            String labHeadEmail = requestDataRecord.getStringVal("LabHeadEmail", user);
            String lab = GetRequestPermissionsTask.labHeadEmailToLabName(labHeadEmail);
            String recipe = requestDataRecord.getStringVal("RequestName", user);

            Date execDate = new Date(System.currentTimeMillis() + 10000);
            String body = formatDeliverPipelineJSON(requestId, lab, recipe, execDate);

            String cmd = "/bin/curl -X POST -d '" + body + "' \"http://igo-ln01:8080/api/v1/dags/deliver_pipeline/dagRuns\" -H 'content-type: application/json' --user \"airflow-api:" + airflow_pass + "\"";
            log.info("Calling airflow pipeline: " + cmd);
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("bash", "-c", cmd);
            Process process = processBuilder.start();
        } catch (IoError | NotFound | IOException ex) {
            log.error(ex);
            ex.printStackTrace();
        }
    }

    public static void logResults(Process process, Log log) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String line = "";
        log.info("Airflow exec pipeline error results:");
        while ((line = reader.readLine()) != null) {
            log.info(line);
        }
        reader.close();
    }

    protected static String formatDeliverPipelineJSON(String requestId, String lab, String recipe, Date execDate) {
        //2021-01-01T15:00:00Z - airflow format
        DateFormat airflowFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss");
        airflowFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateStr = airflowFormat.format(execDate);
        dateStr = dateStr.replace(' ', 'T') + "Z";
        // create json body like:
        // {"execution_date": "2022-05-19", "conf": {"project":"13097","pi":"abdelwao","recipe":"RNASeq-TruSeqPolyA"}}
        String conf = "\"conf\":{\"project\":\""+requestId+"\",\"pi\":\""+lab+"\",\"recipe\":\""+recipe+"\"}";
        String body ="{\"execution_date\":\""+dateStr+"\","+conf+"}";
        return body;
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
     * @param sl
     * @return
     */
    private List<Map<String, Object>> getSampleManifestListByRequestId(RequestSampleList sl) {
        // construct list of igo id strings for the 'GetSampleManifestTask'
        List<RequestSample> requestSamples = sl.getSamples();
        String[] igoIds = new String[requestSamples.size()];
        Map<String, Boolean> igoCompleteMap = new HashMap<>();
        for (int i = 0; i < requestSamples.size(); i++) {
            RequestSample rs = requestSamples.get(i);
            igoIds[i] = rs.getIgoSampleId();
            igoCompleteMap.put(rs.getIgoSampleId(), rs.isIgoComplete());
        }

        // fetch list of sample manifests for request sample (igo)ids
        List<SampleManifest> sampleManifestList = null;
        GetSampleManifestTask sampleManifest = new GetSampleManifestTask(igoIds, conn);
        GetSampleManifestTask.SampleManifestResult result = sampleManifest.execute();
        if (result == null) {
            log.error("Sample Manifest generation failed for: " + StringUtils.join(igoIds, ", "));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        } else if (result.error == null) {
            log.info("Returning n rows: " + result.smList.size());
            sampleManifestList = result.smList;
        } else {
            log.error("Sample Manifest generation failed with error: " + result.error);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, result.error);
        }
        List<Map<String, Object>> toReturn = new ArrayList<>();
        Gson gson = new Gson();
        for (SampleManifest sm : sampleManifestList) {
            Map<String, Object> smMap = gson.fromJson(gson.toJson(sm), Map.class);
            smMap.put("igoComplete", igoCompleteMap.get(sm.getIgoId()));
            toReturn.add(smMap);
        }

        return toReturn;
    }

    /**
     * Packages message for CMO MetaDB and publishes to MetaDB NATS server.
     * @param projectId
     * @param requestId
     * @param sampleManifestList
     */
    private void publishIgoNewRequestToMetaDb(String projectId, RequestSampleList requestDetails, List<Map<String, Object>> sampleManifestList) {
        // construct igo request entity to publish to metadb
        Gson gson = new Gson();
        Map<String, Object> igoRequestMap = gson.fromJson(gson.toJson(requestDetails), Map.class);
        // add project id and sample manifest list to returned map
        igoRequestMap.put("projectId", projectId);
        igoRequestMap.put("samples", sampleManifestList);
        String igoRequestJson = gson.toJson(igoRequestMap);

        // publish request json to IGO_REQUEST_VALIDATOR_TOPIC
        try {
            log.info("Publishing to 'IGO_REQUEST_VALIDATOR_TOPIC' " + IGO_REQUEST_VALIDATOR_TOPIC + ": " + requestDetails.getRequestId());
            messagingGateway.publish(IGO_REQUEST_VALIDATOR_TOPIC, igoRequestJson);
        } catch (Exception e) {
            String metadbErrorMsg = "Error during attempt to publish request to topic: " + IGO_REQUEST_VALIDATOR_TOPIC;
            log.error(metadbErrorMsg, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, metadbErrorMsg, e);
        }
    }
}