package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.controller.GetSampleStatus;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Task to compile data for the delphi-sample-tracker app. This endpoint currently collects data for all the entries in DMPSampleTracker for "WholeExomeSequencing" and retrieves information from
 * CVR db, oncotree db and LIMS.
 *
 * @author sharmaa1
 */
public class GetSampleStatusTask {
    private final List<String> TISSUE_SAMPLE_TYPES = Arrays.asList("cells","plasma","blood","tissue","buffy coat","blocks/slides","ffpe sample","other","tissue sample");
    private final List<String> SAMPLETYPES_IN_ORDER = Arrays.asList("dna", "rna", "cdna", "amplicon", "dna library", "cdnalibrary", "pooled library");
    private final List<String> NUCLEIC_ACID_TYPES = Arrays.asList("dna","rna","cdna","cfdna","dna,cfdna","amplicon","pre-qc rna");
    private final List<String> LIBRARY_SAMPLE_TYPES = Arrays.asList("dna library", "cdna library", "gdna library");
    private final List<String> CAPTURE_SAMPLE_TYPES = Arrays.asList("capture library");
    private final List<String> POOLED_SAMPLE_TYPES = Arrays.asList("pooled library");

    DataRecordManager dataRecordManager;
    private Log log = LogFactory.getLog(GetSampleStatus.class);
    private String igoId;
    private ConnectionLIMS conn;
    private User user;

    public GetSampleStatusTask(String igoId, ConnectionLIMS conn) {
        this.igoId = igoId;
        this.conn = conn;
    }


    public String execute() {
        long start = System.currentTimeMillis();
        String status = null;
        String requestId = null;
        try {
            VeloxConnection vConn = conn.getConnection();
            user = vConn.getUser();
            dataRecordManager = vConn.getDataRecordManager();
            log.info("Starting GetSampleStatus task using IGO ID " + igoId);
            List<DataRecord> samples = dataRecordManager.queryDataRecords("Sample", "SampleId = '" + igoId + "'", user);
            log.info("Num Sample Records: " + samples.size());
            if (samples.size()==1){
                DataRecord sample = samples.get(0);
                requestId = getValueFromDataRecord(sample, "RequestId");
                status = getSampleStatus(samples.get(0), requestId);
            }
            log.info("request id: " + requestId);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
        log.info("Total time: " + (System.currentTimeMillis()-start) + " ms");
        log.info("status: " + status);
        return status;
    }


    /**
     * Get a DataField value from a DataRecord.
     *
     * @param record
     * @param fieldName
     * @return Object
     * @throws NotFound
     * @throws RemoteException
     */
    private String getValueFromDataRecord(DataRecord record, String fieldName) throws NotFound, RemoteException {
        if (record == null) {
            return "";
        }
        if (record.getValue(fieldName, user) != null) {
            return record.getStringVal(fieldName, user);
        }
        return "";
    }


    /**
     * Method to get order or Sample based on its SampleType.
     * @param sampleType
     * @return int order
     */
    private int getSampleTypeOrder(String sampleType){
        if (TISSUE_SAMPLE_TYPES.contains(sampleType)){
            return 1;
        }
        if (NUCLEIC_ACID_TYPES.contains(sampleType)){
            return 2;
        }
        if (LIBRARY_SAMPLE_TYPES.contains(sampleType)){
            return 3;
        }
        if (CAPTURE_SAMPLE_TYPES.contains(sampleType)){
            return 4;
        }
        if (POOLED_SAMPLE_TYPES.contains(sampleType)){
            return 5;
        }
        return 0;
    }


    /**
     * Method to get latest sample status.
     *
     * @param sample
     * @param requestId
     * @return
     */
    private String getSampleStatus(DataRecord sample, String requestId) {
        String sampleId = "";
        String sampleStatus;
        String currentSampleType = "";
        String currentSampleStatus = "";
        try {
            sampleId = sample.getStringVal("SampleId", user);
            sampleStatus = (String) getValueFromDataRecord(sample, "ExemplarSampleStatus");
            int statusOrder = -1;
            long recordId = 0;
            Stack<DataRecord> sampleStack = new Stack<>();
            sampleStack.add(sample);
            do {
                DataRecord current = sampleStack.pop();
                currentSampleType = getValueFromDataRecord(current, "ExemplarSampleType");
                currentSampleStatus = getValueFromDataRecord(current, "ExemplarSampleStatus");
                int currentStatusOrder = getSampleTypeOrder(currentSampleType.toLowerCase());
                long currentRecordId = current.getRecordId();
                if (isSequencingComplete(current)) {
                    return "Completed Sequencing";
                }
                if (currentRecordId > recordId && currentStatusOrder > statusOrder && isCompleteStatus(currentSampleStatus)) {
                    sampleStatus = resolveCurrentStatus(currentSampleStatus, currentSampleType);
                    recordId = currentRecordId;
                    statusOrder = currentStatusOrder;
                    log.info("current status: " + sampleStatus);
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
            return "unknown";
        }
        return sampleStatus;
    }

    /**
     * Method to check is sample status is a completed status.
     *
     * @param status
     * @return
     */
    private boolean isCompleteStatus(String status) {
        return status.toLowerCase().contains("completed") || status.toLowerCase().contains("failed");
    }

    /**
     * Method to check if the sample status is equivalent to "completed sequencing".
     *
     * @param status
     * @return
     */
    private boolean isSequencingCompleteStatus(String status) {
        status = status.toLowerCase();
        return status.contains("completed - ") && status.contains("illumina") && status.contains("sequencing");
    }

    /**
     * Method to resolve the sample status to one of the main sample statuses.
     *
     * @param status
     * @param sampleType
     * @return
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
        return status;
    }

    /**
     * Method to check if Sequencing for a sample is complete based on presence of SeqAnalysisSampleQC as child record and status of SeqAnalysisSampleQC as Passed.
     *
     * @param sample
     * @return
     */
    private Boolean isSequencingComplete(DataRecord sample) {
        try {
            List<DataRecord> seqAnalysisRecords = Arrays.asList(sample.getChildrenOfType("SeqAnalysisSampleQC", user));
            if (seqAnalysisRecords.size() > 0) {
                Object sequencingStatus = seqAnalysisRecords.get(0).getValue("SeqQCStatus", user);
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
