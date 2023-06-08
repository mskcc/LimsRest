package org.mskcc.limsrest.util;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sloan.cmo.recmodels.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mskcc.domain.sample.NucleicAcid;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.assignedprocess.QcStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static org.mskcc.limsrest.util.StatusTrackerConfig.*;

public class Utils {
    private final static Log LOGGER = LogFactory.getLog(Utils.class);
    private final static List<String> TISSUE_SAMPLE_TYPES = Arrays.asList("cells", "plasma", "blood", "tissue", "buffy coat", "blocks/slides", "ffpe sample", "other", "tissue sample");
    private final static List<String> NUCLEIC_ACID_TYPES = Arrays.asList("dna", "rna", "cdna", "cfdna", "dna,cfdna", "amplicon", "pre-qc rna");
    private final static List<String> LIBRARY_SAMPLE_TYPES = Arrays.asList("dna library", "cdna library", "gdna library");
    private final static List<String> CAPTURE_SAMPLE_TYPES = Collections.singletonList("capture library");
    private final static List<String> POOLED_SAMPLE_TYPES = Collections.singletonList("pooled library");
    private final static String FAILED_STATUS_TEXT = "failed";
    private final static String IGO_ID_WITHOUT_ALPHABETS_PATTERN = "^[0-9]+_[0-9]+.*$";  // sample id without alphabets
    private final static String IGO_ID_WITH_ALPHABETS_PATTERN = "^[0-9]+_[A-Z]+_[0-9]+.*$";  // sample id without alphabets

    public static void runAndCatchNpe(Runnable runnable) {
        try {
            runnable.run();
        } catch (NullPointerException var2) {
        }
    }

    public static String requireNonNullNorEmpty(String string, String message) {
        if (string != null && !"".equals(string)) {
            return string;
        } else {
            throw new RuntimeException(message);
        }
    }

    public static Optional<NucleicAcid> getOptionalNucleicAcid(String nucleicAcid, String sampleId) {
        try {
            return Optional.of(NucleicAcid.fromValue(nucleicAcid));
        } catch (Exception e) {
            LOGGER.warn(String.format("Nucleic acid for sample %s is empty. For some sample types cmo sample " +
                    "id won't be able to be generated", sampleId));

            return Optional.empty();
        }
    }

    public static <T> ResponseEntity<T> getResponseEntity(T input, HttpStatus status) {
        ResponseEntity<T> resp = new ResponseEntity<T>(input, status);
        return resp;
    }

    /**
     * Method to check if Sequencing for a sample is Passed Complete based on presence of SeqAnalysisSampleQC as child record
     * and status of SeqAnalysisSampleQC as Passed on any record and IgoComplete set to True.
     * @param qcRecords
     * @param user
     * @return
     */
    private static boolean isSequencingPassedComplete(List<DataRecord>qcRecords, User user){
        try{
            for (DataRecord rec: qcRecords){
                Object qcStatus = rec.getValue(SeqAnalysisSampleQCModel.SEQ_QCSTATUS, user);
                if (qcStatus != null && qcStatus.toString().equalsIgnoreCase(QcStatus.PASSED.toString()) && isQcStatusIgoComplete(rec, user)) {
                    return true;
                }
            }
        }catch (Exception e){
            String msg = String.format("%s -> Error while checking Sequencing QC status equals 'Passed'", ExceptionUtils.getRootCause(e), ExceptionUtils.getStackTrace(e));
            LOGGER.error(msg);
        }
        return false;
    }

    /**
     * Method to check if Sequencing for a sample is Failed Complete based on presence of SeqAnalysisSampleQC as child record
     * and status of SeqAnalysisSampleQC as Failed on any record and IgoComplete set to True.
     * @param qcRecords
     * @param user
     * @return
     */
    private static boolean isSequencingFailedComplete(List<DataRecord>qcRecords, User user){
        try{
            for (DataRecord rec: qcRecords){
                Object qcStatus = rec.getValue(SeqAnalysisSampleQCModel.SEQ_QCSTATUS, user);
                if (qcStatus != null && qcStatus.toString().equalsIgnoreCase(QcStatus.FAILED.toString()) && isQcStatusIgoComplete(rec, user)) {
                    return true;
                }
            }
            // check if the status is actually failed but igoComplete is not marked true. Sometimes, sample is marked failed
            // but igoComplete is not checked. It is possible that we do not mark IgoComplete because there is not data to
            // deliver to the user.
            for (DataRecord rec: qcRecords){
                Object qcStatus = rec.getValue(SeqAnalysisSampleQCModel.SEQ_QCSTATUS, user);
                if (qcStatus != null && qcStatus.toString().equalsIgnoreCase(QcStatus.FAILED.toString())) {
                    return true;
                }
            }
        }catch (Exception e){
            String msg = String.format("%s -> Error while checking Sequencing QC status equals 'Passed'", ExceptionUtils.getRootCause(e), ExceptionUtils.getStackTrace(e));
            LOGGER.error(msg);
        }
        return false;
    }

