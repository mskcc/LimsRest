package org.mskcc.limsrest.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.InvalidValue;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.sqlbuilder.*;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_AssignedProcess;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_Sample;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.Workflow;
import org.mskcc.limsrest.service.assignedprocess.*;
import org.mskcc.limsrest.util.Messages;
import org.springframework.security.access.prepost.PreAuthorize;

import java.rmi.RemoteException;
import java.util.*;

/*
 * A queued task that sets Investigator Decision in QC Report tables.
 *
 * @author Lisa Wagner
 */

public class SetQcInvestigatorDecisionTask extends LimsTask {

    private static Log log = LogFactory.getLog(SetQcInvestigatorDecisionTask.class);
    List<Map<String, Object>> data;
    protected QcStatusAwareProcessAssigner qcStatusAwareProcessAssigner = new QcStatusAwareProcessAssigner();

    public SetQcInvestigatorDecisionTask() {
    }

    public void init(final List<Map<String, Object>> data) {

        this.data = data;

    }
    public static AssignedProcessConfig getProcessAssignerConfig(String qcStatus, DataRecord sample, User user) throws Exception {
        switch (qcStatus) {
            case "Assign from Investigator Decisions":
                return new ResequencePoolAssignedProcessConfig(sample, user);
            default:
                throw new RuntimeException(String.format("Not supported qc status: %s", qcStatus));
        }
    }

    @PreAuthorize("hasRole('READ')")
    @Override
    public Object execute(VeloxConnection conn) {
        int count = 0;
        try {

            for (Map entry : data) {
                String datatype = (String) entry.get("datatype");

                List<Object> records = (List<Object>) entry.get("records");
                List<Map<String, Object>> decisions = (List<Map<String, Object>>) entry.get("decisions");
                List<DataRecord> matched = dataRecordManager.queryDataRecords(datatype, "RecordId", records, user);

                for (DataRecord match :
                        matched) { //match is of type QC RNA/DNA/Library Report
                    for (Map field : decisions) {
                        if (String.valueOf(match.getRecordId()).equals(String.valueOf(field.get("RecordId")))) {
                            match.setDataField("InvestigatorDecision", field.get("InvestigatorDecision"), user);
                            if(field.get("InvestigatorDecision").toString().toLowerCase().contains("continue processing")) {
                                String newStatus = "Ready for - Assign from Investigator Decisions";
                                if(match.getParentsOfType("Sample", user) != null && match.getParentsOfType
                                        ("Sample", user).size() > 0) {
                                    match.getParentsOfType("Sample", user).get(0).setDataField(DT_Sample.
                                            EXEMPLAR_SAMPLE_STATUS, newStatus, user);

                                    DataRecord sample = match.getParentsOfType("Sample", user).get(0);
                                    String status = match.getParentsOfType("Sample", user).get(0).
                                            getDataField(DT_Sample.EXEMPLAR_SAMPLE_STATUS, user).toString();

                                    AssignedProcessConfig assignedProcessConfig = getProcessAssignerConfig(status, sample, user);
                                    AssignedProcess assignedProcess = assignedProcessConfig.getProcessToAssign();
                                    Map<String, Object> assignedProcessMap = AssignedProcessCreator.create(sample, assignedProcess, user);

                                    List<DataRecord> assignedProcesses = dataRecordManager.addDataRecords(DT_AssignedProcess.DATA_TYPE,
                                            Collections.singletonList(assignedProcessMap), user);
                                    validateAssignedProcessAdded(assignedProcessMap, assignedProcesses);
                                    //assignedProcesses.get(0);



                                    // Add a record in "Assigned Process" table
                                    String sampleId = match.getParentsOfType("Sample", user).
                                            get(0).getDataField("SampleId", user).toString();
                                    List<DataRecord> assigned = dataRecordManager.queryDataRecords("AssignedProcess",
                                            "SampleId = '" + sampleId + "'", user);
                                    log.info("assigned size: " + assigned.size());
                                    if (assigned.size() != 1) {
                                        return "Failed to set status for " + sampleId + " because it maps to multiple assigned Processes";
                                    }
                                    DataRecord[] childSamples = assigned.get(0).getChildrenOfType("Sample", user);
                                    log.info("childSamples length: " + childSamples.length);
                                    if (childSamples.length == 0) {
                                        log.info("no sample under assigned process -> adding the sample as a child to assigned process");
                                        assigned.get(0).addChild(match.getParentsOfType("Sample", user).get(0), user);
                                    }
                                }
                            }
                            count++;
                        }
                    }
                }
                dataRecordManager.storeAndCommit("Storing QC Decision", null, user);
                log.info(count + " Investigator Decisions set in " + datatype);
            }


        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return Messages.ERROR_IN + " SETTING INVESTIGATOR DECISION: " + e.getMessage();
        }
        return count + " Investigator Decisions set";
    }

    private void validateAssignedProcessAdded(Map<String, Object> assignedProcessMap, List<DataRecord> assignedProcesses) {
        if (assignedProcesses.size() == 0)
            throw new RuntimeException(String.format("Unable to assign process to sample: %s (%s)",
                    assignedProcessMap.get(DT_AssignedProcess.SAMPLE_ID), assignedProcessMap.get(DT_AssignedProcess
                            .OTHER_SAMPLE_ID)));
    }
}
