package org.mskcc.limsrest.service.assignedprocess;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_AssignedProcess;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_Sample;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

public class AssignedProcessCreator {
    private final static Log log = LogFactory.getLog(AssignedProcessCreator.class);
    public static Map<String, Object> create(DataRecord sample, AssignedProcess assignedProcess, boolean isSetInvestigatorDecision, User user) throws Exception {
        Map<String, Object> assignedProcessMap = new HashMap<>();
        log.info("Inside create function!");
        String igoSampleId = sample.getStringVal(DT_Sample.SAMPLE_ID, user);
        String cmoSampleId = sample.getStringVal(DT_Sample.OTHER_SAMPLE_ID, user);

        assignedProcessMap.put(DT_AssignedProcess.SAMPLE_ID, igoSampleId);
        assignedProcessMap.put(DT_AssignedProcess.PROCESS_STEP_NUMBER, assignedProcess.getStepNumber());
        assignedProcessMap.put(DT_AssignedProcess.PROCESS_NAME, assignedProcess.getName());
        assignedProcessMap.put(DT_AssignedProcess.OTHER_SAMPLE_ID, cmoSampleId);
        assignedProcessMap.put(DT_AssignedProcess.STATUS, assignedProcess.getStatus());
        assignedProcessMap.put(DT_AssignedProcess.SAMPLE_RECORD_ID, sample.getRecordId());
        assignRequestRecordId(assignedProcessMap, sample, isSetInvestigatorDecision, user);
        log.info("Populated the map!");
        return assignedProcessMap;
    }

    private static void assignRequestRecordId(Map<String, Object> assignedProcessMap, DataRecord sample, boolean isSetInvestigatorDecision, User user) throws
            NotFound, RemoteException {
        log.info("In assignRequestRecordId");
        String reqRecordIdsString;
        if (isSetInvestigatorDecision) {
            reqRecordIdsString = sample.getDataField(DT_Sample.RECORD_ID, user).toString();
        }
        else {
            reqRecordIdsString = sample.getStringVal(DT_Sample.REQUEST_RECORD_ID_LIST, user);
        }

        String[] reqRecordIds = reqRecordIdsString.split(",");

        if (reqRecordIds.length > 0)
            assignedProcessMap.put(DT_AssignedProcess.REQUEST_RECORD_ID, reqRecordIds[0]);
    }
}
