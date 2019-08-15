package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sapioutils.shared.managers.DataRecordUtilManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.util.Messages;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

/**
 * A queued task that takes a user sample id and service id, checks to see if that banked sample exists, if it does, deletes it.
 * NOTE: banked samples are not normally deleted. This service is explicitly for e2e testing to avoid polluting the database with test data
 *
 * @author Aaron Gabow
 */
public class DeleteBanked extends LimsTask {
    private static Log log = LogFactory.getLog(DeleteBanked.class);
    private String serviceId;
    private String userId;

    public void init(String userId, String serviceId) {
        this.userId = userId;
        this.serviceId = serviceId;
    }

    public void init(String serviceId) {
        this.serviceId = serviceId;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public Object execute(VeloxConnection conn) {
        if (serviceId != null && userId != null) {
            try {
                DataRecordUtilManager drum = new DataRecordUtilManager(managerContext);
                List<DataRecord> bankedList = dataRecordManager.queryDataRecords("BankedSample", "UserSampleID = '" + userId + "' AND ServiceId = '" + serviceId + "'", user);
                if (bankedList.size() == 0) {
                    throw new LimsException("No banked sample match that userId and serviceId");
                } else if (bankedList.size() > 1) {
                    throw new LimsException("More than one banked sample matches that userId and serviceId. Fix within the LIMS");
                } else {
                    drum.deleteRecords(bankedList, false);
                    dataRecordManager.storeAndCommit("Deleted the banked sample " + userId, user);
                }
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                log.info(e.getMessage() + " TRACE: " + sw.toString());
                return Messages.ERROR_IN + " DELETING BANKED SAMPLE: " + e.toString() + ": " + e.getMessage();
            }
            return Messages.SUCCESS;
        } else if (serviceId != null) {
            try {
                String requestId = "";
                List<DataRecord> bankedList = dataRecordManager.queryDataRecords("BankedSample", "ServiceId = '" + serviceId + "'", user);
                if (bankedList.size() == 0) {
                    throw new LimsException("No banked sample match that userId and serviceId");
                } else {
                    try {
                        requestId = bankedList.get(0).getStringVal("RequestId", user);
                    } catch (Exception e) {
                    }
                }
                DataRecordUtilManager drum = new DataRecordUtilManager(managerContext);
                if (requestId.equals("")) {
                    drum.deleteRecords(bankedList, false);
                } else {
                    List<DataRecord> requestList = dataRecordManager.queryDataRecords("Request", "RequestId = '" + requestId + "'", user);

                    if (requestList.size() == 0 && !requestId.equals("")) {
                        throw new LimsException("No request has that request id: " + requestId);
                    } else if (requestList.size() > 1) {
                        throw new LimsException("More than one request matches that request id: " + requestId);
                    }

                    List<DataRecord> ancestorProjectList = requestList.get(0).getAncestorsOfType("Project", user);
                    List<DataRecord> childSampleList = Arrays.asList(requestList.get(0).getChildrenOfType("Sample", user));
                    List<DataRecord> descendantSampleList = requestList.get(0).getDescendantsOfType("Sample", user);
                    if (childSampleList.size() != descendantSampleList.size()) {
                        throw new LimsException("Can only delete requests if the children samples have no descendants");
                    }
                    List<DataRecord> descendantCmoList = requestList.get(0).getDescendantsOfType("SampleCMOInfoRecords", user);
                    List<DataRecord> descendantSeqReqList = requestList.get(0).getDescendantsOfType("SeqRequirement", user);
                    List<DataRecord> descendantBarcodeList = requestList.get(0).getDescendantsOfType("IndexBarcode", user);
                    drum.deleteRecords(descendantCmoList, false);
                    drum.deleteRecords(descendantSeqReqList, false);
                    drum.deleteRecords(descendantBarcodeList, false);
                    drum.deleteRecords(childSampleList, false);
                    drum.deleteRecords(requestList, false);
                    drum.deleteRecords(ancestorProjectList, false);
                    drum.deleteRecords(bankedList, false);
                }
                dataRecordManager.storeAndCommit("Deleted all related service", user);
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                log.info(e.getMessage() + " TRACE: " + sw.toString());
                return Messages.ERROR_IN + " DELETING REQUEST: " + e.toString() + ": " + e.getMessage();
            }
            return Messages.SUCCESS;
        } else {
            return Messages.FAILURE_IN + " Delete";
        }
    }
}