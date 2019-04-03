package org.mskcc.limsrest.limsapi;

import com.velox.api.datamgmtserver.DataMgmtServer;
import com.velox.api.servermanager.PickListConfig;
import com.velox.api.servermanager.PickListManager;
import com.velox.api.user.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.LinkedList;
import java.util.List;

public class LimsPickListRetriever implements PickListRetriever {
    private Log log = LogFactory.getLog(LimsPickListRetriever.class);

    @Override
    public List<String> retrieve(String pickListName, DataMgmtServer dataMgmtServer, User user) {
        List<String> values = new LinkedList<>();

        try {
            PickListManager picklister = dataMgmtServer.getPickListManager(user);
            PickListConfig pickConfig = picklister.getPickListConfig(pickListName);
            if (pickConfig != null) {
                values = pickConfig.getEntryList();
            }
        } catch (Throwable e) {
            log.warn(String.format("Error while retrieving values for pick list %s", pickListName), e);
        }

        log.info(String.format("%s Pick list values retrieved (%d): %s", pickListName, values.size(), values));

        return values;
    }
}
