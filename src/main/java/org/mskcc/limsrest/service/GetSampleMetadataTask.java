package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.controller.GetSampleMetadata;
import org.mskcc.limsrest.service.samplemetadata.SampleMetadata;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

public class GetSampleMetadataTask {
    private final List<String> NUCLEIC_ACID_TYPES = Arrays.asList("dna", "rna", "cfdna", "amplicon", "cdna");
    private final List<String> LIBRARY_SAMPLE_TYPES = Arrays.asList("dna library", "cdna library", "pooled library");
    private final List<String> VALID_SAMPLETYPES = Arrays.asList("dna", "rna", "cdna", "cfdna", "amplicon", "dna library", "cdna library", "pooled library");
    private final List<String> SAMPLETYPES_IN_ORDER = Arrays.asList("dna", "rna", "cdna", "amplicon", "dna library", "cdnalibrary", "pooled library");
    private Log log = LogFactory.getLog(GetSampleMetadata.class);
    private String timestamp;
    private ConnectionLIMS conn;
    private User user;
    private String baitSet = "";

    public GetSampleMetadataTask(String timestamp, ConnectionLIMS conn) {
        this.timestamp = timestamp;
        this.conn = conn;
    }

    public List<SampleMetadata> execute() {
        long start = System.currentTimeMillis();
        List<SampleMetadata> sampleMetadata = new ArrayList<>();
        try {
            VeloxConnection vConn = conn.getConnection();
            user = vConn.getUser();
            DataRecordManager dataRecordManager = vConn.getDataRecordManager();
            log.info(" Starting GetSampleMetadata task using timestamp " + timestamp);
            List<DataRecord> requests = new ArrayList<>();
            try {
                requests = dataRecordManager.queryDataRecords("Request", "CompletedDate > '" + timestamp + "' AND Status IN ('Completed', 'Completed with Failures')", user);
                //requests = dataRecordManager.queryDataRecords("Request", "RequestId = '07973'", user);//'" + timestamp +"'", user);//for testing requests.size());
                log.info("Total Requests: " + requests.size());
                for (DataRecord req : requests) {
                    String requestId = req.getStringVal("RequestId", user);
                    log.info("Request ID: " + requestId);
                    String labHead = (String) getValueFromDataRecord(req, "LaboratoryHead", "String");
                    DataRecord[] samples = req.getChildrenOfType("Sample", user);
                    log.info(String.format("Number of samples  in request %s: %d", requestId, samples.length));
                    for (DataRecord sample : samples) {
                        DataRecord cmoInfoRec = getRelatedCmoInfoRec(sample);
                        String mrn = getRandomValue();
                        String cmoPatientId = (String) getFieldValueForSample(sample, cmoInfoRec, "CorrectedInvestPatientId", "PatientId", "String");
                        String cmoSampleId = (String) getFieldValueForSample(sample, cmoInfoRec, "CorrectedCMOID", "OtherSampleId", "String");
                        String igoId = sample.getStringVal("SampleId", user);
                        String investigatorSampleId = (String) getFieldValueForSample(sample, cmoInfoRec, "UserSampleID", "UserSampleID", "String");
                        String species = (String) getFieldValueForSample(sample, cmoInfoRec, "Species", "Species", "String");
                        String sex = (String) getFieldValueForSample(sample, cmoInfoRec, "Gender", "Gender", "String");
                        String tumorOrNormal = (String) getFieldValueForSample(sample, cmoInfoRec, "TumorOrNormal", "TumorOrNormal", "String");
                        String sampleType = (String) getValueFromDataRecord(sample, "ExemplarSampleType", "String");
                        String preservation = (String) getFieldValueForSample(sample, cmoInfoRec, "Preservation", "Preservation", "String");
                        String tumorType = (String) getFieldValueForSample(sample, cmoInfoRec, "TumorType", "TumorType", "String");
                        String parentTumorType = "";
                        if (!StringUtils.isBlank(tumorType) && !StringUtils.isBlank(tumorOrNormal) && tumorOrNormal.toLowerCase().equals("tumor")) {
                            parentTumorType = getOncotreeTumorType(tumorType);
                        }
                        String specimenType = (String) getFieldValueForSample(sample, cmoInfoRec, "SpecimenType", "SpecimenType", "String");
                        String sampleOrigin = (String) getFieldValueForSample(sample, cmoInfoRec, "SampleOrigin", "SampleOrigin", "String");
                        String tissueSource = (String) getFieldValueForSample(sample, cmoInfoRec, "TissueSource", "TissueSource", "String");
                        String tissueLocation = (String) getFieldValueForSample(sample, cmoInfoRec, "TissueLocation", "TissueLocation", "String");
                        String recipe = (String) getFieldValueForSample(sample, cmoInfoRec, "Recipe", "Recipe", "String");
                        String baitset = baitSet;
                        String fastqPath = "";
                        String ancestorSample = getOriginSampleId(sample);
                        boolean doNotUse = false;
                        String sampleStatus = getSampleStatus(sample);

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
     * Get a DataField value from a DataRecord.
     *
     * @param record
     * @param fieldName
     * @param fieldType
     * @return Object
     * @throws NotFound
     * @throws RemoteException
     */
    private Object getValueFromDataRecord(DataRecord record, String fieldName, String fieldType) throws NotFound, RemoteException {
        if (record == null) {
            return "";
        }
        if (record.getValue(fieldName, user) != null) {
            if (fieldType.equals("String")) {
                return record.getStringVal(fieldName, user);
            }
            if (fieldType.equals("Integer")) {
                return record.getIntegerVal(fieldName, user);
            }
            if (fieldType.equals("Long")) {
                return record.getLongVal(fieldName, user);
            }
            if (fieldType.equals("Double")) {
                return record.getDoubleVal(fieldName, user);
            }
            if (fieldType.equals("Date")) {
                SimpleDateFormat dateFormatter = new SimpleDateFormat("MM-dd-yyyy");
                return dateFormatter.format(new Date(record.getDateVal(fieldName, user)));
            }
        }
        return "None";
    }

    private DataRecord getRelatedCmoInfoRec(DataRecord sample){
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

    private Object getFieldValueForSample(DataRecord sample, DataRecord cmoInfoRecord, String cmoInfoFieldName, String sampleFieldName, String fieldType) {
        String sampleId = "";
        try {
            sampleId = sample.getStringVal("SampleId", user);
            Object fieldValue = null;
            if (cmoInfoRecord != null) {
                fieldValue = getValueFromDataRecord(cmoInfoRecord, cmoInfoFieldName, fieldType);
                if (fieldType != null || fieldValue != "") {
                    return getValueFromDataRecord(sample, sampleFieldName, fieldType);
                }
            }
            return getValueFromDataRecord(sample, sampleFieldName, fieldType);
        } catch (Exception e) {
            log.error(String.format("Error getting '%s' value for sample '%s' from related samples or SampleCMOInfoRecords", sampleFieldName, sampleId));
        }
        return "";
    }

    /**
     * Get MainCancerType from oncotree
     *
     * @param tumorType
     * @return String
     */
    private String getOncotreeTumorType(String tumorType) {
        StringBuffer response = new StringBuffer();
        JSONArray oncotreeResponseData = null;
        String mainTumorType = "";
        try {
            URL url = new URL("http://oncotree.mskcc.org/api/tumorTypes/search/name/" + tumorType.split("/")[0].replace(" ", "%20") + "?exactMatch=false");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            con.disconnect();
            oncotreeResponseData = new JSONArray(response.toString());
        } catch (Exception e) {
            log.error(String.format("Error occured while querying oncotree end point for Tumor Type %s\n%s", tumorType, Arrays.toString(e.getStackTrace())));
            return "";
        }
        if (oncotreeResponseData.length() > 0) {
            try {
                JSONObject rec = oncotreeResponseData.getJSONObject(0);
                mainTumorType = rec.getString("mainType");
                log.info(mainTumorType);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            mainTumorType = "Oncotree Tumor Type not found.";
        }
        return mainTumorType;
    }

    private String getOriginSampleId(DataRecord sample) {
        String sampleId = "";
        try {
            sampleId = sample.getStringVal("SampleId", user);
            if (sample.getChildrenOfType("SampleCMOInfoRecords", user).length > 0) {
                return sample.getStringVal("SampleId", user);
            }
            Stack<DataRecord> sampleStack = new Stack<>();
            if (sample.getParentsOfType("Sample", user).size() > 0) {
                sampleStack.addAll((sample.getParentsOfType("Sample", user)));
            }
            do {
                DataRecord startSample = sampleStack.pop();
                if (startSample.getChildrenOfType("SampleCMOInfoRecords", user).length > 0) {
                    return sample.getStringVal("SampleId", user);
                }
                if (startSample.getParentsOfType("Sample", user).size() > 0) {
                    sampleStack.addAll(startSample.getParentsOfType("Sample", user));
                }

            } while (!sampleStack.isEmpty());
        } catch (Exception e) {
            log.error(String.format("Error occured while finding related SampleCMOInfoRecords for Sample %s", sampleId));
        }
        return sampleId;
    }

    private String getSampleStatus(DataRecord sample) {
        String requestId;
        String sampleId = "";
        String sampleStatus;
        String currentSampleType = "";
        String currentSampleStatus = "";
        try {
            requestId = (String) getValueFromDataRecord(sample, "RequestId", "String");
            sampleId = sample.getStringVal("SampleId", user);
            sampleStatus = (String) getValueFromDataRecord(sample, "ExemplarSampleStatus", "String");
            int statusOrder = -1;
            long recordId = 0;
            Stack<DataRecord> sampleStack = new Stack<>();
            sampleStack.add(sample);
            do {
                DataRecord current = sampleStack.pop();
                currentSampleType = (String) getValueFromDataRecord(current, "ExemplarSampleType", "String");
                currentSampleStatus = (String) getValueFromDataRecord(current, "ExemplarSampleStatus", "String");
                int currentStatusOrder = SAMPLETYPES_IN_ORDER.indexOf(currentSampleType.toLowerCase());
                long currentRecordId = current.getRecordId();
                if (isSequencingComplete(current)) {
                    return "Completed Sequencing";
                }
                if (currentRecordId > recordId && currentStatusOrder > statusOrder && isCompleteStatus(currentSampleStatus)) {
                    sampleStatus = currentSampleStatus;
                    statusOrder = currentStatusOrder;
                }
                DataRecord[] childSamples = current.getChildrenOfType("Sample", user);
                for (DataRecord sam : childSamples) {
                    String childRequestId = sam.getStringVal("RequestId", user);
                    if (requestId.equalsIgnoreCase(childRequestId)) {
                        sampleStack.push(sam);
                    }
                }
            } while (sampleStack.size() > 0);
        } catch (Exception e) {
            log.error(String.format("Error while getting status for sample '%s'.", sampleId));
            return "";
        }
        return resolveCurrentStatus(sampleStatus, currentSampleType);
    }

    private boolean isCompleteStatus(String status) {
        return status.toLowerCase().contains("completed");
    }

    private boolean isSequencingCompleteStatus(String status) {
        status = status.toLowerCase();
        return status.contains("completed - ") && status.contains("illumina") && status.contains("sequencing");
    }

    private String resolveCurrentStatus(String status, String sampleType) {
        if (NUCLEIC_ACID_TYPES.contains(sampleType.toLowerCase()) && status.toLowerCase().contains("completed -") && status.toLowerCase().contains("extraction") && status.toLowerCase().contains("dna/rna simultaneous")) {
            return String.format("Completed - %s Extraction", sampleType.toUpperCase());
        }
        if (NUCLEIC_ACID_TYPES.contains(sampleType.toLowerCase()) && status.toLowerCase().contains("completed -") && status.toLowerCase().contains("extraction") && status.toLowerCase().contains("rna")) {
            return "Completed - RNA Extraction";
        }
        if (NUCLEIC_ACID_TYPES.contains(sampleType.toLowerCase()) && status.toLowerCase().contains("completed -") && status.toLowerCase().contains("extraction") && status.toLowerCase().contains("dna")) {
            return "Completed - DNA Extraction";
        }
        if (NUCLEIC_ACID_TYPES.contains(sampleType.toLowerCase()) && status.toLowerCase().contains("completed -") && status.toLowerCase().contains("quality control")) {
            return "Completed - Quality Control";
        }
        if (LIBRARY_SAMPLE_TYPES.contains(sampleType.toLowerCase()) && status.toLowerCase().contains("completed") && status.toLowerCase().contains("library preparation")) {
            return "Completed - Library Preparaton";
        }
        if (LIBRARY_SAMPLE_TYPES.contains(sampleType.toLowerCase()) && isSequencingCompleteStatus(status)) {
            return "Completed - Sequencing";
        }
        return "";
    }

    private Boolean isSequencingComplete(DataRecord sample) {
        try {
            baitSet = "";
            List<DataRecord> seqAnalysisRecords = Arrays.asList(sample.getChildrenOfType("SeqAnalysisSampleQC", user));
            if (seqAnalysisRecords.size() > 0) {
                Object sequencingStatus = seqAnalysisRecords.get(0).getValue("SeqQCStatus", user);
                baitSet = (String) (getValueFromDataRecord(seqAnalysisRecords.get(0), "BaitSet", "String"));
                if (sequencingStatus != null && (sequencingStatus.toString().equalsIgnoreCase("passed") || sequencingStatus.toString().equalsIgnoreCase("failed"))) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
        return false;
    }
}
