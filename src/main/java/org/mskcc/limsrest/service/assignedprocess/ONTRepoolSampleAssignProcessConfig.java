package org.mskcc.limsrest.service.assignedprocess;

import com.velox.api.datarecord.DataRecord;

public class ONTRepoolSampleAssignProcessConfig implements AssignedProcessConfig {
    private final DataRecord sample;
    public ONTRepoolSampleAssignProcessConfig(DataRecord sample) {
        this.sample = sample;
    }
    @Override
    public DataRecord getSample() {
        return sample;
    }

    @Override
    public AssignedProcess getProcessToAssign() {
        return AssignedProcess.ONT_LIBRARY_PREP_LIGATION;
    }
}
