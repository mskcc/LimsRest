package org.mskcc.limsrest.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.SampleModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 *
 */
public class GetRequestSamplesTask {
    private static Log log = LogFactory.getLog(GetRequestSamplesTask.class);

    private ConnectionLIMS conn;
    private String requestId;

    public GetRequestSamplesTask(String requestId, ConnectionLIMS conn) {
        this.requestId = requestId;
        this.conn = conn;
    }

    public GetRequestSamplesTask.RequestSampleList execute() {
        try {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            DataRecordManager drm = vConn.getDataRecordManager();

            List<DataRecord> requestList = drm.queryDataRecords("Request", "RequestId = '" + this.requestId + "'", user);
            if (requestList.size() != 1) {  // error: request ID not found or more than one found
                log.error("Request not found:" + requestId);
                return new RequestSampleList("NOT_FOUND");
            }

            // get set of all samples in that request that are "IGO Complete"
            HashSet<String> samplesIGOComplete = getSamplesIGOComplete(requestId, user, drm);
            log.info("Samples IGO Complete: " + samplesIGOComplete.size());

            DataRecord requestDataRecord = requestList.get(0);
            DataRecord[] samples = requestDataRecord.getChildrenOfType("Sample", user);
            log.info("Child samples found: " + samples.length);

            List<RequestSample> sampleList = new ArrayList<>();
            String recipe = "";
            for (DataRecord sample : samples) {
                String igoId = sample.getStringVal("SampleId", user);
                String sampleRecipe = sample.getStringVal(SampleModel.RECIPE, user);
                if ("Fingerprinting".equals(sampleRecipe)) // for example 07951_S_50_1, skip for pipelines for now
                    continue;
                else if (!GetSampleManifestTask.isPipelineRecipe(sampleRecipe))
                    continue;
                else
                    recipe = sampleRecipe;
                String othersampleId = sample.getStringVal("OtherSampleId", user);
                boolean igoComplete = samplesIGOComplete.contains(othersampleId);

                RequestSample rs = new RequestSample(othersampleId, igoId, igoComplete);
                sampleList.add(rs);
            }
            RequestSampleList rsl = new RequestSampleList(requestId, sampleList);

            if (isIMPACTOrHEMEPACTBeforeIMPACT505(recipe)) {
                rsl.pooledNormals = findPooledNormals(requestId);
            }
            rsl.setRecipe(recipe);
            rsl.setPiEmail(requestDataRecord.getStringVal("PIemail", user));
            rsl.setLabHeadName(requestDataRecord.getStringVal("LaboratoryHead", user));
            rsl.setLabHeadEmail(requestDataRecord.getStringVal("LabHeadEmail", user));
            rsl.setProjectManagerName(requestDataRecord.getStringVal("ProjectManager", user));

            rsl.setInvestigatorName(requestDataRecord.getStringVal("Investigator", user));
            rsl.setInvestigatorEmail(requestDataRecord.getStringVal("Investigatoremail", user));
            rsl.setDataAnalystName(requestDataRecord.getStringVal("DataAnalyst", user));
            rsl.setDataAnalystEmail(requestDataRecord.getStringVal("DataAnalystEmail", user));
            for (RequestSample s : sampleList)
                System.out.println(s.igoSampleId);
            return rsl;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Pooled normals were added to IMPACT requests up to IMPACT505 and used by the pipeline for
     * sample pairing when the patient's normal sample was not present.
     * @param recipe
     * @return
     */
    protected static boolean isIMPACTOrHEMEPACTBeforeIMPACT505(String recipe) {
        if ("IMPACT341,IMPACT410,IMPACT410+,IMPACT468,HemePACT_v3,HemePACT_v4".contains(recipe))
            return true;
        return false;
    }

    /**
     * Returns set of all samples IGO Complete by seqanalysissampleqc.othersampleid
     * @param requestId
     * @return
     * @throws Exception
     */
    protected HashSet<String> getSamplesIGOComplete(String requestId, User user, DataRecordManager drm) throws Exception {
        String whereClause = "PassedQC = 1 AND SeqQCStatus = 'Passed' AND Request = '" + requestId + "'";
        List<DataRecord> listIGOComplete = drm.queryDataRecords("SeqAnalysisSampleQC", whereClause, user);
        HashSet<String> samplesIGOComplete = new HashSet<>();
        for (DataRecord r : listIGOComplete) {
            samplesIGOComplete.add(r.getStringVal("OtherSampleId", user));
        }
        return samplesIGOComplete;
    }

    /*
    Finds all pooled normals included on any run for a given request.
     */
    public static List<String> findPooledNormals(String request) {
        // TODO
        String url = "http://delphi.mskcc.org:8080/ngs-stats/rundone/getpoolednormals/" + request;
        log.info("Finding pooled normal fastqs in fastq DB for: " + url);

        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<List<ArchivedFastq>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ArchivedFastq>>() {
                    });
            List<ArchivedFastq> fastqList = response.getBody();
            List<String> fastqPaths = new ArrayList<>();
            for (ArchivedFastq fastq : fastqList) {
                fastqPaths.add(fastq.getFastq());
            }
            return fastqPaths;
        } catch (Exception e) {
            log.error("FASTQ Search error:" + e.getMessage());
            return null;
        }
    }

