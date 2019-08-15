package org.mskcc.limsrest.service.assignedprocess;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.user.User;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_Sample;

import java.util.List;

public class InitialPoolRetriever {
    public DataRecord retrieve(DataRecord sampleLevelQc, User user) throws Exception {
        List<DataRecord> parents = sampleLevelQc.getParentsOfType("Sample", user);
        validateParentSampleExist(parents, sampleLevelQc.getRecordId());
        DataRecord parentSample = parents.get(0);

        DataRecord[] childSamples = parentSample.getChildrenOfType("Sample", user);
        validateChildPoolExist(childSamples, parentSample, user);

        return childSamples[0];
    }

    private void validateParentSampleExist(List<DataRecord> parents, long recordId) {
        if (parents.size() == 0)
            throw new RuntimeException(String.format("No parent sample for sample level qc with record id: %s", recordId));
    }

    private void validateChildPoolExist(DataRecord[] childSamples, DataRecord parentSample, User user) throws Exception {
        if (childSamples.length == 0)
            throw new NoInitialPoolFoundException(String.format("No initial pool found for sample with igo id: %s", parentSample.getStringVal(DT_Sample.OTHER_SAMPLE_ID, user)));
    }
}