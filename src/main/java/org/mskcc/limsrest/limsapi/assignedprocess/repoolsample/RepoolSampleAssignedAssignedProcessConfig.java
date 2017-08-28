package org.mskcc.limsrest.limsapi.assignedprocess.repoolsample;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.user.User;
import org.mskcc.domain.AssignedProcess;
import org.mskcc.limsrest.limsapi.assignedprocess.QcParentSampleRetriever;
import org.mskcc.limsrest.limsapi.assignedprocess.config.AssignedProcessConfig;

public class RepoolSampleAssignedAssignedProcessConfig implements AssignedProcessConfig {
    private final DataRecord sample;
    private final QcParentSampleRetriever qcParentSampleRetriever = new QcParentSampleRetriever();

    public RepoolSampleAssignedAssignedProcessConfig(User user, DataRecord qc) throws Exception {
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
