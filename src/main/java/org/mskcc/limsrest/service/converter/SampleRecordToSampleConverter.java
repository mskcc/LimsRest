package org.mskcc.limsrest.service.converter;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.exception.recoverability.serverexception.UnrecoverableServerException;
import com.velox.api.datarecord.IoError;
import com.velox.api.user.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.sample.CmoSampleInfo;
import org.mskcc.domain.sample.Sample;
import org.mskcc.util.VeloxConstants;

import java.rmi.RemoteException;
import java.util.Map;

public class SampleRecordToSampleConverter {
    private final static Log LOGGER = LogFactory.getLog(SampleRecordToSampleConverter.class);

    public Sample convert(DataRecord sampleRecord, User user) throws RemoteException, IoError, UnrecoverableServerException {
        Map<String, Object> fields = sampleRecord.getFields(user);
        String sampleId = (String) fields.get(VeloxConstants.SAMPLE_ID);

        DataRecord[] sampleInfoRecords = sampleRecord.getChildrenOfType(VeloxConstants.SAMPLE_CMO_INFO_RECORDS, user);

        if (sampleInfoRecords.length == 0)
            throw new RuntimeException(String.format("Sample Cmo Info not present for sample: %s", sampleId));
        if (sampleInfoRecords.length > 1)
            throw new RuntimeException(String.format("Multiple Sample Cmo Info records found for sample: %s",
                    sampleId));

        Sample sample = new Sample(sampleId);
        sample.setCmoSampleInfo(getCmoSampleInfo(sampleInfoRecords[0], user));
        sample.setFields(fields);

        LOGGER.debug(String.format("Retrieved sample: %s", sample.getFields()));

        return sample;
    }

    private CmoSampleInfo getCmoSampleInfo(DataRecord sampleInfoRecord, User user) throws RemoteException {
        Map<String, Object> cmoInfoFields = sampleInfoRecord.getFields(user);
        return new CmoSampleInfo(cmoInfoFields);
    }
}
