package org.mskcc.limsrest.util;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sloan.cmo.recmodels.SampleModel;
import com.velox.sloan.cmo.recmodels.SeqAnalysisSampleQCModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mskcc.domain.sample.NucleicAcid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.mskcc.limsrest.ConnectionLIMS;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.mskcc.limsrest.util.StatusTrackerConfig.*;

public class Utils {
    private final static Log LOGGER = LogFactory.getLog(Utils.class);

    public final static String SEQ_QC_STATUS_PASSED = "passed";
    public final static String SEQ_QC_STATUS_FAILED = "failed";
    public final static String SEQ_QC_STATUS_PENDING = "not available";

    private final static List<String> TISSUE_SAMPLE_TYPES = Arrays.asList("cells","plasma","blood","tissue","buffy coat","blocks/slides","ffpe sample","other","tissue sample");
    private final static List<String> NUCLEIC_ACID_TYPES = Arrays.asList("dna","rna","cdna","cfdna","dna,cfdna","amplicon","pre-qc rna");
    private final static List<String> LIBRARY_SAMPLE_TYPES = Arrays.asList("dna library", "cdna library", "gdna library");
    private final static List<String> CAPTURE_SAMPLE_TYPES = Collections.singletonList("capture library");
    private final static List<String> POOLED_SAMPLE_TYPES = Collections.singletonList("pooled library");

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
     * Method to check if Sequencing for a sample is complete based on presence of SeqAnalysisSampleQC as child record
     * and status of SeqAnalysisSampleQC as Passed.
     *
     * @param sample
     * @return
     */
    public static Boolean isSequencingComplete(DataRecord sample, User user) {
        DataRecord[] qcRecord = getChildrenofDataRecord(sample, SeqAnalysisSampleQCModel.DATA_TYPE_NAME, user);
        if (qcRecord.length == 0) {
            return false;
        }
        String sequencingStatus = getRecordStringValue(qcRecord[0], SeqAnalysisSampleQCModel.SEQ_QCSTATUS, user);
        return sequencingStatus.equalsIgnoreCase(SEQ_QC_STATUS_PASSED) || sequencingStatus.equalsIgnoreCase(SEQ_QC_STATUS_FAILED);
    }

    /**
     * Returns whether the input status is a failed one
     *
     * @param status
     * @return
     */
    public static Boolean isFailedStatus(String status) {
        if(status != null && status != ""){
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
        } catch (IoError | RemoteException e) {
            LOGGER.error(String.format("Failed to retrieve %s children of %s record: %d", childDataType, record.getDataTypeName(), record.getRecordId()));
        }
        return new DataRecord[0];
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
     * @param tumorType
     * @return String
     */
    private static String getOncotreeTumorTypeUsingName(URL url, String tumorType) {
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
            LOGGER.info(String.format("Error while querying oncotree api for name search. Will attempt to search using oncotree api for code search:\n%s", e.getMessage()));
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
            LOGGER.info(String.format("Error while querying oncotree api using code search. Cannot find Main tumor type.\n%s", e.getMessage()));
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
            // First query oncotree using api for name search
            URL url = new URL("http://oncotree.mskcc.org/api/tumorTypes/search/name/" + tumorType.split("/")[0].replace(" ", "%20") + "?exactMatch=false");
            mainTumorType = getOncotreeTumorTypeUsingName(url, tumorType);
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
            if(!pendingStatus.isEmpty()){
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
                    // Return the Completed-Sequencing status, NOT currentSampleStatus as this could not unrelated to sequencing
                    return String.format("%s%s", WORKFLOW_STATUS_COMPLETED, "Illumina Sequencing");
                }
                if (currentRecordId > recordId && currentStatusOrder > statusOrder && isCompleteStatus(currentSampleStatus)) {
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

}