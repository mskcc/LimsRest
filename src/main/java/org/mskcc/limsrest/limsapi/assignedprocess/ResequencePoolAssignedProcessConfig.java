package org.mskcc.limsrest.limsapi.assignedprocess;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.user.User;

public class ResequencePoolAssignedProcessConfig implements AssignedProcessConfig {
    private final DataRecord qc;
    private final User user;
    private final InitialPoolRetriever initialPoolRetriever = new InitialPoolRetriever();
    private DataRecord sample;

    public ResequencePoolAssignedProcessConfig(DataRecord qc, User user) {
        this.qc = qc;
        this.user = user;
    }

    @Override
    public DataRecord getSample() throws Exception {
        if (sample == null)
            sample = retrieveSample();
        return sample;
    }

    private DataRecord retrieveSample() throws Exception {
        return initialPoolRetriever.retrieve(qc, user);
    }

    @Override
    public AssignedProcess getProcessToAssign() {
        return AssignedProcess.PRE_SEQUENCING_POOLING_OF_LIBRARIES;
    }
}
