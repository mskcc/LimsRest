package org.mskcc.limsrest.service.assignedprocess;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.user.User;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_Sample;

import java.util.List;

public class QcParentSampleRetriever {
    public DataRecord retrieve(DataRecord qc, User user) throws Exception {
        List<DataRecord> samples = qc.getParentsOfType(DT_Sample.DATA_TYPE, user);
        validateSampleExist(samples, qc.getRecordId());

        return samples.get(0);
    }

    private void validateSampleExist(List<DataRecord> samples, long recordId) {
        if(samples.size() == 0)
            throw new RuntimeException(String.format("Parent sample doesn't exist for sample level qc: %s", recordId));
    }
}
