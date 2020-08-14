package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.controller.GetSampleMetadata;
import org.mskcc.limsrest.service.samplemetadata.SampleMetadata;

import java.util.*;
import static org.mskcc.limsrest.util.Utils.*;


public class GetSampleMetadataTask {
    private Log log = LogFactory.getLog(GetSampleMetadata.class);
    private String timestamp;
    private String projectId;
    private ConnectionLIMS conn;
    private User user;
    private String baitSet = "";

    public GetSampleMetadataTask(String timestamp, String projectId, ConnectionLIMS conn) {
        this.timestamp = timestamp;
        this.projectId = projectId;
        this.conn = conn;
    }

    public List<SampleMetadata> execute() {
        long start = System.currentTimeMillis();
        List<SampleMetadata> sampleMetadata = new ArrayList<>();
        try {
            VeloxConnection vConn = conn.getConnection();
            user = vConn.getUser();
            DataRecordManager dataRecordManager = vConn.getDataRecordManager();
            log.info(timestamp);
            log.info(" Starting GetSampleMetadata task using timestamp: " + timestamp + " and projectId: " + projectId);
            List<DataRecord> requests;
            log.info("projectid is blank: " + StringUtils.isBlank(projectId));
            try {
                if (StringUtils.isBlank(projectId)){
                    requests = dataRecordManager.queryDataRecords("Request", "DateCreated > " + timestamp + " AND IsCmoRequest = 1", user);
                    //requests = dataRecordManager.queryDataRecords("Request", "RequestId= '05457_F' AND IsCmoRequest = 1", user);
                }
                else {
                    String likeParam = projectId.split("_")[0] + "_%";
                    requests = dataRecordManager.queryDataRecords("Request", "RequestId= '" + projectId + "' OR RequestId LIKE '" + likeParam + "' AND IsCmoRequest = 1", user);
                    //requests = dataRecordManager.queryDataRecords("Request", "RequestId= '06345_B' AND IsCmoRequest = 1", user);
                }
                log.info("Total Requests: " + requests.size());
                for (DataRecord req : requests) {
                    String requestId = req.getStringVal("RequestId", user);
                    log.info("Request ID: " + requestId);
                    String labHead = (String) getValueFromDataRecord(req, "LaboratoryHead", "String", user);
                    String recipe = (String) getValueFromDataRecord(req, "RequestName", "String", user);
                    DataRecord[] samples = req.getChildrenOfType("Sample", user);
                    log.info(String.format("Number of samples  in request %s: %d", requestId, samples.length));
                    for (DataRecord sample : samples) {
                        baitSet = ""; // set baitset to empty before the search for each sample begins.
                        DataRecord cmoInfoRec = getRelatedCmoInfoRec(sample);
                        String mrn = getRandomValue();
                        String cmoPatientId = (String) getFieldValueForSample(sample, cmoInfoRec, "CmoPatientId", "PatientId", "String");
                        String cmoSampleId = (String) getFieldValueForSample(sample, cmoInfoRec, "CorrectedCMOID", "OtherSampleId", "String");
                        log.info("CMO Sample ID: " + cmoSampleId);
                        String igoId = sample.getStringVal("SampleId", user);
                        String investigatorSampleId = (String) getFieldValueForSample(sample, cmoInfoRec, "UserSampleID", "UserSampleID", "String");
                        String species = (String) getFieldValueForSample(sample, cmoInfoRec, "Species", "Species", "String");
                        String sex = (String) getFieldValueForSample(sample, cmoInfoRec, "Gender", "Gender", "String");
                        String tumorOrNormal = (String) getFieldValueForSample(sample, cmoInfoRec, "TumorOrNormal", "TumorOrNormal", "String");
                        String sampleType = (String) getValueFromDataRecord(sample, "ExemplarSampleType", "String", user);
                        String preservation = (String) getFieldValueForSample(sample, cmoInfoRec, "Preservation", "Preservation", "String");
                        String tumorType = (String) getFieldValueForSample(sample, cmoInfoRec, "TumorType", "TumorType", "String");
                        String parentTumorType = "";
                        if (!StringUtils.isBlank(tumorType) && !StringUtils.isBlank(tumorOrNormal) && tumorOrNormal.toLowerCase().equals("tumor")) {
                            parentTumorType = getOncotreeTumorType(tumorType);
                        }
                        log.info("parent tumor type: " + parentTumorType);
                        String specimenType = (String) getFieldValueForSample(sample, cmoInfoRec, "SpecimenType", "SpecimenType", "String");
                        String sampleOrigin = (String) getFieldValueForSample(sample, cmoInfoRec, "SampleOrigin", "SampleOrigin", "String");
                        String tissueSource = (String) getFieldValueForSample(sample, cmoInfoRec, "TissueSource", "TissueSource", "String");
                        String tissueLocation = (String) getFieldValueForSample(sample, cmoInfoRec, "TissueLocation", "TissueLocation", "String");
                        String baitset = getBaitSet(sample, requestId, user);;
                        log.info("baitset: " + baitset);
                        String fastqPath = "";
                        String ancestorSample = getOriginSampleId(sample, user);
                        boolean doNotUse = false;
                        String sampleStatus = getMostAdvancedLimsStage(sample, requestId, this.conn);
                        SampleMetadata metadata = new SampleMetadata(mrn, cmoPatientId, cmoSampleId, igoId, investigatorSampleId, species,
                                sex, tumorOrNormal, sampleType, preservation, tumorType, parentTumorType,
                                specimenType, sampleOrigin, tissueSource, tissueLocation, recipe,
                                baitset, fastqPath, labHead, ancestorSample, doNotUse, sampleStatus);
                        sampleMetadata.add(metadata);
                        log.info("Done building object.");
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        log.info("total time: " + (System.currentTimeMillis() - start));
        return sampleMetadata;
    }

    /**
     * Method to temporarily create random mrn for samples. This will be replaced with actual code to return valid MRN's
     *
     * @return String
     */
    private String getRandomValue() {
        Random r = new Random();
        char c = Character.toUpperCase((char) (r.nextInt(26) + 'a'));
        long number = (long) Math.floor(Math.random() * 9_000_000_0L) + 1_000_000_0L;
        return String.valueOf(number + c);
    }

    /**
     * Method to get related SampleCMOInfoRecords for sample.
     *
     * @param sample
     * @return
     */
    private DataRecord getRelatedCmoInfoRec(DataRecord sample) {
        String sampleId = "";
        try {
            sampleId = sample.getStringVal("SampleId", user);
            if (sample.getChildrenOfType("SampleCMOInfoRecords", user).length > 0) {
                System.out.println("ended get cmo info rec");
                return sample.getChildrenOfType("SampleCMOInfoRecords", user)[0];
            }
            Stack<DataRecord> sampleStack = new Stack<>();
            if (sample.getParentsOfType("Sample", user).size() > 0) {
                sampleStack.addAll((sample.getParentsOfType("Sample", user)));
            }
            do {
                DataRecord startSample = sampleStack.pop();
                if (startSample.getChildrenOfType("SampleCMOInfoRecords", user).length > 0) {
                    System.out.println("ended get cmo info rec");
                    return startSample.getChildrenOfType("SampleCMOInfoRecords", user)[0];
                }
                if (startSample.getParentsOfType("Sample", user).size() > 0) {
                    sampleStack.addAll(startSample.getParentsOfType("Sample", user));
                }
            } while (!sampleStack.isEmpty());
        } catch (Exception e) {
            log.error(String.format("Error occured while finding related SampleCMOInfoRecords for Sample %s", sampleId));
            return null;
        }
        return null;
    }

    /**
     * Method to get value for a field from SampleCMOInfoRecords if present or get fallback value from Parent Sample record.
     *
     * @param sample
     * @param cmoInfoRecord
     * @param cmoInfoFieldName
     * @param sampleFieldName
     * @param fieldType
     * @return
     */
    private Object getFieldValueForSample(DataRecord sample, DataRecord cmoInfoRecord, String cmoInfoFieldName, String sampleFieldName, String fieldType) {
        String sampleId = "";
        try {
            sampleId = sample.getStringVal("SampleId", user);
            Object fieldValue;
            if (cmoInfoRecord != null) {
                fieldValue = getValueFromDataRecord(cmoInfoRecord, cmoInfoFieldName, fieldType, user);
                log.info(String.format("%s : %s",cmoInfoFieldName, fieldValue.toString()));
                if (fieldValue != "") {
                    return fieldValue;
                }
            }
            return getValueFromDataRecord(sample, sampleFieldName, fieldType, user);
        } catch (Exception e) {
            log.error(String.format("Error getting '%s' value for sample '%s' from related samples or SampleCMOInfoRecords", sampleFieldName, sampleId));
        }
        return "";
    }
}