    /**
     * Method to check if Sequencing for a sample is complete based on presence of SeqAnalysisSampleQC as child record
     * and status of SeqAnalysisSampleQC as Passed.
     *
     * @param sample
     * @return
     */
    private static Boolean isSequencingComplete(DataRecord sample, User user){
        DataRecord[] qcRecord = getChildrenofDataRecord(sample, SeqAnalysisSampleQCModel.DATA_TYPE_NAME, user);
        if (qcRecord.length == 0) {
            return false;
        }
        DataRecord record = qcRecord[0];
        Boolean isComplete = isQcStatusIgoComplete(record, user);
        String sequencingStatus = getRecordStringValue(qcRecord[0], SeqAnalysisSampleQCModel.SEQ_QCSTATUS, user);
        Boolean isFailed = sequencingStatus.equalsIgnoreCase(QcStatus.FAILED.toString());
        return isComplete || isFailed;
    }

    /**
     * Method to get BaitSet used for a sample.
     *
     * @param sample
     * @param qcRecords
     * @return
     */
    public static String getBaitSet(DataRecord sample, List<DataRecord> qcRecords, User user) {
        try {
            if (qcRecords.isEmpty()) {
                LOGGER.info(String.format("Seq qc record not found for sample with recordId: %d.", sample.getRecordId()));
                return "";
            }
            for (DataRecord qcRec : qcRecords){
                Object baitset = qcRec.getValue(SeqAnalysisSampleQCModel.BAIT_SET, user);
                if (baitset != null && !StringUtils.isBlank(baitset.toString())){
                    return baitset.toString();
                }
            }
        } catch (NotFound | RemoteException e) {
            LOGGER.error(String.format("%s -> Error while fetching the baitset: %s", ExceptionUtils.getRootCause(e), ExceptionUtils.getStackTrace(e)));
            return "";
        }
        return "";
    }

    /**
     * Method to get child record of specified DataType under a Sample.
     *
     * @param sample
     * @param childRecordType
     * @param user
     * @return
     */
    private static DataRecord getChildDataRecordOfType(DataRecord sample, String childRecordType, User user) {
        try {
            Object requestId = sample.getValue(SampleModel.REQUEST_ID, user);
            if (sample.getChildrenOfType(childRecordType, user).length > 0) {
                return sample.getChildrenOfType(childRecordType, user)[0];
            }
            Stack<DataRecord> sampleStack = new Stack<>();
            DataRecord[] childSamples = sample.getChildrenOfType(SampleModel.DATA_TYPE_NAME, user);
            for (DataRecord childSample : childSamples) {
                Object childSampleRequestId = childSample.getValue(SampleModel.REQUEST_ID, user);
                if(childSampleRequestId !=null && requestId!= null && requestId.toString().equalsIgnoreCase(childSampleRequestId.toString())){
                    sampleStack.add(childSample);
                }
            }
            if (sampleStack.isEmpty()){
                return null;
            }
            do {
                DataRecord startSample = sampleStack.pop();
                DataRecord[] seqQcRecs = startSample.getChildrenOfType(childRecordType, user);
                if (seqQcRecs.length > 0) {
                    return seqQcRecs[0];
                }
                List<DataRecord> childSampleRecords = Arrays.asList(startSample.getChildrenOfType(SampleModel.DATA_TYPE_NAME, user));
                if (childSampleRecords.size() > 0) {
                    for (DataRecord sam : childSampleRecords) {
                        Object reqId = sam.getValue(SampleModel.REQUEST_ID, user);
                        if (reqId != null && requestId.equals(reqId.toString())) {
                            sampleStack.add(sam);
                        }
                    }
                }
            } while (!sampleStack.isEmpty());
        } catch (Exception e) {
            LOGGER.error(String.format("RemoteException -> Error occurred while finding related SampleCMOInfoRecords for Sample with RecordId: %d\n%s", sample.getRecordId(), ExceptionUtils.getStackTrace(e)));
        }
        return null;
    }

