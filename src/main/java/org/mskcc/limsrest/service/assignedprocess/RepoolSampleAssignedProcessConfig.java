package org.mskcc.limsrest.service.assignedprocess;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.user.User;

public class RepoolSampleAssignedProcessConfig implements AssignedProcessConfig {
    private final DataRecord sample;

    public RepoolSampleAssignedProcessConfig(DataRecord sample) {
        this.sample = sample;
    }

    @Override
    public DataRecord getSample() {
        return sample;
    }

    @Override
    public AssignedProcess getProcessToAssign() {
        return AssignedProcess.PRE_SEQUENCING_POOLING_OF_LIBRARIES;
    }
}