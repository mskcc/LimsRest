package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.RequestModel;
import com.velox.sloan.cmo.recmodels.SeqAnalysisSampleQCModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.mskcc.limsrest.util.StatusTrackerConfig.isIgoComplete;
import static org.mskcc.limsrest.util.Utils.getRecordLongValue;
import static org.mskcc.limsrest.util.Utils.getRecordStringValue;

public class GetSequencingRequestsTask extends LimsTask {
    private static Log log = LogFactory.getLog(GetIgoRequestsTask.class);

    private Long days;
    private Boolean igoComplete;

    public GetSequencingRequestsTask(Long days, Boolean igoComplete) {
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
     * <p>
     * NOTE - This should be in sync w/ @StatusTrackerConfig::isIgoComplete. If changing this, uncomment
     *
     * @return
     * @getIgoRequestsTask_matchesIsIgoCompleteUtil_* tests in GetIgoRequestsTaskTest
     */
    private String getQuery() {
        long searchPoint = getSearchPoint();
        if (this.igoComplete) {
            return String.format("%s > %d AND PassedQc = TRUE", SeqAnalysisSampleQCModel.DATE_CREATED, searchPoint);
        }
        return String.format("%s > %d AND PassedQc = FALSE", SeqAnalysisSampleQCModel.DATE_CREATED, searchPoint);
    }

    @PreAuthorize("hasRole('READ')")
    @Override
    public List<RequestSummary> execute(VeloxConnection conn) {
        User user = conn.getUser();
        String query = getQuery();
        List<DataRecord> records = new ArrayList<>();
        try {
            records = conn.getDataRecordManager().queryDataRecords(SeqAnalysisSampleQCModel.DATA_TYPE_NAME, query, user);
        } catch (IoError | RemoteException | NotFound e) {
            log.error(String.format("Failed to query DataRecords w/ query: %s", query));
            return new ArrayList<>();
        }

        List<List<DataRecord>> parentSamples = new ArrayList<>();
        try {
            parentSamples = dataRecordManager.getParentsOfType(records, "Sample", user);
        } catch (ServerException | RemoteException e) {
            log.error("Failed to query Samples");
        }

        List<String> reqIds = new LinkedList<>();
        for (List<DataRecord> samples : parentSamples) {
            for (DataRecord sample : samples) {
                try {
                    String possibleReqId = getRecordStringValue(sample, RequestModel.REQUEST_ID, user);
                    if (!reqIds.contains(possibleReqId)) {
                        log.info("Adding " + possibleReqId);
                        reqIds.add(possibleReqId);
                    }
                } catch (NullPointerException npe) {
                }
            }
        }
        List<DataRecord> requestRecords = new LinkedList<>();
        for (String rid : reqIds) {
            try {
                requestRecords.addAll(dataRecordManager.queryDataRecords(RequestModel.DATA_TYPE_NAME,
                        "RequestId = '" + rid + "'", user));
            } catch (RemoteException | NotFound | IoError e) {
                log.error("Failed to query Samples");
            }
        }

        List<RequestSummary> requests = new ArrayList<>();

        for (DataRecord request : requestRecords) {
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
