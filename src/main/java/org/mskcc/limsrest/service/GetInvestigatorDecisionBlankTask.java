package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.util.IGOTools;

import java.rmi.RemoteException;
import java.util.*;

/**
 * Run LIMS queries for tables with blank investigator decisions.
 */
public class GetInvestigatorDecisionBlankTask {
    private static Log log = LogFactory.getLog(GetInvestigatorDecisionBlankTask.class);
    private ConnectionLIMS conn;

    public GetInvestigatorDecisionBlankTask(ConnectionLIMS conn) {
        this.conn = conn;
    }

    public HashMap<String,String> execute() {
        try {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            DataRecordManager drm = vConn.getDataRecordManager();
            HashMap<String,String> emptyDecision = new HashMap<>();

            Long oneYearAgo = System.currentTimeMillis() - 31556952000L; // only include samples created in the last year
            String query = "INVESTIGATORDECISION IS NULL OR INVESTIGATORDECISION = '' AND DATECREATED > " + oneYearAgo.toString();
            log.info("Querying LIMS:  " + query);

            List<DataRecord> requestList1 = drm.queryDataRecords("QcReportDna", query, user);
            addToResult(user, emptyDecision, requestList1, "DNA Report");

            List<DataRecord> requestList2 = drm.queryDataRecords("QcReportRna", query, user);
            addToResult(user, emptyDecision, requestList2, "RNA Report");

            List<DataRecord> requestList3 = drm.queryDataRecords("QcReportLibrary", query, user);
            addToResult(user, emptyDecision, requestList3, "Library Report");

            return emptyDecision;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private void addToResult(User user, HashMap<String, String> emptyDecision, List<DataRecord> requestList,
                             String reportName) throws NotFound, RemoteException {
        for (DataRecord request : requestList) {
            String sampleId = request.getStringVal("SampleId", user);
            // remove controls & pools like 'FROZENPOOLEDNORMAL'...
            if (sampleId == null || sampleId.length() == 0 || !Character.isDigit((sampleId.charAt(0))))
                continue;
            String requestId = IGOTools.requestFromIgoId(sampleId);
            if (requestId != null && requestId.length() > 0)
                emptyDecision.put(requestId, reportName);
        }
    }
}