    /**
     * Method to get a list of child records of specified DataType under a Sample.
     * @param sample
     * @param childRecordType
     * @param user
     * @return
     */
    public static List<DataRecord> getChildDataRecordsOfType(DataRecord sample, String childRecordType, User user){
        List<DataRecord> records = new ArrayList<>();
        try {
            Object requestId = sample.getValue(SampleModel.REQUEST_ID, user);
            Stack<DataRecord> sampleStack = new Stack<>();
            sampleStack.add(sample);
            do {
                DataRecord startSample = sampleStack.pop();
                DataRecord[] childRecs = startSample.getChildrenOfType(childRecordType, user);
                if (childRecs.length > 0) {
                    records.addAll(Arrays.asList(childRecs));
                }
                List<DataRecord> childSampleRecords = Arrays.asList(startSample.getChildrenOfType(SampleModel.DATA_TYPE_NAME, user));
                if (childSampleRecords.size() > 0) {
                    for (DataRecord sam : childSampleRecords) {
                        Object reqId = sam.getValue(SampleModel.REQUEST_ID, user);
                        if (reqId != null && requestId.equals(reqId.toString())) {
                            sampleStack.add(sam);
                        }
                    }
                }
            } while (!sampleStack.isEmpty());
        } catch (Exception e) {
            LOGGER.error(String.format("RemoteException -> Error occurred while finding related SampleCMOInfoRecords for Sample with RecordId: %d\n%s", sample.getRecordId(), ExceptionUtils.getStackTrace(e)));
        }
        return records;
    }


    /**
     * Returns whether the input status is a failed one
     *
     * @param status
     * @return
     */
    public static Boolean isFailedStatus(String status) {
        if (status != null && status != "") {
            return status.toLowerCase().contains("failed");
        }
        return Boolean.FALSE;
    }

    /**
     * Method to check is sample status is a completed status.
     *
     * @param status
     * @return
     */
    public static boolean isCompleteStatus(String status) {
        return status.toLowerCase().contains("completed") || status.toLowerCase().contains("failed");
    }

    /**
     * Safely retrieves a String value from a dataRecord. Returns empty string on error.
     *
     * @param record
     * @param key
     * @param user
     * @return
     */
    public static String getRecordStringValue(DataRecord record, String key, User user) {
        try {
            if (record.getValue(key, user) != null) {
                return record.getStringVal(key, user);
            }
        } catch (NotFound | RemoteException | NullPointerException e) {
            LOGGER.error(String.format("Failed to get (String) key %s from Data Record: %d", key, record.getRecordId()));
            return "";
        }
        return "";
    }

    /**
     * Safely retrieves a Long Value from a dataRecord
     *
     * @param record
     * @param key
     * @param user
     * @return
     */
    public static Long getRecordLongValue(DataRecord record, String key, User user) {
        try {
            return record.getLongVal(key, user);
        } catch (NotFound | RemoteException | NullPointerException e) {
            LOGGER.error(String.format("Failed to get (Long) key %s from Sample Record: %d", key, record.getRecordId()));
        }
        return null;
    }

    /**
     * Safely retrieves a Double Value from a dataRecord
     *
     * @param record
     * @param key
     * @param user
     * @return
     */
    public static Double getRecordDoubleValue(DataRecord record, String key, User user) {
        try {
            return record.getDoubleVal(key, user);
        } catch (NotFound | RemoteException | NullPointerException e) {
            LOGGER.error(String.format("Failed to get (Double) key %s from Sample Record: %d", key, record.getRecordId()));
        }
        return null;
    }


    /**
     * Safely retrieves a Short Value from a dataRecord
     *
     * @param record
     * @param key
     * @param user
     * @return
     */
    public static Short getRecordShortValue(DataRecord record, String key, User user) {
        try {
            return record.getShortVal(key, user);
        } catch (NotFound | RemoteException | NullPointerException e) {
            LOGGER.error(String.format("Failed to get (Short) key %s from Sample Record: %d", key, record.getRecordId()));
        }
        return null;
    }


