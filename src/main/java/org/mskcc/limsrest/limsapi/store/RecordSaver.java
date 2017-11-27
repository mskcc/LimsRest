package org.mskcc.limsrest.limsapi.store;

import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import org.mskcc.domain.sample.BankedSample;

public interface RecordSaver {
    void save(BankedSample bankedSample, DataRecordManager dataRecordManager, User user);
}
