package org.mskcc.limsrest.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 *
 */
public class GetRequestSamplesTask extends LimsTask {
    private static Log log = LogFactory.getLog(GetRequestSamplesTask.class);

    protected String requestId;
    protected boolean tumorOnly;

    public void init(String requestId, boolean tumorOnly) {
        this.requestId = requestId;
        this.tumorOnly = tumorOnly;
    }

    @Override
    public Object execute(VeloxConnection conn) {
        try {
            List<DataRecord> requestList = dataRecordManager.queryDataRecords("Request", "RequestId = '" + this.requestId + "'", this.user);
            if (requestList.size() != 1) {  // error: request ID not found or more than one found
                log.error("Request not found:" + requestId);
                return new RequestSampleList("NOT_FOUND");
            }

            // get set of all samples in that request that are "IGO Complete"
            HashSet<String> samplesIGOComplete = getSamplesIGOComplete(requestId);
            log.info("Samples IGO Complete: " + samplesIGOComplete.size());

            DataRecord requestDataRecord = requestList.get(0);
            DataRecord[] samples = requestDataRecord.getChildrenOfType("Sample", user);
            log.info("Child samples found: " + samples.length);

            List<RequestSample> sampleList = new ArrayList<>();
            for (DataRecord sample : samples) {
                String igoId = sample.getStringVal("SampleId", user);
                String othersampleId = sample.getStringVal("OtherSampleId", user);
                boolean igoComplete = samplesIGOComplete.contains(othersampleId);
                boolean tumor = "Tumor".equals(sample.getStringVal("TumorOrNormal", user));

                if (tumorOnly && tumor) {
                    RequestSample rs = new RequestSample(othersampleId, igoId, igoComplete);
                    sampleList.add(rs);
                } else if (!tumorOnly){
                    RequestSample rs = new RequestSample(othersampleId, igoId, igoComplete);
                    sampleList.add(rs);
                }
            }

            RequestSampleList rsl = new RequestSampleList(requestId, sampleList);
            // "PIemail"
            rsl.setLabHeadName(requestDataRecord.getStringVal("LaboratoryHead", user));
            rsl.setLabHeadEmail(requestDataRecord.getStringVal("LabHeadEmail", user));
            rsl.setProjectManagerName(requestDataRecord.getStringVal("ProjectManager", user));

            rsl.setInvestigatorName(requestDataRecord.getStringVal("Investigator", user));
            rsl.setInvestigatorEmail(requestDataRecord.getStringVal("Investigatoremail", user));
            rsl.setDataAnalystName(requestDataRecord.getStringVal("DataAnalyst", user));
            rsl.setDataAnalystEmail(requestDataRecord.getStringVal("DataAnalystEmail", user));
            log.info("Result size: " + sampleList.size() + " tumors only:" + tumorOnly);
            return rsl;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Returns set of all samples IGO Complete by seqanalysissampleqc.othersampleid
     * @param requestId
     * @return
     * @throws Exception
     */
    protected HashSet<String> getSamplesIGOComplete(String requestId) throws Exception {
        String whereClause = "PassedQC = 1 AND SeqQCStatus = 'Passed' AND Request = '" + requestId + "'";
        List<DataRecord> listIGOComplete = dataRecordManager.queryDataRecords("SeqAnalysisSampleQC", whereClause, user);
        HashSet<String> samplesIGOComplete = new HashSet<>();
        for (DataRecord r : listIGOComplete) {
            samplesIGOComplete.add(r.getStringVal("OtherSampleId", user));
        }
        return samplesIGOComplete;
    }


    public static class RequestSampleList {
        public String requestId;
        public String projectManagerName;
        public String labHeadName, labHeadEmail;
        public String investigatorName, investigatorEmail;
        public String dataAnalystName, dataAnalystEmail;

        public List<RequestSample> samples;

        public RequestSampleList(){}
        public RequestSampleList(String requestId){ this.requestId = requestId; }

        public RequestSampleList(String requestId, List<RequestSample> samples) {
            this.requestId = requestId;
            this.samples = samples;
        }

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