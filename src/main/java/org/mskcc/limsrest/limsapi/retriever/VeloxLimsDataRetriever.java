package org.mskcc.limsrest.limsapi.retriever;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import org.mskcc.domain.sample.BankedSample;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VeloxLimsDataRetriever implements LimsDataRetriever {
    @Override
    public List<BankedSample> getBankedSamples(String query, DataRecordManager dataRecordManager, User user) throws
            NotFound, IoError, RemoteException {
        List<DataRecord> dataRecords = dataRecordManager.queryDataRecords(BankedSample.DATA_TYPE_NAME,
                query, user);

        List<BankedSample> bankedSamples = new ArrayList<>();
        for (DataRecord dataRecord : dataRecords) {
            String id = dataRecord.getStringVal(BankedSample.USER_SAMPLE_ID, user);
            Map<String, Object> fields = dataRecord.getFields(user);
            BankedSample bankedSample = new BankedSample(id, fields);

            bankedSamples.add(bankedSample);
        }

        return bankedSamples;
    }
}
