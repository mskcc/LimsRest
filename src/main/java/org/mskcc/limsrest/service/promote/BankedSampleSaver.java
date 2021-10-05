package org.mskcc.limsrest.service.promote;

import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.sample.BankedSample;

import java.rmi.RemoteException;

public class BankedSampleSaver implements RecordSaver {
    private static final Log LOGGER = LogFactory.getLog(BankedSampleSaver.class);

    @Override
    public void save(BankedSample bankedSample, DataRecordManager dataRecordManager, User user) {
        try {
            LOGGER.info(String.format("Saving Banked Sample: %s", bankedSample.getUserSampleID()));

            DataRecord bankedRecord = addBankedSampleRecord(dataRecordManager, user);
            bankedRecord.setFields(bankedSample.getFields(), user);
            dataRecordManager.storeAndCommit(String.format("Created Banked Sample %s", bankedSample.getUserSampleID()), user);
        } catch (Exception e) {
            LOGGER.warn(String.format("Saving Banked Sample %s failed", bankedSample.getUserSampleID()), e);
        }
    }

    protected DataRecord addBankedSampleRecord(DataRecordManager dataRecordManager, User user) throws IoError,
            NotFound, AlreadyExists, InvalidValue, RemoteException, ServerException {
        return dataRecordManager.addDataRecord("BankedSample", user);
    }
}
