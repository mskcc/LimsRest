package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.RequestModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import static org.mskcc.limsrest.util.Utils.getRecordLongValue;
import static org.mskcc.limsrest.util.Utils.getRecordStringValue;
import static org.mskcc.limsrest.util.StatusTrackerConfig.isIgoComplete;

public class GetIgoRequestsTask extends LimsTask {
    private static Log log = LogFactory.getLog(GetIgoRequestsTask.class);

    private Long days;
    private Boolean igoComplete;

    public GetIgoRequestsTask(Long days, Boolean igoComplete) {
        this.days = days;
        this.igoComplete = igoComplete;
    }

    private long getSearchPoint() {
        long now = System.currentTimeMillis();
        long offset = days * 24 * 60 * 60 * 1000;
        return now - offset;
    }

    /**
     * Returns the query to use to retrieve the IGO request
     *
     * NOTE - This should be in sync w/ @StatusTrackerConfig::isIgoComplete. If changing this, uncomment
     * @getIgoRequestsTask_matchesIsIgoCompleteUtil_* tests in GetIgoRequestsTaskTest
     * @return
     */
    private String getQuery() {
        long searchPoint = getSearchPoint();
        if (this.igoComplete) {
            /** IGO completion Criteria:
             *      1) Request w/ RECENT_DELIVERY_DATE date (Sequencing project marked for delivery)
             *      2) Extraction Request having a COMPLETED_DATE (all other requests)
             */
            return String.format("%s > %d OR (%s > %d AND lower(%s) LIKE '%%extraction%%')",
                    RequestModel.RECENT_DELIVERY_DATE, searchPoint,
                    RequestModel.COMPLETED_DATE, searchPoint, RequestModel.REQUEST_NAME);
        }
        /** IGO incomplete Criteria:
         *      1) No RECENT_DELIVERY_DATE (Sequencing project NOT marked for delivery)
         *      2) Extraction requests w/o a COMPLETED_DATE
         * NOTE: DATE_CREATED is used because sometimes ReceivedDate can be null
         */
        return String.format("%s > %d AND NOT ((%s IS NOT NULL) OR (%s IS NOT NULL AND %s LIKE '%%extraction%%'))",
                RequestModel.DATE_CREATED, searchPoint,
                RequestModel.RECENT_DELIVERY_DATE,
                RequestModel.COMPLETED_DATE,
                RequestModel.REQUEST_NAME);
    }

    @PreAuthorize("hasRole('READ')")
    @Override
    public List<RequestSummary> execute(VeloxConnection conn) {
        User user = conn.getUser();
        String query = getQuery();
        List<DataRecord> records = new ArrayList<>();
        try {
            records = conn.getDataRecordManager().queryDataRecords(RequestModel.DATA_TYPE_NAME, query, user);
        } catch (IoError | RemoteException | NotFound e) {
            log.error(String.format("Failed to query DataRecords w/ query: %s", query));
            return new ArrayList<>();
        }

        // Transform requests into a redacted API response
        List<RequestSummary> requests = new ArrayList<>();

        for (DataRecord request : records) {
            String requestId = getRecordStringValue(request, RequestModel.REQUEST_ID, user);
            RequestSummary rs = new RequestSummary(requestId);
            rs.setInvestigator(getRecordStringValue(request, RequestModel.INVESTIGATOR, user));
            rs.setPi(getRecordStringValue(request, RequestModel.LABORATORY_HEAD, user));
            rs.setRequestType(getRecordStringValue(request, RequestModel.REQUEST_NAME, user));
            rs.setReceivedDate(getRecordLongValue(request, RequestModel.RECEIVED_DATE, user));
            rs.setRecentDeliveryDate(getRecordLongValue(request, RequestModel.RECENT_DELIVERY_DATE, user));
            rs.setCompletedDate(getRecordLongValue(request, RequestModel.COMPLETED_DATE, user));
            rs.setIsIgoComplete(isIgoComplete(request, user));
            requests.add(rs);
        }

        return requests;
    }
}
