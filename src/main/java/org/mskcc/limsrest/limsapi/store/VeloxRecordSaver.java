package org.mskcc.limsrest.limsapi.store;

import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import org.apache.log4j.Logger;
import org.mskcc.domain.sample.BankedSample;

import java.rmi.RemoteException;

public class VeloxRecordSaver implements RecordSaver {
    private static final Logger LOGGER = Logger.getLogger(VeloxRecordSaver.class);

    @Override
    public void save(BankedSample bankedSample, DataRecordManager dataRecordManager, User user) {
        try {
            LOGGER.info(String.format("Saving Banked Sample: %s", bankedSample.getUserSampleID()));

            DataRecord bankedRecord = addBankedSampleRecord(dataRecordManager, user);
            bankedRecord.setFields(bankedSample.getFields(), user);
            dataRecordManager.storeAndCommit(String.format("Created Banked Sample %s", bankedSample.getUserSampleID()
            ), user);
        } catch (Exception e) {
            LOGGER.warn(String.format("Saving Banked Sample %s failed", bankedSample.getUserSampleID()), e);
        }
    }

    protected DataRecord addBankedSampleRecord(DataRecordManager dataRecordManager, User user) throws IoError,
            NotFound, AlreadyExists, InvalidValue, RemoteException {
        return dataRecordManager.addDataRecord("BankedSample", user);
    }
}
