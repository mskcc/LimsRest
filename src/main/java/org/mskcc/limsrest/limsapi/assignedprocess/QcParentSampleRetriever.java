package org.mskcc.limsrest.limsapi.assignedprocess;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.IoError;
import com.velox.api.user.User;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_Sample;

import java.rmi.RemoteException;
import java.util.List;

public class QcParentSampleRetriever {
    public static DataRecord retrieve(User user, DataRecord qc) throws Exception {
        List<DataRecord> samples = qc.getParentsOfType(DT_Sample.DATA_TYPE, user);
        validateSampleExist(samples);

        return samples.get(0);
    }

    private static void validateSampleExist(List<DataRecord> samples) {
        if(samples.size() == 0)
            throw new RuntimeException("Sample level qc parent sample doesn't exist");
    }
}
