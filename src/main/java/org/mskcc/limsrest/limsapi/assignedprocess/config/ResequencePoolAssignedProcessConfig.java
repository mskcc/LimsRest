package org.mskcc.limsrest.limsapi.assignedprocess.config;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_Sample;
import org.mskcc.domain.AssignedProcess;
import org.mskcc.limsrest.utils.Utils;

import java.util.List;

public class ResequencePoolAssignedProcessConfig implements AssignedProcessConfig {
    private final DataRecord sample;

    public ResequencePoolAssignedProcessConfig(DataRecordManager dataRecordManager, DataRecord qc, User user) throws Exception {
        String sampleId = Utils.getInitialPoolId(qc, user);
        List<DataRecord> samples = dataRecordManager.queryDataRecords(DT_Sample.DATA_TYPE, "SampleId = '" + sampleId + "'", user);
        validateSampleExist(samples, sampleId);

        sample = samples.get(0);
    }

    @Override
    public DataRecord getSample() throws Exception {
        return sample;
    }

    @Override
    public AssignedProcess getProcessToAssign() {
        return AssignedProcess.PRE_SEQUENCING_POOLING_OF_LIBRARIES;
    }

    private void validateSampleExist(List<DataRecord> samples, String sampleId) {
        if (samples.size() == 0)
            throw new RuntimeException(String.format("Sample: %s doesn't exist", sampleId));
    }
}
