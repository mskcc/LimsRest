package org.mskcc.limsrest.limsapi.assignedprocess;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.user.User;

public class RepoolSampleAssignedAssignedProcessConfig implements AssignedProcessConfig {
    private final DataRecord sample;
    private final QcParentSampleRetriever qcParentSampleRetriever = new QcParentSampleRetriever();

    public RepoolSampleAssignedAssignedProcessConfig(DataRecord qc, User user) throws Exception {
        sample = qcParentSampleRetriever.retrieve(qc, user);
    }

    @Override
    public DataRecord getSample() throws Exception {
        return sample;
    }

    @Override
    public AssignedProcess getProcessToAssign() {
        return AssignedProcess.PRE_SEQUENCING_POOLING_OF_LIBRARIES;
    }
}