package org.mskcc.limsrest.service;

import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.PoolingSampleLibProtocolModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.assignedprocess.QcStatus;
import org.mskcc.limsrest.service.assignedprocess.QcStatusAwareProcessAssigner;

import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.mskcc.util.VeloxConstants.SAMPLE;

/**
 * A queued task that takes a request id and returns all some request information and some sample information
 *
 * @author Aaron Gabow
 */
public class ToggleSampleQcStatusTask {
    private static Log log = LogFactory.getLog(ToggleSampleQcStatusTask.class);
    // Pooling protocol that is used to identify sample that should be re-pooled
    private final static String POOLING_PROTOCOL = PoolingSampleLibProtocolModel.DATA_TYPE_NAME;
    protected QcStatusAwareProcessAssigner qcStatusAwareProcessAssigner = new QcStatusAwareProcessAssigner();
    boolean isSeqAnalysisSampleqc = true; // which LIMS table to update
    private ConnectionLIMS conn;
    private long recordId;
    private String status;
    private String requestId;
    private String sampleId;
    private String correctedSampleId;
    private String run;
    private String analyst;
    private String note;
    private String fastqPath;
    private boolean isSeq = true;
    private boolean isOnt = false;
    private String airflow_pass;

    protected static void setSeqAnalysisSampleQcStatus(DataRecord seqQc, QcStatus qcStatus, String status, User user)
            throws IoError, InvalidValue, NotFound, RemoteException, ServerException {
        if (qcStatus == QcStatus.IGO_COMPLETE) {
            seqQc.setDataField("PassedQc", Boolean.TRUE, user);
            seqQc.setDataField("SeqQCStatus", QcStatus.PASSED.getText(), user);
            seqQc.setDataField("DateIgoComplete", System.currentTimeMillis(), user);
        } else {
            seqQc.setDataField("SeqQCStatus", status, user);
            seqQc.setDataField("PassedQc", Boolean.FALSE, user);
        }
    }

    protected static String formatMoveFailedFastqJSON(String igoId, String run, Date execDate) {
        //2021-01-01T15:00:00Z - airflow format
        DateFormat airflowFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss");
        airflowFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateStr = airflowFormat.format(execDate);
        dateStr = dateStr.replace(' ', 'T') + "Z";
        // create json body like:
        // {"execution_date": "2022-05-19", "conf": {"igo_id":"12345_1","run":"xyz"}}
        String conf = "\"conf\":{\"igo_id\":\""+igoId+"\",\"run\":\""+run+"\"}";
        String body ="{\"execution_date\":\""+dateStr+"\","+conf+"}";
        return body;
    }

    public ToggleSampleQcStatusTask(long recordId, String status, String requestId, String correctedSampleId,
                                    String run, String qcType, String analyst, String note, String fastqPath,
                                    ConnectionLIMS conn, String airflow_pass) {
        this.recordId = recordId;
        this.status = status;
        this.requestId = requestId;
        this.correctedSampleId = (correctedSampleId == null) ? "" : correctedSampleId;
        this.run = run;
        this.analyst = analyst;
        this.note = note;
        this.fastqPath = fastqPath;
//        if ("Post".equals(qcType)) {
//            isSeqAnalysisSampleqc = false;
//        }
        if (qcType.equals("Ont")) {
            isOnt = true;
            isSeq = false;
        }
        this.conn = conn;
        this.airflow_pass = airflow_pass;
    }

