package org.mskcc.limsrest.service;

import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.api.util.IoUtil;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.RequestModel;
import com.velox.sloan.cmo.recmodels.SeqAnalysisSampleQCModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.util.Utils;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.*;

/**
 * A queued task that finds all requests that have all qc in a status of Failed or Passed but haven't been delivered
 * or that are being redelivered within a window
 * @author David Streid (Aaron Gabow)
 */
public class GetUndeliveredProjectsTask extends LimsTask {
    private static Log log = LogFactory.getLog(GetUndeliveredProjectsTask.class);
    private Integer daysToExamine;

    public GetUndeliveredProjectsTask(Integer daysToExamine){
        this.daysToExamine = daysToExamine;
    }

    @PreAuthorize("hasRole('READ')")
    @Override
    public List<RequestSummary> execute(VeloxConnection conn) {
        // Set<DataRecord> undeliveredSet = new HashSet<>();
        User user = conn.getUser();

        List<DataRecord> undeliveredRecords = new ArrayList<>();
        String query = String.format("%s IS NULL OR %s >  UNIX_TIMESTAMP(NOW() - INTERVAL %d DAY) * 1000",
            RequestModel.RECENT_DELIVERY_DATE, RequestModel.RECENT_DELIVERY_DATE, this.daysToExamine);
        try {
            undeliveredRecords = conn.getDataRecordManager().queryDataRecords(RequestModel.DATA_TYPE_NAME, query, user);
        } catch (NotFound | RemoteException | IoError e){
            log.error("Failed to retrieve Projects from Database");
        }

        /*
        AuditLog auditLog = null;
        try {
            auditLog = user.getAuditLog();
        } catch(RemoteException e){
            log.error("Failed to get auditLog");
        }
        boolean reviewComplete, hasQc, requiresDelivery;
        long deliveryDate;
        log.info(String.format("Retrieving data on %d projects", undeliveredList.size()));
        for (DataRecord undeliveredProject : undeliveredList) {
            reviewComplete = true;
            hasQc = false;                  // Only oneSeqAnalysisSampleQCModel descendant is required to "have QC"
            requiresDelivery = false;

            deliveryDate = Utils.getRecordLongValue(undeliveredProject, RequestModel.RECENT_DELIVERY_DATE, user);
            String requestId = Utils.getRecordStringValue(undeliveredProject, RequestModel.REQUEST_ID, user);
            List<DataRecord> qcs = Utils.getDescendentsOfDataRecord(undeliveredProject, SeqAnalysisSampleQCModel.DATA_TYPE_NAME, user);
            for (DataRecord qc : qcs) {
                String seqQcDescendant = Utils.getRecordStringValue(qc, "RequestId", user);
                hasQc = hasQc || requestId.equals(seqQcDescendant);
                String seqQcStatusPickListVal = Utils.getRecordPickListVal(qc,SeqAnalysisSampleQCModel.SEQ_QCSTATUS, user);
                if (!SEQ_QC_STATUS_PASSED.equals(seqQcStatusPickListVal) &&
                    !SEQ_QC_STATUS_FAILED.equals(seqQcStatusPickListVal)) {
                    reviewComplete = false;
                } else {
                    List<AuditLogEntry> qcHistory = new ArrayList<>();
                    if(auditLog != null) {
                        try {
                            qcHistory = auditLog.getAuditLogHistory(qc, false, user);
                        } catch (IoError | RemoteException e) {
                            log.error(String.format("Failed to get audit log on Qc Record: %d", qc.getRecordId()));
                        }
                        if (qcHistory.size() > 0 && qcHistory.get(0).timestamp > deliveryDate) {
                            requiresDelivery = true;
                        }
                    }
                }
            }
            if (reviewComplete && hasQc && requiresDelivery) {
                undeliveredSet.add(undeliveredProject);
            }
        }
         */

        // Transform requests into a redacted API response
        List<RequestSummary> undeliveredRequests = new LinkedList<>();
        for (DataRecord request : undeliveredRecords) {
            Map<String, Object> requestFields = new HashMap<>();
            try {
                 requestFields = request.getFields(user);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
            }
            String requestId = (String) requestFields.get(RequestModel.REQUEST_ID);
            RequestSummary rs = new RequestSummary(requestId);
            rs.setInvestigator((String) requestFields.computeIfAbsent(RequestModel.INVESTIGATOR, k -> ""));
            rs.setPi((String) requestFields.computeIfAbsent(RequestModel.LABORATORY_HEAD, k -> ""));
            rs.setAnalysisRequested((Boolean) requestFields.computeIfAbsent(RequestModel.BICANALYSIS, k -> Boolean.FALSE));
            rs.setRequestType((String) requestFields.computeIfAbsent(RequestModel.REQUEST_NAME, k -> ""));
            rs.setProjectManager((String) requestFields.computeIfAbsent(RequestModel.PROJECT_MANAGER, k -> ""));
            rs.setSampleNumber(((Short) requestFields.computeIfAbsent(RequestModel.SAMPLE_NUMBER, k->0)).intValue());
            undeliveredRequests.add(rs);
        }

        return undeliveredRequests;
    }
}