package org.mskcc.limsrest.limsapi;

import com.velox.api.datarecord.AuditLog;
import com.velox.api.datarecord.AuditLogEntry;
import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A queued task that finds all requests that have all qc in a status of Failed or Passed but haven't been delivered
 * or that are being redelivered within a window
 * @author Aaron Gabow
 */
@Service
public class GetUndelivered extends LimsTask {
    private static Log log = LogFactory.getLog(GetDelivered.class);
    private String passed = "Passed";
    private String failed = "Failed";
    private String daysToExamine = "30"; //we need this to not look at all requests


    @PreAuthorize("hasRole('READ')")
    @Override
    public Object execute(VeloxConnection conn) {
        Set<DataRecord> undeliveredSet = new HashSet<>();
        try {
            AuditLog auditlog = user.getAuditLog();
            List<DataRecord> undelivered = dataRecordManager.queryDataRecords("Request", "DeliveryDate IS NULL OR DeliveryDate >  UNIX_TIMESTAMP(NOW() - INTERVAL " + daysToExamine + " DAY) * 1000", user);
            for (DataRecord undel : undelivered) {
                boolean reviewComplete = true;
                boolean hasQc = false;
                boolean requiresDelivery = false;
                long deliveryDate = 0L;
                try {
                    deliveryDate = undel.getLongVal("DeliveryDate", user);
                } catch (NullPointerException npe) {
                }

                String requestId = undel.getStringVal("RequestId", user);
                List<DataRecord> qcs = undel.getDescendantsOfType("SeqAnalysisSampleQC", user);
                for (DataRecord qc : qcs) {
                    if (requestId.equals(qc.getStringVal("RequestId", user))) {
                        hasQc = true;
                    }
                    if (!passed.equals(qc.getPickListVal("SeqQcStatus", user)) &&
                            !failed.equals(qc.getPickListVal("SeqQcStatus", user))) {
                        reviewComplete = false;
                    } else {
                        List<AuditLogEntry> qcHistory = auditlog.getAuditLogHistory(qc, false, user);
                        if (qcHistory.get(0).timestamp > deliveryDate) {
                            requiresDelivery = true;
                        }
                    }
                }
                if (reviewComplete && hasQc && requiresDelivery) {
                    undeliveredSet.add(undel);
                }
            }
        } catch (Throwable e) {
            log.info(e.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            log.info(e.getMessage() + " TRACE: " + sw.toString());
        }
        List<RequestSummary> undelivered = new LinkedList<>();
        for (DataRecord request : undeliveredSet) {
            try {
                Map<String, Object> requestFields = request.getFields(user);
                String requestId = (String) requestFields.get("RequestId");
                RequestSummary rs = new RequestSummary(requestId);
                try {
                    rs.setInvestigator((String) requestFields.get("Investigator"));
                } catch (Exception e) {
                }
                try {
                    rs.setPi((String) requestFields.get("LaboratoryHead"));
                } catch (Exception e) {
                }
                try {
                    rs.setAnalysisRequested((Boolean) requestFields.get("BICAnalysis"));
                } catch (Exception e) {
                }
                try {
                    rs.setRequestType((String) requestFields.get("RequestName"));
                } catch (Exception e) {
                }
                try {
                    rs.setProjectManager((String) requestFields.get("ProjectManager"));
                } catch (Exception e) {
                }
                try {
                    rs.setSampleNumber(((Short) requestFields.get("SampleNumber")).intValue());
                } catch (Exception e) {
                    log.info(e.getMessage());
                }
                undelivered.add(rs);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
            }
        }

        return undelivered;
    }
}