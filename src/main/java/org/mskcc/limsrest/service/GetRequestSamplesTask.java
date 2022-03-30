package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.RequestModel;
import com.velox.sloan.cmo.recmodels.SampleModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.model.RequestSample;
import org.mskcc.limsrest.model.RequestSampleList;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
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

    public RequestSampleList execute() {
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
                else
                    recipe = sampleRecipe;

                String othersampleId = sample.getStringVal("OtherSampleId", user);
                boolean igoComplete = samplesIGOComplete.contains(othersampleId);
                // same othersampleId as other samples but these failed, could check exemplarSampleStatus too
                // remove if qc status lookup done by IGO ID
                if ("07078_E_1".equals(igoId) || "07078_E_2".equals(igoId) || "07078_E_5".equals(igoId))
                    igoComplete = false;

                RequestSample rs = new RequestSample(othersampleId, igoId, igoComplete);
                sampleList.add(rs);
            }

            // TODO REMOVE 06302_AO custom demux code
            if (requestId.equals("06302_AO")) {
                log.info("REPLACING LIMS SAMPLE LIST.");
                sampleList = samples06302_AO;
            }
            RequestSampleList rsl = new RequestSampleList(requestId, sampleList);

            if (isIMPACTOrHEMEPACT(recipe)) {
                log.info("Adding pooled normals for recipe: " + recipe);
                rsl.setPooledNormals(findPooledNormals(requestId));
            }
            String requestName = requestDataRecord.getStringVal(RequestModel.REQUEST_NAME, user);
            if (requestName != null && requestName.toUpperCase().contains("RNASEQ")) {
                setRNASeqLibraryTypeAndStrandedness(rsl, requestName);
            }

            // Recipe in LIMS is setup differently for the CMO-CH panel which is submitted as a CustomCapture with "CMO-CH" panel
            if ("CustomCapture".equals(recipe))  {
                // copy RUN-QC logic to get recipe from the last aliquot prior to pooling which is CMO-CH, for example project 12405_C
                List<DataRecord> qcRecords = drm.queryDataRecords("SeqAnalysisSampleQC", "Request = '" + this.requestId + "'", user);
                if (qcRecords.size() > 0) {  // SeqAnalysisSampleQC will only have records after the samples are sequenced
                    DataRecord parentSample = qcRecords.get(0).getParentsOfType("Sample", user).get(0);
                    recipe = parentSample.getStringVal(SampleModel.RECIPE, user);
                    log.info("Updated recipe from CustomCapture to " + recipe);
                }
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
            rsl.setOtherContactEmails(requestDataRecord.getStringVal("MailTo", user));
            rsl.setQcAccessEmails(requestDataRecord.getStringVal("QcAccessEmails", user));
            rsl.setDataAccessEmails(requestDataRecord.getStringVal("DataAccessEmails", user));
            Object deliveryDate = requestDataRecord.getValue("RecentDeliveryDate", user);
            if (deliveryDate != null)
                rsl.setDeliveryDate(requestDataRecord.getLongVal("RecentDeliveryDate", user));

            // GetRequestPermissionsTask will set fastq.gz permissions based on whether or not BIC or CMO request so
            // return those values here too.
            // alternatively, IGO could tell people to call the GetRequestPermissions endpoint instead of adding
            // the fields here too.
            Boolean isCmoRequest = Boolean.FALSE;
            Boolean bicAnalysis = Boolean.FALSE;
            String analysisType = "";
            try {
                isCmoRequest = requestDataRecord.getBooleanVal("IsCmoRequest", user);
                bicAnalysis = requestDataRecord.getBooleanVal("BICAnalysis", user);
                analysisType = requestDataRecord.getStringVal("AnalysisType", user);
            } catch (NullPointerException e) {
                log.warn("Correct invalid null valid in database for request: " + requestId);
            }
            Boolean isBicRequest = GetRequestPermissionsTask.isBicRequest(analysisType, bicAnalysis);
            rsl.setIsCmoRequest(isCmoRequest);
            rsl.setBicAnalysis(isBicRequest);

            return rsl;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    protected void setRNASeqLibraryTypeAndStrandedness(RequestSampleList rsl, String requestName) {
        // this classification by requestname will not be correct for all historical requests
        // customer has stated they already have all historical values and only need Nov. 2019-on to work

        // RNASeq-SMARTerAmp is non-stranded
        // RNASeq-TruSeqPolyA & RNASeq-TruSeqRiboDeplete is stranded-reverse
        // ignore RNAExtraction & RNA-QC request names
        String requestNameUpper = requestName.toUpperCase();
        if (requestNameUpper.contains("SMARTER"))
            rsl.setStrand("non-stranded");
        else // as of 2019 IGO has no "stranded-forward" kits.
            rsl.setStrand("stranded-reverse");

        // The LibraryType field was included in the original Pipelinekickoff codebase and BIC requested that
        // IGO add it to the endpoint in August 2020
        rsl.setLibraryType(requestName);
    }

    /**
     * Pooled normals were added to IMPACT requests up to IMPACT505 and used by the pipeline for
     * sample pairing when the patient's normal sample was not present.
     * @param recipe
     * @return
     */
    protected static boolean isIMPACTOrHEMEPACT(String recipe) {
        if (recipe.isEmpty())
            return false;
        if ("IMPACT341,IMPACT410,IMPACT410+,IMPACT468,IMPACT505,HemePACT_v3,HemePACT_v4".contains(recipe))
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

    // TODO REMOVE after fingerprinting
    private static ArrayList<RequestSample> samples06302_AO = new ArrayList<RequestSample>() {
        {
            add(new RequestSample("MSK-ML-0023-CF5-MSK13395A-P", "06302_AO_962", true));
            add(new RequestSample("MSK-ML-0007-CF5-MSK5003894D-P","06302_AO_966", true));
            add(new RequestSample("MSK-ML-0092-CF3-MSK5004862D-P","06302_AO_1007", true));
            add(new RequestSample("MSK-MB-0012-CF6-msk5001817d-p","06302_AO_135", true));
            add(new RequestSample("MSK-MB-0080-CF8-msk5005638d-p","06302_AO_182", true));
            add(new RequestSample("MSK-ML-0086-CF3-MSK5003804D-P","06302_AO_1028", true));
            add(new RequestSample("MSK-ML-0070-CF6-MSK5004852C-P","06302_AO_967", true));
            add(new RequestSample("MSK-ML-0091-CF3-MSK5001430D-P","06302_AO_968", true));
            add(new RequestSample("MSK-MB-0027-CF2-msk5000429c-p","06302_AO_134", true));
            add(new RequestSample("MSK-MB-0030-CF2-MSK14012a-p","06302_AO_158", true));
            add(new RequestSample("MSK-MB-0072-CF6-msk5005272c-p","06302_AO_176", true));
            add(new RequestSample("MSK-ML-0036-CF5-MSK5001255D-P","06302_AO_1036", true));
            add(new RequestSample("MSK-ML-0076-CF8-MSK5004712D-P","06302_AO_980", true));
            add(new RequestSample("MSK-ML-0006-CF10-MSK5003812C-P","06302_AO_1014", true));
            add(new RequestSample("MSK-MB-0045-CF1-MSK10461a-p","06302_AO_128", true));
            add(new RequestSample("MSK-MB-0008-CF3-MSK11534a-p","06302_AO_155", true));
            add(new RequestSample("MSK-MB-0087-CF6-msk5004058d-p","06302_AO_175", true));
            add(new RequestSample("MSK-ML-0036-CF5-MSK5001255C-P","06302_AO_1046", true));
            add(new RequestSample("MSK-ML-0008-CF8-MSK5005402D-P","06302_AO_982", true));
            add(new RequestSample("MSK-ML-0008-CF3-MSK5000441C-P","06302_AO_1004", true));
            add(new RequestSample("MSK-ML-0080-CF4-MSK5004863C-P","06302_AO_1010", true));
            add(new RequestSample("MSK-ML-0033-CF5-5000075D-P","06302_AO_1012", true));
            add(new RequestSample("MSK-MB-0034-CF7-msk5002820c-p","06302_AO_118", true));
            add(new RequestSample("MSK-MB-0088-CF6-msk5005643c-p","06302_AO_180", true));
            add(new RequestSample("MSK-ML-0008-CF6-MSK5003805C-P","06302_AO_1021", true));
            add(new RequestSample("MSK-ML-0013-CF4-MSK5003861C-P","06302_AO_1022", true));
            add(new RequestSample("MSK-ML-0033-CF6-MSK5001283D-P","06302_AO_986", true));
            add(new RequestSample("MSK-ML-0041-CF3-MSK13929A-P","06302_AO_988", true));
            add(new RequestSample("MSK-MB-0030-CF8-msk5005621d-p","06302_AO_109", true));
            add(new RequestSample("MSK-MB-0045-CF5-msk5001092d-p","06302_AO_185", true));
            add(new RequestSample("MSK-ML-0092-CF3-MSK5004862C-P","06302_AO_1041", true));
            add(new RequestSample("positivecontrol_100819","06302_AO_1056", true));
            add(new RequestSample("MSK-MB-0030-CF3-MSK14055a-p","06302_AO_123", true));
            add(new RequestSample("MSK-MB-0018-CF2-MSK12430a-p","06302_AO_149", true));
            add(new RequestSample("MSK-MB-0061-CF4-msk5001133c-p","06302_AO_156", true));
            add(new RequestSample("MSK-MB-0085-CF2-msk5001123c-p","06302_AO_167", true));
            add(new RequestSample("MSK-ML-0054-CF1-MSK12746A-P","06302_AO_1035", true));
            add(new RequestSample("MSK-ML-0061-CF3-MSK5001254D-P","06302_AO_1040", true));
            add(new RequestSample("MSK-MB-0085-CF3-msk5001143d-p","06302_AO_140", true));
            add(new RequestSample("MSK-MB-0051-CF6-msk5001144d-p","06302_AO_141", true));
            add(new RequestSample("MSK-MB-0099-CF1-msk5000961d-p","06302_AO_142", true));
            add(new RequestSample("MSK-MB-0085-CF2-msk5001123d-p","06302_AO_166", true));
            add(new RequestSample("MSK-MB-0104-CF8-msk5005634c-p","06302_AO_170", true));
            add(new RequestSample("MSK-ML-0062-CF1-MSK5003813D-P","06302_AO_1023", true));
        }
    };
}
