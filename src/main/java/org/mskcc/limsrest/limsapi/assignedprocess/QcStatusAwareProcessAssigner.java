package org.mskcc.limsrest.limsapi.assignedprocess;

import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_AssignedProcess;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.AssignedProcess;
import org.mskcc.limsrest.limsapi.assignedprocess.config.AssignedProcessConfig;
import org.mskcc.limsrest.limsapi.assignedprocess.config.AssignedProcessConfigFactory;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class QcStatusAwareProcessAssigner {
    private final static Log log = LogFactory.getLog(QcStatusAwareProcessAssigner.class);

    private AssignedProcessConfigFactory assignedProcessConfigFactory;
    private AssignedProcessCreator assignedProcessCreator;

    public QcStatusAwareProcessAssigner(AssignedProcessConfigFactory assignedProcessConfigFactory, AssignedProcessCreator assignedProcessCreator) {
        this.assignedProcessConfigFactory = assignedProcessConfigFactory;
        this.assignedProcessCreator = assignedProcessCreator;
    }

    public void assign(DataRecordManager dataRecordManager, User user, DataRecord seqQc, String status) {
        try {
            AssignedProcessConfig assignedProcessConfig = assignedProcessConfigFactory.getProcessAssigner(status, dataRecordManager, seqQc, user);

            DataRecord sample = assignedProcessConfig.getSample();
            DataRecord assignedProcess = addAssignedProcess(dataRecordManager, user, assignedProcessConfig);

            addSampleAsChild(user, sample, assignedProcess);
        } catch (Exception e) {
            log.warn(String.format("Unable to assign process with status: %s", status));
        }
    }

    private DataRecord addAssignedProcess(DataRecordManager dataRecordManager, User user, AssignedProcessConfig assignedProcessConfig) throws Exception {
        AssignedProcess assignedProcess = assignedProcessConfig.getProcessToAssign();

        DataRecord sample = assignedProcessConfig.getSample();
        Map<String, Object> assignedProcessMap = assignedProcessCreator.create(sample, assignedProcess, user);
        log.info(String.format("Assigning process: %s to sample: %s (%s)", assignedProcess.getName(), assignedProcessMap.get(DT_AssignedProcess.SAMPLE_ID), assignedProcessMap.get(DT_AssignedProcess.OTHER_SAMPLE_ID)));

        List<DataRecord> assignedProcesses = dataRecordManager.addDataRecords(DT_AssignedProcess.DATA_TYPE, Collections.singletonList(assignedProcessMap), user);
        validateAssignedProcessAdded(assignedProcessMap, assignedProcesses);
        return assignedProcesses.get(0);
    }

    private void addSampleAsChild(User user, DataRecord sample, DataRecord assignedProcess) throws AlreadyExists, NotFound, IoError, RemoteException {
        assignedProcess.addChild(sample, user);
    }

    private void validateAssignedProcessAdded(Map<String, Object> assignedProcessMap, List<DataRecord> assignedProcesses) {
        if (assignedProcesses.size() == 0)
            throw new RuntimeException(String.format("Unable to assign process to sample: %s (%s)", assignedProcessMap.get(DT_AssignedProcess.SAMPLE_ID), assignedProcessMap.get(DT_AssignedProcess.OTHER_SAMPLE_ID)));
    }
}
