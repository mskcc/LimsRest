package org.mskcc.limsrest.limsapi.assignedprocess;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_AssignedProcess;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_Sample;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

public class AssignedProcessCreator {
    public static Map<String, Object> create(DataRecord sample, AssignedProcess assignedProcess, User user) throws Exception {
        Map<String, Object> assignedProcessMap = new HashMap<>();

        String igoSampleId = sample.getStringVal(DT_Sample.SAMPLE_ID, user);
        String cmoSampleId = sample.getStringVal(DT_Sample.OTHER_SAMPLE_ID, user);

        assignedProcessMap.put(DT_AssignedProcess.SAMPLE_ID, igoSampleId);
        assignedProcessMap.put(DT_AssignedProcess.PROCESS_STEP_NUMBER, assignedProcess.getStepNumber());
        assignedProcessMap.put(DT_AssignedProcess.PROCESS_NAME, assignedProcess.getName());
        assignedProcessMap.put(DT_AssignedProcess.OTHER_SAMPLE_ID, cmoSampleId);
        assignedProcessMap.put(DT_AssignedProcess.STATUS, assignedProcess.getStatus());
        assignedProcessMap.put(DT_AssignedProcess.SAMPLE_RECORD_ID, sample.getRecordId());
        assignRequestRecordId(assignedProcessMap, sample, user);

        return assignedProcessMap;
    }

    private static void assignRequestRecordId(Map<String, Object> assignedProcessMap, DataRecord sample, User user) throws
            NotFound, RemoteException {
        String reqRecordIdsString = sample.getStringVal(DT_Sample.REQUEST_RECORD_ID_LIST, user);
        String[] reqRecordIds = reqRecordIdsString.split(",");

        if (reqRecordIds.length > 0)
            assignedProcessMap.put(DT_AssignedProcess.REQUEST_RECORD_ID, reqRecordIds[0]);
    }
}
