package org.mskcc.limsrest.service.assignedprocess;

import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_AssignedProcess;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_Batch;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_Sample;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class QcStatusAwareProcessAssigner {
    private final static Log log = LogFactory.getLog(QcStatusAwareProcessAssigner.class);

    public static AssignedProcessConfig getProcessAssignerConfig(QcStatus qcStatus, DataRecord sample, User user) throws Exception {
        switch (qcStatus) {
            case RESEQUENCE_POOL:
                return new ResequencePoolAssignedProcessConfig(sample, user);
            case REPOOL_SAMPLE:
                return new RepoolSampleAssignedProcessConfig(sample);
            case CONTINUE_PROCESSING:
                log.info("Here I am!");
                return new InvestigatorDecisionAssignedProcessConfig(sample);
            default:
                throw new RuntimeException(String.format("Not supported qc status: %s", qcStatus));
        }
    }

    public void assign(DataRecordManager dataRecordManager, User user, DataRecord seqQc, QcStatus status) {
        DataRecord sample = null;
        boolean isSetInvestigatorDecision = false;
        log.info("status is:" + status);
        try {
            if(status.equals(QcStatus.CONTINUE_PROCESSING)) {
                log.info("Setting isSetInvestigatorDecision to true");
                isSetInvestigatorDecision = true;
            }
            AssignedProcessConfig assignedProcessConfig = getProcessAssignerConfig(status, seqQc, user);
            sample = assignedProcessConfig.getSample();
            log.info("AssignedProcessConfig: sample id is: " + sample.getDataField("SampleId", user));
            DataRecord assignedProcess = addAssignedProcess(dataRecordManager, user, assignedProcessConfig, isSetInvestigatorDecision);

            if (!isSetInvestigatorDecision) {
                changeSampleStatus(sample, assignedProcessConfig, user);
            }
            if (isSetInvestigatorDecision) {
                sample = sample.getParentsOfType("Sample", user).get(0);
            }
            addSampleAsChild(user, sample, assignedProcess);
            if (!isSetInvestigatorDecision) {
                removeSampleFromBatch(user, sample);
            }
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
            } catch (Exception omitted) {
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
            assignedProcessConfig, boolean isInvestigatorDecision) throws Exception {
        AssignedProcess assignedProcess = assignedProcessConfig.getProcessToAssign();
        log.info("assigned process is: " + assignedProcess.getName());
        DataRecord sample = assignedProcessConfig.getSample();
        Map<String, Object> assignedProcessMap = AssignedProcessCreator.create(sample, assignedProcess, isInvestigatorDecision, user);
        log.info("assigned process map size = " + assignedProcessMap);
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
            log.info(String.format("Number of parent batches for sample %s: %d.", igoId, batches.size()));
            for (DataRecord batch : batches) {
                String batchName = batch.getStringVal(DT_Batch.BATCH_NAME, user);
                log.info(String.format("Removing child sample %s from batch %s", igoId, batchName));
                batch.removeChild(sampleRecord, null, user);
            }
        }
    }

    private void validateAssignedProcessAdded(Map<String, Object> assignedProcessMap, List<DataRecord> assignedProcesses) {
        if (assignedProcesses.size() == 0) {
            log.info("in validate assigned process added, assignedProcesses.size() == 0");
            throw new RuntimeException(String.format("Unable to assign process to sample: %s (%s)",
                    assignedProcessMap.get(DT_AssignedProcess.SAMPLE_ID), assignedProcessMap.get(DT_AssignedProcess
                            .OTHER_SAMPLE_ID)));
        }
    }
}