    /**
     * Safely retrieves a Boolean Value from a dataRecord
     *
     * @param record
     * @param key
     * @param user
     * @return
     */
    public static Boolean getRecordBooleanValue(DataRecord record, String key, User user) {
        try {
            return record.getBooleanVal(key, user);
        } catch (NotFound | RemoteException | NullPointerException e) {
            LOGGER.error(String.format("Failed to get (Boolean) key %s from Sample Record: %d", key, record.getRecordId()));
        }
        return null;
    }

    /**
     * Returns the DataRecord children of an input datatype of an input data record
     *
     * @param record
     * @param childDataType
     * @param user
     * @return
     */
    public static DataRecord[] getChildrenofDataRecord(DataRecord record, String childDataType, User user) {
        try {
            DataRecord[] sampleChildren = record.getChildrenOfType(childDataType, user);
            return sampleChildren;
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to retrieve %s children of %s record: %d", childDataType, record.getDataTypeName(), record.getRecordId()));
        }
        return new DataRecord[0];
    }

    /**
     * Returns Datatype descendents of the input DataRecord
     *
     * @param record
     * @param dataType
     * @param user
     * @return
     */
    public static List<DataRecord> getDescendantsOfType(DataRecord record, String dataType, User user) {
        try {
            return record.getDescendantsOfType(dataType, user);
        } catch (Exception e){
            LOGGER.error(String.format("Failed to retrieve %s descendents of %s record: %d", dataType, record.getDataTypeName(), record.getRecordId()));
        }
        return new ArrayList<>();
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
    public static Object getValueFromDataRecord(DataRecord record, String fieldName, String fieldType, User user) throws NotFound, RemoteException {
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
     * Method to get Main Tumor Type using Tumor Type Name eg: Breast Cancer or Pancreatic cancer etc.
     *
     * @param url
     * @return String
     */
    private static String getOncotreeTumorTypeUsingName(URL url) {
        HttpURLConnection con = null;
        StringBuilder response = new StringBuilder();
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
            LOGGER.info(String.format("Error while querying oncotree api for name search using url %s. Will attempt to search using oncotree api for code search:\n%s", url, e.getMessage()));
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
    private static String getOncotreeTumorTypeUsingCode(URL url, String tumorType) {
        HttpURLConnection con = null;
        StringBuilder response = new StringBuilder();
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
            LOGGER.info(String.format("Error while querying oncotree api using code search using url %s. Cannot find Main tumor type.\n%s", url, e.getMessage()));
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
    public static String getOncotreeTumorType(String tumorType) {
        String mainTumorType = "";
        try {
            // In LIMS tumor types entry is not controlled. Sometimes tumor type as tumor name is entered and other times tumor type code is entered.
            // First query oncotree using api for name search enforcing exact match
            URL urlExact = new URL("http://oncotree.mskcc.org/api/tumorTypes/search/name/" + tumorType.split("/")[0].replace(" ", "%20") + "?exactMatch=true");
            mainTumorType = getOncotreeTumorTypeUsingName(urlExact);
            // If name search with exact match enforced returns nothing, then query oncotree again with exact match turned off
            if (StringUtils.isBlank(mainTumorType)) {
                URL url = new URL("http://oncotree.mskcc.org/api/tumorTypes/search/name/" + tumorType.split("/")[0].replace(" ", "%20") + "?exactMatch=false");
                mainTumorType = getOncotreeTumorTypeUsingName(url);
            }
            // If name search returns nothing, then query oncotree using api for code search
            if (StringUtils.isBlank(mainTumorType)) {
                URL url2 = new URL("http://oncotree.mskcc.org/api/tumorTypes/search/code/" + tumorType.split("/")[0].replace(" ", "%20") + "?exactMatch=true");
                mainTumorType = getOncotreeTumorTypeUsingCode(url2, tumorType);
            }
        } catch (Exception e) {
            LOGGER.error(String.format("Error occured while querying oncotree end point for Tumor Type %s\n%s", tumorType, e.getMessage()));
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
    public static String getOriginSampleId(DataRecord sample, User user) {
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
            LOGGER.error(String.format("Error occured while finding related SampleCMOInfoRecords for Sample %s", sampleId));
        }
        return sampleId;
    }

    /**
     * Returns the Lims Stage corresponding to the most advacned stage of the DataRecord
     *
     * @param sample
     * @param requestId
     * @param conn
     * @return
     */
    // TODO - how to handle "Ready For" <- should be the one that proceeds it (eventually)
    public static String getMostAdvancedLimsStage(DataRecord sample, String requestId, ConnectionLIMS conn) {
        User user = conn.getConnection().getUser();
        String mostAdvancedSampleStatus = getMostAdvancedSampleStatus(sample, requestId, user);
        if (mostAdvancedSampleStatus.toLowerCase().contains(FAILED_STATUS_TEXT) && mostAdvancedSampleStatus.contains(STAGE_SEQUENCING_ANALYSIS)) {
            return mostAdvancedSampleStatus;
        }
        if (mostAdvancedSampleStatus.toLowerCase().contains(FAILED_STATUS_TEXT)) {
            return mostAdvancedSampleStatus;
        }
        LimsStage stage = getLimsStageFromStatus(conn, mostAdvancedSampleStatus);
        return stage.toString();
    }

    /**
     * Method to get latest sample status.
     *
     * @param sample
     * @param requestId
     * @return
     */
    private static String getMostAdvancedSampleStatus(DataRecord sample, String requestId, User user) {
        String sampleId = "";
        String sampleStatus = STAGE_AWAITING_PROCESSING;        // Default stage
        String currentSampleType = "";
        String currentSampleStatus = "";
        try {
            String pendingStatus = getRecordStringValue(sample, SampleModel.EXEMPLAR_SAMPLE_STATUS, user);
            if (!pendingStatus.isEmpty()) {
                sampleStatus = pendingStatus;
            }
            sampleId = sample.getStringVal("SampleId", user);
            int statusOrder = -1;
            long recordId = 0;
            Stack<DataRecord> sampleStack = new Stack<>();
            sampleStack.add(sample);
            do {
                DataRecord current = sampleStack.pop();
                currentSampleType = (String) getValueFromDataRecord(current, "ExemplarSampleType", "String", user);
                currentSampleStatus = (String) getValueFromDataRecord(current, "ExemplarSampleStatus", "String", user);
                int currentStatusOrder = getSampleTypeOrder(currentSampleType.toLowerCase());
                long currentRecordId = current.getRecordId();
                if (isSequencingComplete(current, user)) {
                    // Check if Sample has failed Sequencing analysis
                    List<DataRecord> seqQcRecords =  getChildDataRecordsOfType(current, SeqAnalysisSampleQCModel.DATA_TYPE_NAME, user);
                    if (isSequencingPassedComplete(seqQcRecords, user)){
                        // Return the Completed-Sequencing status, NOT currentSampleStatus as this could not unrelated to sequencing
                        return String.format("%s%s", WORKFLOW_STATUS_COMPLETED, STAGE_SEQUENCING_ANALYSIS);
                    }
                    if (isSequencingFailedComplete(seqQcRecords, user)){
                        return String.format("%s%s", WORKFLOW_STATUS_FAILED, STAGE_SEQUENCING_ANALYSIS);
                    }
                }
                if (currentRecordId > recordId && currentStatusOrder >= statusOrder && isCompleteStatus(currentSampleStatus)) {
                    sampleStatus = currentSampleStatus;
                    recordId = currentRecordId;
                    statusOrder = currentStatusOrder;
                    LOGGER.info("current status: " + sampleStatus);
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
            System.out.println("Status error: " + e);
            LOGGER.error(String.format("Error while getting status for sample '%s'.", sampleId));
            return "unknown";
        }
        return sampleStatus;
    }

    /**
     * Method to check if the sample status is equivalent to "completed sequencing".
     *
     * @param status
     * @return
     */
    public static boolean isSequencingCompleteStatus(String status) {
        status = status.toLowerCase();
        return status.contains("completed - ") && status.contains("illumina") && status.contains("sequencing");
    }

    /**
     * Method to get order or Sample based on its SampleType.
     *
     * @param sampleType
     * @return int order
     */
    public static int getSampleTypeOrder(String sampleType) {
        if (TISSUE_SAMPLE_TYPES.contains(sampleType)) {
            return 1;
        }
        if (NUCLEIC_ACID_TYPES.contains(sampleType)) {
            return 2;
        }
        if (LIBRARY_SAMPLE_TYPES.contains(sampleType)) {
            return 3;
        }
        if (CAPTURE_SAMPLE_TYPES.contains(sampleType)) {
            return 4;
        }
        if (POOLED_SAMPLE_TYPES.contains(sampleType)) {
            return 5;
        }
        return 0;
    }

    /**
     * Method to get base Sample ID when aliquot annotation is present.
     * Example: for sample id 012345_1_1_2, base sample id is 012345_1
     * Example2: for sample id 012345_B_1_1_2, base sample id is 012345_B_1
     * @param sampleId
     * @return
     */
    public static String getBaseSampleId(String sampleId){
        Pattern alphabetPattern = Pattern.compile(IGO_ID_WITH_ALPHABETS_PATTERN);
        Pattern withoutAlphabetPattern = Pattern.compile(IGO_ID_WITHOUT_ALPHABETS_PATTERN);
        if (alphabetPattern.matcher(sampleId).matches()){
            String[] sampleIdValues =  sampleId.split("_");
            return String.join("_", Arrays.copyOfRange(sampleIdValues,0,3));
        }
        if(withoutAlphabetPattern.matcher(sampleId).matches()){
            String[] sampleIdValues =  sampleId.split("_");
            return String.join("_", Arrays.copyOfRange(sampleIdValues,0,2));
        }
        return sampleId;
    }

    /**
     * This method will return the first DataRecord(s) with DATA_TYPE_NAME equal to @param targetDataType found in the
     * parent tree upstream. The targetDataType DataRecord is usually present as a child on one of the parents in the
     * hierarchy tree. @param parentDataType must either be same as @param record or @param record must be directly
     * under a DataRecord with DATA_TYPE_NAME equal to @param parentDataType.
     * @param record
     * @param parentDataType
     * @param targetDataType
     * @return
     */
    public static List<DataRecord> getRecordsOfTypeFromParents(DataRecord record, String parentDataType, String targetDataType, User user) {
        List<DataRecord> records = new ArrayList<>();
        try {
            if (record.getChildrenOfType(targetDataType, user).length > 0){
                return Arrays.asList(record.getChildrenOfType(targetDataType, user));
            }
            Stack<DataRecord> recordsStack = new Stack<>();
            List<DataRecord> parentRecords = record.getParentsOfType(parentDataType, user);
            recordsStack.addAll(parentRecords);
            while (!recordsStack.isEmpty()){
                DataRecord poppedRecord = recordsStack.pop();
                if (poppedRecord.getChildrenOfType(targetDataType, user).length > 0){
                    return Arrays.asList(poppedRecord.getChildrenOfType(targetDataType, user));
                }
                recordsStack.addAll(poppedRecord.getParentsOfType(parentDataType, user));
            }
        } catch (Exception e) {
            LOGGER.error(String.format("%s -> Error while getting %s records for %s record with Record Id %d,\n%s",
                    ExceptionUtils.getRootCause(e), targetDataType, record.getDataTypeName(), record.getRecordId(), ExceptionUtils.getStackTrace(e)));
        }
        return records;
    }

    /**
     * Method to get Pooled Samples related to Sequencing experiment.
     * @param seqExperiments
     * @param runId
     * @param user
     * @return
     */
    public static List<DataRecord> getSamplesRelatedToSeqExperiment(List<DataRecord> seqExperiments, String runId, User user) {
        List<DataRecord> relatedSamples = new ArrayList<>();
        try {
            for (DataRecord exp : seqExperiments) {
                DataRecord [] flowCells = exp.getChildrenOfType(FlowCellModel.DATA_TYPE_NAME, user);
                for (DataRecord fc : flowCells) {
                    DataRecord [] flowCellLanes = fc.getChildrenOfType(FlowCellLaneModel.DATA_TYPE_NAME, user);
                    for (DataRecord fcl : flowCellLanes){
                        relatedSamples.addAll(fcl.getParentsOfType(SampleModel.DATA_TYPE_NAME, user));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(String.format("%s Exception while retrieving flow cell lanes for sequencing run %s: %s", ExceptionUtils.getRootCause(e), runId, ExceptionUtils.getStackTrace(e)));
        }
        return relatedSamples;
    }

    public static double selectLarger(String requestedReads) {
        // example "30-40 million" => 40.0
        String[] parts = requestedReads.split("[ -]+");
        requestedReads = parts[1].trim();
        return Double.parseDouble(requestedReads);
    }
}