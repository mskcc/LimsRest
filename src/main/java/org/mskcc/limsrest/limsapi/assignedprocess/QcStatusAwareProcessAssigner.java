package org.mskcc.limsrest.limsapi.assignedprocess;

import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_AssignedProcess;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_Batch;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_Sample;
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

    public QcStatusAwareProcessAssigner(AssignedProcessConfigFactory assignedProcessConfigFactory,
                                        AssignedProcessCreator assignedProcessCreator) {
        this.assignedProcessConfigFactory = assignedProcessConfigFactory;
        this.assignedProcessCreator = assignedProcessCreator;
    }

    public void assign(DataRecordManager dataRecordManager, User user, DataRecord seqQc, String status) {
        DataRecord sample = null;
        try {
            AssignedProcessConfig assignedProcessConfig = assignedProcessConfigFactory.getProcessAssignerConfig
                    (status, dataRecordManager, seqQc, user);

            sample = assignedProcessConfig.getSample();
            DataRecord assignedProcess = addAssignedProcess(dataRecordManager, user, assignedProcessConfig);
            changeSampleStatus(sample, assignedProcessConfig, user);

            addSampleAsChild(user, sample, assignedProcess);
            removeSampleFromBatch(user, sample);
        } catch (Exception e) {
            String sampleId = tryToGetSampleId(user, sample);
            log.warn(String.format("Unable to assign process with status: %s for sample: %s", status, sampleId), e);
        }
    }

    private String tryToGetSampleId(User user, DataRecord sample) {
        String sampleId = "";
        if (sample != null) {
            try {
                sampleId = sample.getStringVal(DT_Sample.SAMPLE_ID, user);
            } catch (Exception ommited) {
            }
        }
        return sampleId;
    }

    private void changeSampleStatus(DataRecord sample, AssignedProcessConfig assignedProcessConfig, User user) throws
            Exception {
        String igoId = sample.getStringVal(DT_Sample.SAMPLE_ID, user);
        String newStatus = assignedProcessConfig.getProcessToAssign().getStatus();
        String oldStatus = sample.getStringVal(DT_Sample.EXEMPLAR_SAMPLE_STATUS, user);

        log.info(String.format("Changing sample's %s status from %s to %s", igoId, oldStatus, newStatus));

        sample.setDataField(DT_Sample.EXEMPLAR_SAMPLE_STATUS, newStatus, user);
    }

    private DataRecord addAssignedProcess(DataRecordManager dataRecordManager, User user, AssignedProcessConfig
            assignedProcessConfig) throws Exception {
        AssignedProcess assignedProcess = assignedProcessConfig.getProcessToAssign();

        DataRecord sample = assignedProcessConfig.getSample();
        Map<String, Object> assignedProcessMap = assignedProcessCreator.create(sample, assignedProcess, user);

        log.info(String.format("Assigning process: %s to sample: %s (%s)", assignedProcess.getName(),
                assignedProcessMap.get(DT_AssignedProcess.SAMPLE_ID), assignedProcessMap.get(DT_AssignedProcess
                        .OTHER_SAMPLE_ID)));

        List<DataRecord> assignedProcesses = dataRecordManager.addDataRecords(DT_AssignedProcess.DATA_TYPE,
                Collections.singletonList(assignedProcessMap), user);
        validateAssignedProcessAdded(assignedProcessMap, assignedProcesses);
        return assignedProcesses.get(0);
    }

    private void addSampleAsChild(User user, DataRecord sample, DataRecord assignedProcess) throws AlreadyExists,
            NotFound, IoError, RemoteException {
        String igoId = sample.getStringVal(DT_Sample.SAMPLE_ID, user);
        log.info(String.format("Adding sample %s as a child to Assigned Process with record id: %d", igoId,
                assignedProcess.getRecordId()));

        assignedProcess.addChild(sample, user);
    }

    private void removeSampleFromBatch(User user, DataRecord sampleRecord) throws Exception {
        List<DataRecord> batches = sampleRecord.getParentsOfType(DT_Batch.DATA_TYPE, user);
        String igoId = sampleRecord.getStringVal(DT_Sample.SAMPLE_ID, user);

        if (batches.size() == 0) {
            log.warn(String.format("There is no parent batch for sample %s to remove from", igoId));
        } else {
            DataRecord batch = batches.get(batches.size() - 1);
            String batchName = batch.getStringVal(DT_Batch.BATCH_NAME, user);
            String recordName = batch.getStringVal(DT_Batch.DATA_RECORD_NAME, user);

            log.info(String.format("Number of parent batches for sample %s id %d . Choosing the last one: %s (%s) to " +
                    "remove" +
                    " from", igoId, batches.size(), batchName, recordName));

            log.info(String.format("Removing child sample %s from batch %s", igoId, batchName));

            batch.removeChild(sampleRecord, null, user);
        }
    }

    private void validateAssignedProcessAdded(Map<String, Object> assignedProcessMap, List<DataRecord>
            assignedProcesses) {
        if (assignedProcesses.size() == 0)
            throw new RuntimeException(String.format("Unable to assign process to sample: %s (%s)",
                    assignedProcessMap.get(DT_AssignedProcess.SAMPLE_ID), assignedProcessMap.get(DT_AssignedProcess
                            .OTHER_SAMPLE_ID)));
    }
}