    public String execute() {
        // designed to update either SeqAnalysisSampleQC or PostSeqAnalysisQC LIMS status tables
        try {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            DataRecordManager dataRecordManager = vConn.getDataRecordManager();
            if (isSeq || isOnt) {
                log.info("QC type is Seq or Ont");
                if (isSeq) {
                    log.info("SeqAnalysisSampleQC updating to " + status + " for record:" + recordId);
                    DataRecord seqQc = dataRecordManager.querySystemForRecord(recordId, "SeqAnalysisSampleQC", user);

                    String currentStatusLIMS = (String) seqQc.getDataField("SeqQCStatus", user);
                    // Failed status is terminal on Qc site and can't be changed.
                    if (QcStatus.FAILED.getText().equals(currentStatusLIMS)) {
                        log.info("QC status already failed and can't be updated from there via REST interface.");
                        return QcStatus.FAILED.toString();
                    }

                    QcStatus qcStatus = QcStatus.fromString(status);
                    setSeqAnalysisSampleQcStatus(seqQc, qcStatus, status, user);

                    if (qcStatus == QcStatus.RESEQUENCE_POOL) {
                        qcStatusAwareProcessAssigner.assign(dataRecordManager, user, seqQc, qcStatus);
                    } else if (qcStatus == QcStatus.REPOOL_SAMPLE) {
                        repoolByPoolingProtocol(seqQc, qcStatus, dataRecordManager, user);
                    }
                    dataRecordManager.storeAndCommit("SeqAnalysisSampleQC updated to " + status, null, user);

                    log.info("SeqAnalysisSampleQC updated to:" + status + " from:" + currentStatusLIMS);

                    // Call Airflow to move the fastq.gz files if failed
                    if (qcStatus == QcStatus.FAILED) {
                        if (airflow_pass == null || airflow_pass == "")
                            log.error("Airflow password not initialized, can't move failed fastq.gz files.");
                        else {
                            String igoIdFromLims = (String) seqQc.getDataField("SampleId", user); // IGO ID
                            String runFromLims = (String) seqQc.getDataField("SequencerRunFolder", user);

                            Date execDate = new Date(System.currentTimeMillis() + 10000);
                            String body = formatMoveFailedFastqJSON(igoIdFromLims, runFromLims, execDate);
                            String cmd = "/bin/curl -X POST -d '" + body + "' \"http://igo-ln01:8080/api/v1/dags/move_failed_fastqs/dagRuns\" -H 'content-type: application/json' --user \"airflow-api:" + airflow_pass + "\"";
                            log.info("Calling airflow pipeline to move failed fastq.gz files: " + cmd);
                            ProcessBuilder processBuilder = new ProcessBuilder();
                            processBuilder.command("bash", "-c", cmd);
                            Process process = processBuilder.start();
                        }
                    }
                }
                else if (isOnt) {
                    log.info("SequencingAnalysisONT updating to " + status + " for record:" + recordId);
                    DataRecord ontQc = dataRecordManager.querySystemForRecord(recordId, "SequencingAnalysisONT", user);

                    String currentStatusLIMS = (String) ontQc.getDataField("SeqQCStatus", user);
                    // Failed status is terminal on Qc site and can't be changed.
                    if (QcStatus.FAILED.getText().equals(currentStatusLIMS)) {
                        log.info("QC status already failed and can't be updated from there via REST interface.");
                        return QcStatus.FAILED.toString();
                    }

                    QcStatus qcStatus = QcStatus.fromString(status);
                    setSeqAnalysisSampleQcStatus(ontQc, qcStatus, status, user);

                    if (qcStatus == QcStatus.RESEQUENCE_POOL) {
                        qcStatusAwareProcessAssigner.assign(dataRecordManager, user, ontQc, qcStatus);
                    } else if (qcStatus == QcStatus.REPOOL_SAMPLE) {
                        repoolByPoolingProtocol(ontQc, qcStatus, dataRecordManager, user);
                    }
                    dataRecordManager.storeAndCommit("SequencingAnalysisONT updated to " + status, null, user);

                    log.info("SequencingAnalysisONT updated to:" + status + " from:" + currentStatusLIMS);
                }
            }
            else { //qcType.equals("Post")
                List<DataRecord> request = dataRecordManager.queryDataRecords("Request", "RequestId = '" + requestId + "'", user);
                if (request.size() != 1) {
                    return "Invalid Request Id";
                }
                DataRecord[] childSamples = request.get(0).getChildrenOfType("Sample", user);
                DataRecord matchedSample = null;
                //first try to resolve through cmo info records. Only after this try to resolve directly off samples names.
                //we must do it this was because otherwise if the cmo info records resolve a sample swap we could miss it
                for (int i = 0; i < childSamples.length; i++) {
                    DataRecord[] cmoInfo = childSamples[i].getChildrenOfType("SampleCMOInfoRecords", user);
                    if (cmoInfo.length > 0 && cmoInfoCheck(correctedSampleId, cmoInfo[0], user)) {
                        matchedSample = childSamples[i];
                    }
                }
                if (matchedSample == null) {
                    try {
                        for (int i = 0; i < childSamples.length; i++) {
                            if (correctedSampleId.equals(childSamples[i].getStringVal("SampleId", user))) {
                                matchedSample = childSamples[i];
                            }
                        }
                    } catch (NullPointerException npe) {
                        log.warn(String.format("Failed to find Matched Sample. Error: %s", npe.getMessage()));
                    }
                }
                if (matchedSample == null) {
                    return "Invalid corrected sample id";
                }
                //Because it's possible that the wet lab puts has children of the matched sample in multiple requests and while unlikely they could end up on the same sequencer run, we fail if this does happen
                List<DataRecord> pqcs = matchedSample.getDescendantsOfType("PostSeqAnalysisQC", user);
                int matchCount = 0;
                for (DataRecord pqc : pqcs) {
                    try {
                        if (run.equals(pqc.getStringVal("SequencerRunFolder", user))) {
                            matchCount++;
                            if (status != null)
                                pqc.setDataField("PostSeqQCStatus", status, user);
                            if (analyst != null)
                                pqc.setDataField("Analyst", analyst, user);
                            if (note != null) {
                                String oldNote = "";
                                try {
                                    oldNote = pqc.getStringVal("Note", user);
                                } catch (NullPointerException npe) {
                                    log.warn(String.format("Failed to parse out note from PostSeqAnalysisQC record",
                                            npe.getMessage()));
                                }
                                pqc.setDataField("Note", oldNote + "\n" + note, user);
                            }
                        }
                    } catch (NullPointerException npe) {
                        log.warn(String.format("Failed to parse data from PostSeqAnalysisQC record",
                                npe.getMessage()));
                    }
                }
                if (matchCount == 0) {
                    return "ERROR: Failed to match a triplet with project, sample id and run id" + requestId + "," + sampleId + "," + run;
                }
                if (matchCount > 1) {
                    return "ERROR: It appears that multiple children of the sample were sequenced on the same run. Please fail sample " + requestId + ", " + correctedSampleId + " in the lims";
                }
                dataRecordManager.storeAndCommit("PostSeqAnalysisQC updated to " + status, user);
            }
        } catch (Throwable e) {
            String results = "ERROR IN SETTING REQUEST STATUS";
            log.error(e);
            return results;
        }

        return status;
    }

