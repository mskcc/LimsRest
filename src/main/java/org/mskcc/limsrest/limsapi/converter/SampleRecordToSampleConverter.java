package org.mskcc.limsrest.limsapi.converter;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.IoError;
import com.velox.api.user.User;
import org.mskcc.domain.sample.CmoSampleInfo;
import org.mskcc.domain.sample.Sample;
import org.mskcc.util.VeloxConstants;

import java.rmi.RemoteException;
import java.util.Map;

public class SampleRecordToSampleConverter {
    public Sample convert(DataRecord sampleRecord, User user) throws RemoteException, IoError {
        Map<String, Object> fields = sampleRecord.getFields(user);
        String sampleId = (String) fields.get(VeloxConstants.SAMPLE_ID);

        DataRecord[] sampleInfoRecords = sampleRecord.getChildrenOfType(VeloxConstants.SAMPLE_CMO_INFO_RECORDS,
                user);

        validateSampleInfoRecords(sampleId, sampleInfoRecords);

        Sample sample = new Sample(sampleId);
        sample.setCmoSampleInfo(getCmoSampleInfo(sampleInfoRecords[0], user));
        sample.setFields(fields);
        return sample;
    }

    private void validateSampleInfoRecords(String sampleId, DataRecord[] sampleInfoRecords) {
        if (sampleInfoRecords.length == 0)
            throw new RuntimeException(String.format("Sample Cmo Info not present for sample: %s", sampleId));
        if (sampleInfoRecords.length > 1)
            throw new RuntimeException(String.format("Multiple Sample Cmo Info records found for sample: %s",
                    sampleId));
    }

    private CmoSampleInfo getCmoSampleInfo(DataRecord sampleInfoRecord, User user) throws RemoteException {
        Map<String, Object> cmoInfoFields = sampleInfoRecord.getFields(user);
        return new CmoSampleInfo(cmoInfoFields);
    }
}
