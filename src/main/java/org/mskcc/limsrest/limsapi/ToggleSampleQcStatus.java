package org.mskcc.limsrest.limsapi;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.InvalidValue;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.limsapi.assignedprocess.QcStatus;
import org.mskcc.limsrest.limsapi.assignedprocess.QcStatusAwareProcessAssigner;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.rmi.RemoteException;
import java.util.List;

/**
 * A queued task that takes a request id and returns all some request information and some sample information
 *
 * @author Aaron Gabow
 */
@Service
public class ToggleSampleQcStatus extends LimsTask {
    private Log log = LogFactory.getLog(ToggleSampleQcStatus.class);

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

    private QcStatusAwareProcessAssigner qcStatusAwareProcessAssigner = new QcStatusAwareProcessAssigner();

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

                if (qcStatus == QcStatus.RESEQUENCE_POOL || qcStatus == QcStatus.REPOOL_SAMPLE)
                    qcStatusAwareProcessAssigner.assign(dataRecordManager, user, seqQc, qcStatus);

                dataRecordManager.storeAndCommit("SeqAnalysisSampleQC updated to " + status, user);
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
                    } catch (NullPointerException npe){}
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
                                } catch (NullPointerException npe){}
                                pqc.setDataField("Note", oldNote + "\n" + note, user);
                            }
                        }
                    } catch (NullPointerException npe){}
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

    protected static void setSeqAnalysisSampleQcStatus(DataRecord seqQc, QcStatus qcStatus, String status, User user)
            throws IoError, InvalidValue, NotFound, RemoteException {
        if (qcStatus == qcStatus.IGO_COMPLETE) {
            seqQc.setDataField("PassedQc", Boolean.TRUE, user);
            seqQc.setDataField("SeqQCStatus", QcStatus.PASSED.getText(), user);
            seqQc.setDataField("DateIgoComplete", System.currentTimeMillis(), user);
        } else {
            seqQc.setDataField("SeqQCStatus", status, user);
            seqQc.setDataField("PassedQc", Boolean.FALSE, user);
        }
    }
}