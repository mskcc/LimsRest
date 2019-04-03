package org.mskcc.limsrest.limsapi;

import com.velox.api.datamgmtserver.DataMgmtServer;
import com.velox.api.user.User;

import java.util.List;

public interface PickListRetriever {
    List<String> retrieve(String pickListName, DataMgmtServer dataMgmtServer, User user);
}
