package org.mskcc.limsrest.utils;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.user.User;
import org.mskcc.limsrest.limsapi.assignedprocess.NoInitialPoolFoundException;

import java.util.List;

public class Utils {
    public static String getInitialPoolId(DataRecord qc, User user) throws Exception {
        List<DataRecord> parents = qc.getParentsOfType("Sample", user);
        validateSampleQcParent(parents);
        DataRecord[] childSamples = parents.get(0).getChildrenOfType("Sample", user);
        validatePoolExist(childSamples);

        return childSamples[0].getStringVal("SampleId", user);
    }

    private static void validatePoolExist(DataRecord[] childSamples) throws NoInitialPoolFoundException {
        if(childSamples.length == 0)
            throw new NoInitialPoolFoundException("No initial pool found");
    }

    private static void validateSampleQcParent(List<DataRecord> parents) {
        if(parents.size() == 0)
            throw new RuntimeException(String.format("No parent sample for sample level qc"));
    }
}