    public static class RequestSampleList {
        public String requestId;
        public String recipe;
        public String projectManagerName;
        public String piEmail;
        public String labHeadName, labHeadEmail;
        public String investigatorName, investigatorEmail;
        public String dataAnalystName, dataAnalystEmail;

        public List<RequestSample> samples;

        public List<String> pooledNormals;

        public RequestSampleList(){}

        public RequestSampleList(String requestId){ this.requestId = requestId; }

        public RequestSampleList(String requestId, List<RequestSample> samples) {
            this.requestId = requestId;
            this.samples = samples;
        }

        public String getRecipe() { return recipe; }
        public void setRecipe(String recipe) { this.recipe = recipe; }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public String getRequestId() {
            return requestId;
        }
        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public List<RequestSample> getSamples() {
            return samples;
        }
        public void setSamples(List<RequestSample> samples) {
            this.samples = samples;
        }
        public String getProjectManagerName() {
            return projectManagerName;
        }

        public String getPiEmail() {
            return piEmail;
        }

        public void setPiEmail(String piEmail) {
            this.piEmail = piEmail;
        }

        public void setProjectManagerName(String projectManagerName) {
            this.projectManagerName = projectManagerName;
        }

        public String getInvestigatorName() {
            return investigatorName;
        }

        public void setInvestigatorName(String investigatorName) {
            this.investigatorName = investigatorName;
        }

        public String getInvestigatorEmail() {
            return investigatorEmail;
        }

        public void setInvestigatorEmail(String investigatorEmail) {
            this.investigatorEmail = investigatorEmail;
        }

        public String getDataAnalystName() {
            return dataAnalystName;
        }

        public void setDataAnalystName(String dataAnalystName) {
            this.dataAnalystName = dataAnalystName;
        }

        public String getDataAnalystEmail() {
            return dataAnalystEmail;
        }

        public void setDataAnalystEmail(String dataAnalystEmail) {
            this.dataAnalystEmail = dataAnalystEmail;
        }
        public String getLabHeadName() {
            return labHeadName;
        }

        public void setLabHeadName(String labHeadName) {
            this.labHeadName = labHeadName;
        }

        public String getLabHeadEmail() {
            return labHeadEmail;
        }

        public void setLabHeadEmail(String labHeadEmail) {
            this.labHeadEmail = labHeadEmail;
        }
    }

    public static class RequestSample {
        private String investigatorSampleId;
        private String igoSampleId;
        private boolean IGOComplete;

        public RequestSample() {}

        public RequestSample(String investigatorSampleId, String igoSampleId, boolean IGOComplete) {
            this.investigatorSampleId = investigatorSampleId;
            this.igoSampleId = igoSampleId;
            this.IGOComplete = IGOComplete;
        }

        @Override
        public String toString() {
            return "RequestSample{" +
                    "investigatorSampleId='" + investigatorSampleId + '\'' +
                    ", igoSampleId='" + igoSampleId + '\'' +
                    ", IGOComplete=" + IGOComplete +
                    '}';
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public String getInvestigatorSampleId() {
            return investigatorSampleId;
        }
        public void setInvestigatorSampleId(String sampleName) {
            this.investigatorSampleId = investigatorSampleId;
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public String getIgoSampleId() {
            return igoSampleId;
        }
        public void setIgoSampleId(String igoSampleId) {
            this.igoSampleId = igoSampleId;
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public boolean isIGOComplete() {
            return IGOComplete;
        }
        public void setIGOComplete(boolean IGOComplete) {
            this.IGOComplete = IGOComplete;
        }
    }
}