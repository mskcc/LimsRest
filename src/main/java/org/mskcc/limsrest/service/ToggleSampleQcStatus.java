package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.InvalidValue;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.PoolingSampleLibProtocolModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.service.assignedprocess.QcStatus;
import org.mskcc.limsrest.service.assignedprocess.QcStatusAwareProcessAssigner;
import org.springframework.security.access.prepost.PreAuthorize;

import java.rmi.RemoteException;
import java.util.List;

import static org.mskcc.util.VeloxConstants.SAMPLE;

/**
 * A queued task that takes a request id and returns all some request information and some sample information
 *
 * @author Aaron Gabow
 */
public class ToggleSampleQcStatus extends LimsTask {
    // Pooling protocol that is used to identify sample that should be re-pooled
    private final static String POOLING_PROTOCOL = PoolingSampleLibProtocolModel.DATA_TYPE_NAME;
    private static Log log = LogFactory.getLog(ToggleSampleQcStatus.class);
    protected QcStatusAwareProcessAssigner qcStatusAwareProcessAssigner = new QcStatusAwareProcessAssigner();
    boolean isSeqAnalysisSampleqc = true; // which LIMS table to update
    long recordId;
    String status;
    String requestId;
    String sampleId;
    String correctedSampleId;
    String run;
    String analyst;
    String note;
    String fastqPath;

    protected static void setSeqAnalysisSampleQcStatus(DataRecord seqQc, QcStatus qcStatus, String status, User user)
            throws IoError, InvalidValue, NotFound, RemoteException {
        if (qcStatus == QcStatus.IGO_COMPLETE) {
            seqQc.setDataField("PassedQc", Boolean.TRUE, user);
            seqQc.setDataField("SeqQCStatus", QcStatus.PASSED.getText(), user);
            seqQc.setDataField("DateIgoComplete", System.currentTimeMillis(), user);
        } else {
            seqQc.setDataField("SeqQCStatus", status, user);
            seqQc.setDataField("PassedQc", Boolean.FALSE, user);
        }
    }

    public void init(long recordId, String status, String requestId, String correctedSampleId, String run, String qcType, String analyst, String note, String fastqPath) {
        this.recordId = recordId;
        this.status = status;
        this.requestId = requestId;
        this.correctedSampleId = (correctedSampleId == null) ? "" : correctedSampleId;
        this.run = run;
        this.analyst = analyst;
        this.note = note;
        this.fastqPath = fastqPath;
        if ("Post".equals(qcType)) {
            isSeqAnalysisSampleqc = false;
        }
    }

    @PreAuthorize("hasRole('USER')")
    @Override
    public Object execute(VeloxConnection conn) {
        // designed to update either SeqAnalysisSampleQC or PostSeqAnalysisQC LIMS status tables
        try {
            if (isSeqAnalysisSampleqc) {
                log.info("SeqAnalysisSampleQC updating to " + status + " for record:" + recordId);
                DataRecord seqQc = dataRecordManager.querySystemForRecord(recordId, "SeqAnalysisSampleQC", user);

                String currentStatusLIMS = (String) seqQc.getDataField("SeqQCStatus", user);
                // Failed status is terminal on Qc site and can't be changed.
                if (QcStatus.FAILED.getText().equals(currentStatusLIMS)) {
                    log.info("QC status already failed and can't be updated from there via REST interface.");
                    return QcStatus.FAILED;
                }

                QcStatus qcStatus = QcStatus.fromString(status);
                setSeqAnalysisSampleQcStatus(seqQc, qcStatus, status, user);

                if (qcStatus == QcStatus.RESEQUENCE_POOL) {
                    qcStatusAwareProcessAssigner.assign(dataRecordManager, user, seqQc, qcStatus);
                } else if (qcStatus == QcStatus.REPOOL_SAMPLE) {
                    repoolByPoolingProtocol(seqQc, qcStatus);
                }
                dataRecordManager.storeAndCommit("SeqAnalysisSampleQC updated to " + status, null, user);

                log.info("SeqAnalysisSampleQC updated to:" + status + " from:" + currentStatusLIMS);
            } else {
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
                    if (cmoInfo.length > 0 && cmoInfoCheck(correctedSampleId, cmoInfo[0])) {
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

    /**
     * Searches for record w/ @POOLING_PROTOCOL and assigns process based on that record. Samples with both Statuses
     * of "Ready for - Pooling of Sample Libraries by Volume" & "Ready for - Pooling of Sample Libraries for Sequencing"
     * should have this attached protocol.
     * NOTE - Repooling by Volume OR Mass should do so by setting status to "PRE_SEQUENCING_POOLING_OF_LIBRARIES"
     *
     * @param seqQc
     * @param qcStatus
     */
    private void repoolByPoolingProtocol(DataRecord seqQc, QcStatus qcStatus) {
        DataRecord[] childSamples = getParentsOfType(seqQc, SAMPLE);
        if (childSamples != null && childSamples.length > 0) {
            log.info(String.format("Found record %s. Searching for child sample with Protocol: %s",
                    recordId, POOLING_PROTOCOL));
            DataRecord record;
            while (childSamples != null && childSamples.length > 0) {
                record = childSamples[0];
                if (isRecordForRepooling(record)) {
                    String pooledSampleRecord = Long.toString(record.getRecordId());
                    log.info(String.format("Found sample, %s, with Protocol: %s", pooledSampleRecord, POOLING_PROTOCOL));
                    qcStatusAwareProcessAssigner.assign(dataRecordManager, user, record, qcStatus);
                    return;
                }
                childSamples = getChildrenOfType(record, SAMPLE);
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
    private boolean isRecordForRepooling(DataRecord record) {
        DataRecord[] poolingSampleLibProtocol = getChildrenOfType(record, POOLING_PROTOCOL);
        return poolingSampleLibProtocol.length > 0;
    }

    /**
     * Returns a list of records that are children of the input record in the input table
     *
     * @param record
     * @param table
     * @return
     */
    private DataRecord[] getChildrenOfType(DataRecord record, String table) {
        try {
            return record.getChildrenOfType(table, user);
        } catch (IoError | RemoteException e) {
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
    private DataRecord[] getParentsOfType(DataRecord record, String table) {
        try {
            List<DataRecord> dataRecords = record.getParentsOfType(table, user);
            DataRecord[] dataRecordsArray = new DataRecord[dataRecords.size()];
            return dataRecords.toArray(dataRecordsArray);
        } catch (IoError | RemoteException e) {
            log.error(String.format("Error getting parents from %s dataType for record %s. Error: %s",
                    table,
                    record.getRecordId(),
                    e.getMessage()));
        }
        return new DataRecord[0];
    }
}