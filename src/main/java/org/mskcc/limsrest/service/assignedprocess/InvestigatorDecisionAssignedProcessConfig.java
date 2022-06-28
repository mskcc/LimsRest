package org.mskcc.limsrest.service.assignedprocess;

import com.velox.api.datarecord.DataRecord;

public class InvestigatorDecisionAssignedProcessConfig implements AssignedProcessConfig {
    private DataRecord sample;

    public InvestigatorDecisionAssignedProcessConfig(DataRecord sample) {
        this.sample = sample;
    }

    @Override
    public DataRecord getSample() {
        return sample;
    }

    @Override
    public AssignedProcess getProcessToAssign() {
        return AssignedProcess.ASSIGN_FROM_INVESTIGATOR_DECISION;
    }
}