    public boolean cmoInfoCheck(String correctedId, DataRecord cmoInfo, User user) {
        try {
            if (correctedId.equals(cmoInfo.getStringVal("CorrectedCMOID", user)) && !cmoInfo.getStringVal
                    ("CorrectedCMOID", user).equals("")) {
                return true;
            } else if (correctedId.equals(cmoInfo.getStringVal("OtherSampleId", user))) {
                return true;
            }
        } catch (NullPointerException npe) {
        } catch (NotFound | RemoteException e) {
        }
        return false;
    }

    /**
     * Searches for record w/ @POOLING_PROTOCOL and assigns process based on that record. Samples with both Statuses
     * of "Ready for - Pooling of Sample Libraries by Volume" & "Ready for - Pooling of Sample Libraries for Sequencing"
     * should have this attached protocol.
     *      NOTE - Repooling by Volume OR Mass should do so by setting status to "PRE_SEQUENCING_POOLING_OF_LIBRARIES"
     *
     * @param seqQc
     * @param qcStatus
     */
    private void repoolByPoolingProtocol(DataRecord seqQc, QcStatus qcStatus, DataRecordManager dataRecordManager, User user) {
        DataRecord[] childSamples = getParentsOfType(seqQc, SAMPLE, user);
        if (childSamples != null && childSamples.length > 0) {
            log.info(String.format("Found record %s. Searching for child sample with Protocol: %s",
                    recordId, POOLING_PROTOCOL));
            DataRecord record;
            while (childSamples != null && childSamples.length > 0) {
                record = childSamples[0];
                if (isRecordForRepooling(record, user)) {
                    String pooledSampleRecord = Long.toString(record.getRecordId());
                    log.info(String.format("Found sample, %s, with Protocol: %s", pooledSampleRecord, POOLING_PROTOCOL));
                    qcStatusAwareProcessAssigner.assign(dataRecordManager, user, record, qcStatus);
                    return;
                }
                childSamples = getChildrenOfType(record, SAMPLE, user);
            }
        }
        log.error(String.format("Failed to assign Repool Process for %s. No associated sample with %s",
                recordId, POOLING_PROTOCOL));
    }

    /**
     * Determines if the Sample DataRecord is the one that should have its status set. This is determiend by whether
     * record has a child type with @POOLING_PROTOCOL
     *
     * @param record
     * @return boolean
     */
    private boolean isRecordForRepooling(DataRecord record, User user) {
        DataRecord[] poolingSampleLibProtocol = getChildrenOfType(record, POOLING_PROTOCOL, user);
        return poolingSampleLibProtocol.length > 0;
    }

    /**
     * Returns a list of records that are children of the input record in the input table
     *
     * @param record
     * @param table
     * @return
     */
    private DataRecord[] getChildrenOfType(DataRecord record, String table, User user) {
        try {
            return record.getChildrenOfType(table, user);
        } catch (Exception e) {
            log.error(String.format("Error getting children from %s dataType for record %s. Error: %s",
                    table,
                    record.getRecordId(),
                    e.getMessage()));
        }
        return new DataRecord[0];
    }

    /**
     * Returns a list of records that are parents of the input record in the input table
     *
     * @param record
     * @param table
     * @return
     */
    private DataRecord[] getParentsOfType(DataRecord record, String table, User user) {
        try {
            List<DataRecord> dataRecords = record.getParentsOfType(table, user);
            DataRecord[] dataRecordsArray = new DataRecord[dataRecords.size()];
            return dataRecords.toArray(dataRecordsArray);
        } catch (IoError | RemoteException | ServerException e) {
            log.error(String.format("Error getting parents from %s dataType for record %s. Error: %s",
                    table,
                    record.getRecordId(),
                    e.getMessage()));
        }
        return new DataRecord[0];
    }
}