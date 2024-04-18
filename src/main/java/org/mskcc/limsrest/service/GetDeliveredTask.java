package org.mskcc.limsrest.service;

import com.velox.api.datarecord.AuditLog;
import com.velox.api.datarecord.AuditLogEntry;
import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.api.util.SortedImmutableList;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.RequestModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.util.Utils;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.*;

import static org.mskcc.limsrest.util.Utils.*;

/**
 * A queued task that finds all requests that have been delivered in a time frame and finds all their samples,
 * noting whether they have been delivered, failed, require more sequencing or never made it to the sequencer
 * 
 * @author Aaron Gabow
 */
public class GetDeliveredTask {
    private static Log log = LogFactory.getLog(GetDeliveredTask.class);
    private ConnectionLIMS conn;

    private final String ignoreTerm = "Under-Review";
    private long time = 0;
    private String units = "";
    private String investigator = "";

    public GetDeliveredTask(ConnectionLIMS conn) {
        this.conn = conn;
    }

    public void init(int time, String units) {
        this.time = time;
        this.units = units;
        this.investigator = "NULL";
    }

    public void init(String investigator) {
        this.investigator = investigator;
        this.time = 2;
        this.units = "w";
    }

    public void init(String investigator, int time, String units) {
        this.investigator = investigator;
        this.time = time;
        this.units = units;
    }

    public void init() {
        this.time = -1;
        this.units = "w";
    }

