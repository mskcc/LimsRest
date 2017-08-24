package org.mskcc.limsrest.limsapi.assignedprocess;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.user.User;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_AssignedProcess;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_Sample;
import org.mskcc.domain.AssignedProcess;
import org.mskcc.domain.Workflow;

import java.util.HashMap;
import java.util.Map;

public class AssignedProcessCreator {
    public Map<String, Object> create(DataRecord sample, AssignedProcess assignedProcess, User user) throws Exception {
        Map<String, Object> assignedProcessMap = new HashMap<>();

        String igoSampleId = sample.getStringVal(DT_Sample.SAMPLE_ID, user);
        String cmoSampleId = sample.getStringVal(DT_Sample.OTHER_SAMPLE_ID, user);
        String requestRecordIds = sample.getStringVal(DT_Sample.REQUEST_RECORD_ID_LIST, user);

        assignedProcessMap.put(DT_AssignedProcess.SAMPLE_ID, igoSampleId);
        assignedProcessMap.put(DT_AssignedProcess.PROCESS_STEP_NUMBER, assignedProcess.getStepNumber());
        assignedProcessMap.put(DT_AssignedProcess.PROCESS_NAME, assignedProcess.getName());
        assignedProcessMap.put(DT_AssignedProcess.OTHER_SAMPLE_ID, cmoSampleId);
        assignedProcessMap.put(DT_AssignedProcess.STATUS, assignedProcess.getStatus());
        assignedProcessMap.put(DT_AssignedProcess.SAMPLE_RECORD_ID, sample.getRecordId());
        assignedProcessMap.put(DT_AssignedProcess.REQUEST_RECORD_ID, requestRecordIds);

        return assignedProcessMap;
    }
}
