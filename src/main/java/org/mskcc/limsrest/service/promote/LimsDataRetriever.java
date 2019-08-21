package org.mskcc.limsrest.service.promote;

import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import org.mskcc.domain.sample.BankedSample;

import java.rmi.RemoteException;
import java.util.List;

public interface LimsDataRetriever {
    List<BankedSample> getBankedSamples(String query, DataRecordManager dataRecordManager, User user) throws
            NotFound, IoError, RemoteException;
}