    @PreAuthorize("hasRole('READ')")
    public Object execute() {
        VeloxConnection vConn = conn.getConnection();
        DataRecordManager dataRecordManager = vConn.getDataRecordManager();
        User user = vConn.getUser();

        long now = System.currentTimeMillis();
        long offset = time;
        if (units.equals("m")) {
            offset *= 60l * 1000;
        } else if (units.equals("h")) {
            offset *= 60l * 60 * 1000;
        } else if (units.equals("w")) {
            offset *= 7l * 24 * 60 * 60 * 1000;
        } else { //don't worry about anything exotic and just assume it is days
            offset *= 24l * 60 * 60 * 1000;
        }
        long searchPoint = now - offset;
        if (searchPoint < 1) {
            searchPoint = 1l;
        }
        //find all runs
        List<RequestSummary> delivered = new LinkedList<>();
        try {
            AuditLog auditlog = user.getAuditLog();
            List<DataRecord> recentDeliveries;
            if (time == -1) {
                searchPoint = now + offset;
                List<DataRecord> unreviewed = dataRecordManager.queryDataRecords("SeqAnalysisSampleQC", "DateCreated >  1484508629000 AND SeqQCStatus != 'Passed' AND SeqQCStatus not like 'Failed%'", user);
                List<List<DataRecord>> parentSamples = dataRecordManager.getParentsOfType(unreviewed, "Sample", user);
                List<String> reqIds = new LinkedList<>();
                for (List<DataRecord> samples : parentSamples) {
                    for (DataRecord sample : samples) {
                        try {
                            String possibleReqId = sample.getStringVal("RequestId", user);
                            if (!reqIds.contains(possibleReqId)) {
                                log.info("Adding " + possibleReqId);
                                reqIds.add(possibleReqId);
                            }
                        } catch (NullPointerException npe) {
                        }
                    }
                }
                recentDeliveries = new LinkedList<>();
                for (String rid : reqIds) {
                    recentDeliveries.addAll(dataRecordManager.queryDataRecords("Request", "RequestId = '" + rid + "'", user));
                }
            } else if (investigator.equals("NULL")) {
                recentDeliveries = dataRecordManager.queryDataRecords("Request", "RecentDeliveryDate > " + searchPoint, user);
            } else {
                recentDeliveries = dataRecordManager.queryDataRecords("Request", "RecentDeliveryDate > " + searchPoint + " AND (Investigatoremail = '" + investigator + "' OR LabHeadEmail = '" + investigator + "')", user);
            }
            SortedImmutableList<DataRecord> immutableList = new SortedImmutableList<>(recentDeliveries);
            List<List<DataRecord>> childSamples = dataRecordManager.getChildrenOfType(immutableList, "Sample", user);
            List<List<DataRecord>> childPlates = dataRecordManager.getChildrenOfType(immutableList, "Plate", user);
            for (int i = 0; i < immutableList.size(); i++) {
                DataRecord request = immutableList.get(i);
                Map<String, Object> requestFields = request.getFields(user);
                String requestId = (String) requestFields.get("RequestId");
                log.info("Processing RequestId:" + requestId);
                RequestSummary rs = new RequestSummary(requestId);

                rs.setInvestigator(getRecordStringValue(request, RequestModel.INVESTIGATOR, user));
                rs.setPi(getRecordStringValue(request, RequestModel.LABORATORY_HEAD, user));
                rs.setAnalysisRequested(getRecordBooleanValue(request, RequestModel.BICANALYSIS, user));
                rs.setAnalysisType(getRecordStringValue(request, "AnalysisType", user));
                rs.setRequestType(getRecordStringValue(request, RequestModel.REQUEST_NAME, user));
                rs.setProjectManager(getRecordStringValue(request, RequestModel.PROJECT_MANAGER, user));
                //rs.setSampleNumber(getRecordShortValue(request, RequestModel.SAMPLE_NUMBER, user));
                //rs.setReceivedDate(getRecordLongValue(request, RequestModel.RECEIVED_DATE, user));

                List<DataRecord> childrenOfRequest = childSamples.get(i);
                List<DataRecord> childrenPlatesOfRequest = childPlates.get(i);
                List<List<DataRecord>> childPlateSamples = dataRecordManager.getChildrenOfType(new SortedImmutableList<>(childrenPlatesOfRequest), "Sample", user);
                for (List<DataRecord> plateSamples : childPlateSamples) {
                    for (DataRecord plateSamp : plateSamples) {
                        try {
                            if (requestId.equals(plateSamp.getStringVal("RequestId", user))) {
                                childrenOfRequest.add(plateSamp);
                            }
                        } catch (NullPointerException npe) {
                        }
                    }
                }

                List<List<DataRecord>> sampleQcs = dataRecordManager.getDescendantsOfType(new SortedImmutableList<>(childrenOfRequest), "SeqAnalysisSampleQC", user);
                List<List<Map<String, Object>>> allCorrectedFields = dataRecordManager.getFieldsForChildrenOfType(new SortedImmutableList<>(childrenOfRequest), "SampleCMOInfoRecords", user);
                List<AuditLogEntry> reqHistory = auditlog.getAuditLogHistory(request, false, user);
                for (AuditLogEntry logline : reqHistory) {
                    if (logline.dataFieldName.equals("RecentDeliveryDate")) {
                        rs.addDeliveryDate(logline.timestamp);
                    }
                }

                for (int j = 0; j < childrenOfRequest.size(); j++) {
                    DataRecord sample = childrenOfRequest.get(j);
                    Map<String, Object> sampleFields = sample.getFields(user);
                    Map<String, Object> correctedFields = null;
                    if (allCorrectedFields != null && allCorrectedFields.size() > 0 && allCorrectedFields.get(j).size() > 0) {
                        correctedFields = allCorrectedFields.get(j).get(0);
                    } else {
                        correctedFields = new HashMap<>();
                        correctedFields.put("CorrectdCMOID", "");
                    }
                    SampleSummary ss = new SampleSummary();
                    String sampleRequestId = "";
                    try {
                        sampleRequestId = (String) sampleFields.get("RequestId");
                        ss.addRequest(sampleRequestId);
                    } catch (NullPointerException npe) {
                    }
                    log.debug(Utils.getBaseSampleId((String) sampleFields.get("SampleId"))); // this will takeover the log for projects w/many samples
                    ss.addBaseId(Utils.getBaseSampleId((String) sampleFields.get("SampleId")));
                    ss.addCmoId((String) sampleFields.get("OtherSampleId"));
                    try {
                        ss.setSpecies((String) sampleFields.get("Species"));
                    } catch (NullPointerException npe) {
                    }
                    try {
                        ss.addExpName((String) sampleFields.get("UserSampleID"));
                    } catch (NullPointerException npe) {
                    }
                    try {
                        ss.setRecipe((String) sampleFields.get("Recipe"));
                    } catch (NullPointerException npe) {
                    }
                    ss.setCorrectedCmoId((String) correctedFields.get("CorrectedCMOID"));

                    //because a child of the sample can enter a new request, we want to assure that this is the correct request
                    //if there are errors err on overreporting
                    List<DataRecord> sampleQcsForSample = sampleQcs.get(j);
                    for (int k = 0; k < sampleQcsForSample.size(); k++) {
                        DataRecord qc = sampleQcsForSample.get(k);

                        Map<String, Object> qcFields = qc.getFields(user);
                        String qcRequestId = "";
                        try {
                            qcRequestId = (String) qcFields.get("Request");
                        } catch (NullPointerException npe) {
                        }
                        if (sampleRequestId.equals(qcRequestId) || qcRequestId == null || qcRequestId.equals("")) {
                            BasicQc qcSummary = new BasicQc();
                            try {
                                qcSummary.setRun((String) qcFields.get("SequencerRunFolder"));
                            } catch (NullPointerException npe) {
                            }
                            try {
                                qcSummary.setSampleName((String) qcFields.get("OtherSampleId"));
                            } catch (NullPointerException npe) {
                            }
                            try {
                                qcSummary.setTotalReads((Long) qcFields.get("TotalReads"));
                            } catch (NullPointerException npe) {
                            }
                            try {
                                qcSummary.setAlias((String) qcFields.get("Alias"));
                            } catch (NullPointerException npe) {
                            }
                            qcSummary.setCreateDate((Long) qcFields.get("DateCreated"));
                            String qcStatus = (String) qcFields.get("SeqQCStatus");
                            if (qcStatus != null) {
                                qcSummary.setQcStatus(qcStatus);
                                List<AuditLogEntry> qcHistory = auditlog.getAuditLogHistory(qc, false, user);
                                for (AuditLogEntry logline : qcHistory) {
                                    if (logline.dataFieldName.equals("SeqQCStatus")) {
                                        qcSummary.putStatusEvent(logline.timestamp, logline.newValue);
                                    }
                                }
                            }
                            ss.addBasicQc(qcSummary);
                        } else {
                            log.info("Ignoring QC record for: " + qcRequestId);
                        }
                    }
                    rs.addSample(ss);
                }
                if (!delivered.contains(rs)) {
                    delivered.add(rs);
                }
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            RequestSummary errorRs = new RequestSummary();
            delivered.add(errorRs);
        }
        log.info("GetDelivered completed.");
        return delivered;
    }
}