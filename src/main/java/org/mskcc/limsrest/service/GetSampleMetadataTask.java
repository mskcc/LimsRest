package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.controller.GetSampleMetadata;
import org.mskcc.limsrest.service.samplemetadata.SampleMetadata;

import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.mskcc.limsrest.util.Utils.isSequencingComplete;


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
                requests = dataRecordManager.queryDataRecords("Request", "CompletedDate > '" + timestamp + "'", user);
                //requests = dataRecordManager.queryDataRecords("Request", "RequestId = '93017_V'", user);//'" + timestamp +"'", user);//for testing requests.size()); AND Status IN ('Completed', 'Completed with Failures')
                log.info("Total Requests: " + requests.size());
                for (DataRecord req : requests) {
                    String requestId = req.getStringVal("RequestId", user);
                    log.info("Request ID: " + requestId);
                    String labHead = (String) getValueFromDataRecord(req, "LaboratoryHead", "String");
                    DataRecord[] samples = req.getChildrenOfType("Sample", user);
                    log.info(String.format("Number of samples  in request %s: %d", requestId, samples.length));
                    for (DataRecord sample : samples) {
                        baitSet = ""; // set baitset to empty before the search for each sample begins.
                        DataRecord cmoInfoRec = getRelatedCmoInfoRec(sample);
                        String mrn = getRandomValue();
                        String cmoPatientId = (String) getFieldValueForSample(sample, cmoInfoRec, "CorrectedInvestPatientId", "PatientId", "String");
                        String cmoSampleId = (String) getFieldValueForSample(sample, cmoInfoRec, "CorrectedCMOID", "OtherSampleId", "String");
                        log.info(cmoSampleId);
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
                        log.info("parent tumor type: " + parentTumorType);
                        String specimenType = (String) getFieldValueForSample(sample, cmoInfoRec, "SpecimenType", "SpecimenType", "String");
                        String sampleOrigin = (String) getFieldValueForSample(sample, cmoInfoRec, "SampleOrigin", "SampleOrigin", "String");
                        String tissueSource = (String) getFieldValueForSample(sample, cmoInfoRec, "TissueSource", "TissueSource", "String");
                        String tissueLocation = (String) getFieldValueForSample(sample, cmoInfoRec, "TissueLocation", "TissueLocation", "String");
                        String recipe = (String) getFieldValueForSample(sample, cmoInfoRec, "Recipe", "Recipe", "String");
                        String baitset = baitSet;
                        log.info("baitset: " + baitset);
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
        return "";
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
                fieldValue = getValueFromDataRecord(cmoInfoRecord, cmoInfoFieldName, fieldType);
                log.info(fieldValue.toString());
                if (fieldValue != "") {
                    return fieldValue;
                }
            }
            return getValueFromDataRecord(sample, sampleFieldName, fieldType);
        } catch (Exception e) {
            log.error(String.format("Error getting '%s' value for sample '%s' from related samples or SampleCMOInfoRecords", sampleFieldName, sampleId));
        }
        return "";
    }


    /**
     * Method to get Main Tumor Type using Tumor Type Name eg: Breast Cancer or Pancreatic cancer etc.
     *
     * @param url
     * @param tumorType
     * @return String
     */
    private String getOncotreeTumorTypeUsingName(URL url, String tumorType) {
        HttpURLConnection con = null;
        StringBuffer response = new StringBuffer();
        JSONArray oncotreeResponseData = null;
        try {
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            con.disconnect();
            oncotreeResponseData = new JSONArray(response.toString());
            if (oncotreeResponseData.length() > 0) {
                JSONObject jsonObject = oncotreeResponseData.getJSONObject(0);
                Object mainType = jsonObject.get("mainType");
                return mainType != null ? mainType.toString() : "";
            }
        } catch (Exception e) {
            log.info(String.format("Error while querying oncotree api for name search. Will attempt to search using oncotree api for code search:\n%s",e.getMessage()));
            return "";
        }
        return "";
    }

    /**
     * Method to get Main Tumor Type using TumorType CODE or abbreviation eg: BRCA for Breast Cancer and PAAD for
     * Pancreatic cancer etc.
     *
     * @param url
     * @param tumorType
     * @return String
     */
    private String getOncotreeTumorTypeUsingCode(URL url, String tumorType) {
        HttpURLConnection con = null;
        StringBuffer response = new StringBuffer();
        JSONArray oncotreeResponseData = null;
        try {
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            con.disconnect();
            oncotreeResponseData = new JSONArray(response.toString());
            if (oncotreeResponseData.length() > 0) {
                for (Object rec : oncotreeResponseData) {
                    Object code = ((JSONObject) rec).get("code");
                    if (code != null && tumorType.toLowerCase().equals(code.toString().trim().toLowerCase())) {
                        Object mainType = ((JSONObject) rec).get("mainType");
                        return mainType != null ? mainType.toString() : "";
                    }
                }
            }
        } catch (Exception e) {
            log.info(String.format("Error while querying oncotree api using code search. Cannot find Main tumor type.\n%s",e.getMessage()));
            return "";
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
            // In LIMS tumor types entry is not controlled. Sometimes tumor type as tumor name is entered and other times tumor type code is entered.
            // First query oncotree using api for name search
            URL url = new URL("http://oncotree.mskcc.org/api/tumorTypes/search/name/" + tumorType.split("/")[0].replace(" ", "%20") + "?exactMatch=false");
            mainTumorType = getOncotreeTumorTypeUsingName(url, tumorType);
            // If name search returns nothing, then query oncotree using api for code search
            if (StringUtils.isBlank(mainTumorType)) {
                URL url2 = new URL("http://oncotree.mskcc.org/api/tumorTypes/search/code/" + tumorType.split("/")[0].replace(" ", "%20") + "?exactMatch=true");
                mainTumorType = getOncotreeTumorTypeUsingCode(url2, tumorType);
            }
        } catch (Exception e) {
            log.error(String.format("Error occured while querying oncotree end point for Tumor Type %s\n%s", tumorType, e.getMessage()));
            return "";
        }
        return mainTumorType;
    }

    /**
     * Method to get origin Sample ID for a sample.
     *
     * @param sample
     * @return boolean
     */
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

    /**
     * Method to get Sample status.
     *
     * @param sample
     * @return boolean
     */
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
                if (isSequencingComplete(current, user)) {
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

    /**
     * Method to check if the sample status is a complete status of a workflow/process.
     *
     * @param status
     * @return boolean
     */
    private boolean isCompleteStatus(String status) {
        return status.toLowerCase().contains("completed") || status.toLowerCase().contains("failed");
    }

    /**
     * Method to check if status indicates sequencing complete.
     *
     * @param status
     * @return boolean
     */
    private boolean isSequencingCompleteStatus(String status) {
        status = status.toLowerCase();
        return status.contains("completed - ") && status.contains("illumina") && status.contains("sequencing");
    }

    /**
     * Method to resolve status to one of the main complete/failed statuses.
     *
     * @param status
     * @param sampleType
     * @return String
     */
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
        if (status.toLowerCase().contains("failed")) {
            return status;
        }
        return "";
    }
}
