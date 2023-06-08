package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.FlowCellLaneModel;
import com.velox.sloan.cmo.recmodels.IlluminaSeqExperimentModel;
import com.velox.sloan.cmo.recmodels.IlluminaSeqProtocolModel;
import com.velox.sloan.cmo.recmodels.RequestModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.mskcc.limsrest.util.StatusTrackerConfig.isIgoComplete;
import static org.mskcc.limsrest.util.Utils.*;

public class GetSequencingRequestsTask {
    private static Log log = LogFactory.getLog(GetSequencingRequestsTask.class);
    private Long days;          // Number of days since sequencing
    private Boolean delivered;  // Whether to determine requests that have been marked IGO-COMPLETE

    public GetSequencingRequestsTask(Long days, Boolean delivered) {
        this.days = days;
        this.delivered = delivered;
    }

    /**
     * Retrieves the sequencing run folders of the input Request DataRecord instance
     *
     *      Request > ... > IlluminaSeqProtocol
     *      IlluminaSeqProtocol -> IlluminaSeqExperiment
     *      IlluminaSeqExperiment::SequencerRunFolder
     *
     * @param drm
     * @param request
     * @param user
     * @return
     */
    private List<String> getSequencingFoldersOfRequest(DataRecordManager drm, DataRecord request, User user) {
        List<DataRecord> seqExperiments = getDescendantsOfType(request, IlluminaSeqProtocolModel.DATA_TYPE_NAME, user);
        List<String> experimentIds = seqExperiments.stream()
                .map((exp) -> {
                    Long expId = getRecordLongValue(exp, IlluminaSeqProtocolModel.EXPERIMENT_RECORD_ID, user);
                    return expId.toString(); })
                .distinct()
                .collect(Collectors.toList());
        String query = String.format("%s in (%s)", IlluminaSeqExperimentModel.RECORD_ID, String.join(",", experimentIds));
        List<DataRecord> illuminaSeqExperiments = new ArrayList<>();
        try {
            illuminaSeqExperiments = drm.queryDataRecords(IlluminaSeqExperimentModel.DATA_TYPE_NAME, query, user);
        } catch (IoError | RemoteException | NotFound | ServerException e) {
            log.error(String.format("Failed to query DataRecords w/ query: '%s' on %s", query,
                    IlluminaSeqExperimentModel.DATA_TYPE_NAME));
            return new ArrayList<>();
        }
        List<String> runFolders = illuminaSeqExperiments.stream()
                .map(seqExpRecord -> getRecordStringValue(seqExpRecord, IlluminaSeqExperimentModel.SEQUENCER_RUN_FOLDER, user))
                .filter(seqExpRecord -> {
                    return !"".equals(seqExpRecord);
                })
                .collect(Collectors.toList());

        return runFolders;
    }

    @PreAuthorize("hasRole('READ')")
    public List<RequestSummary> execute(VeloxConnection conn) {
        DataRecordManager drm = conn.getDataRecordManager();
        User user = conn.getUser();

        // query seqanalysissampleqc for samples in last n days awaiting top-up
        // subtract all request IDs that have status 'Under-Review' (already topped up)
        // query request tables for the remaining set of request IDs to get info for return type like PI

        try {
            String query = "DATECREATED > " + getSearchPoint() + " and SEQQCSTATUS NOT IN ('Failed', 'Passed')";
            log.info("Querying seqanalysissampleqc WHERE " + query);
            List<DataRecord> qcRecords = drm.queryDataRecords("seqanalysissampleqc", query, user);
            String queryUnderReview = "DATECREATED > " + getSearchPoint() + " and SEQQCSTATUS IN ('Under-Review')";
            List<DataRecord> qcUnderReview = drm.queryDataRecords("seqanalysissampleqc", queryUnderReview, user);

            HashSet<String> requestIDs = new HashSet<>();
            for (DataRecord qcRecord : qcRecords) {
                String request = qcRecord.getStringVal("Request", user);
                requestIDs.add(request);
            }
            for (DataRecord qcRecord : qcUnderReview) {
                String request = qcRecord.getStringVal("Request", user);
                requestIDs.remove(request);
            }

            log.info("Found requests that need to be topped up: " + requestIDs.toString());
            String s = "\"" + String.join("\", \"", requestIDs) + "\"";
            query = "REQUESTID IN (" + s + ")";
            log.info("Sending requests query: " + query);
            List<DataRecord> requestRecords = drm.queryDataRecords(RequestModel.DATA_TYPE_NAME, query, user);
            List<RequestSummary> requests = new ArrayList<>();
            for (DataRecord request : requestRecords) {
                String requestId = getRecordStringValue(request, RequestModel.REQUEST_ID, user);

                RequestSummary rs = new RequestSummary(requestId);
                //rs.setRunFolders(seqFolders); // TODO get recent run folder from qc query above
                rs.setInvestigator(getRecordStringValue(request, RequestModel.INVESTIGATOR, user));
                rs.setPi(getRecordStringValue(request, RequestModel.LABORATORY_HEAD, user));
                rs.setRequestType(getRecordStringValue(request, RequestModel.REQUEST_NAME, user));
                rs.setIsIgoComplete(isIgoComplete(request, user));
                requests.add(rs);
            }
            return requests;

        } catch (RemoteException | NotFound | IoError | ServerException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    private long getSearchPoint() {
        long now = System.currentTimeMillis();
        long offset = days * 24 * 60 * 60 * 1000;
        return now - offset;
    }

    /**
     * Returns the query to use to retrieve the samples that have been sequenced, which should all have a FlowCellLane
     * child record
     *
     * @return Query for FlowCellLaneModel
     */
    private String getFlowCellLaneQuery() {
        long searchPoint = getSearchPoint();
        return String.format("%s > %d", FlowCellLaneModel.DATE_CREATED, searchPoint);
    }

    /**
     * Returns the query to use to retrieve the IGO requests based on @delivered
     *
     * @return Request Query
     */
    private String getRequestQuery(String requestId) {
        if (this.delivered) {
            // If IGO-Complete, the recent-delivery date should not be null
            return String.format("%s = '%s' and %s IS NOT NULL", RequestModel.REQUEST_ID, requestId,
                    RequestModel.RECENT_DELIVERY_DATE);
        }
        // If incomplete, then there should not be a recent delivery date
        return String.format("%s = '%s' and %s IS NULL", RequestModel.REQUEST_ID, requestId, RequestModel.RECENT_DELIVERY_DATE);
    }
}