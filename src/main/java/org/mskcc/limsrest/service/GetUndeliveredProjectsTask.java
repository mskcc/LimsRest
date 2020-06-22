package org.mskcc.limsrest.service;

import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.RequestModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import java.rmi.RemoteException;
import java.util.*;

import static org.mskcc.limsrest.util.Utils.*;

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

        if(undeliveredRecords.size() == 0){
            log.error(String.format("No undelivered projects for the past %d days", this.daysToExamine));
            return new ArrayList<>();
        }
        
        // Transform requests into a redacted API response
        List<RequestSummary> undeliveredRequests = new LinkedList<>();
        for (DataRecord request : undeliveredRecords) {
            String requestId = getRecordStringValue(request, RequestModel.REQUEST_ID, user);
            RequestSummary rs = new RequestSummary(requestId);

            rs.setInvestigator(getRecordStringValue(request, RequestModel.INVESTIGATOR, user));
            rs.setPi(getRecordStringValue(request, RequestModel.LABORATORY_HEAD, user));
            rs.setAnalysisRequested(getRecordBooleanValue(request, RequestModel.BICANALYSIS, user));
            rs.setRequestType(getRecordStringValue(request, RequestModel.REQUEST_NAME, user));
            rs.setProjectManager(getRecordStringValue(request, RequestModel.PROJECT_MANAGER, user));
            rs.setSampleNumber(getRecordShortValue(request,RequestModel.SAMPLE_NUMBER, user));

            undeliveredRequests.add(rs);
        }

        return undeliveredRequests;
    }
}