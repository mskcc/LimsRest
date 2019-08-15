package org.mskcc.limsrest.service;

import com.velox.api.servermanager.PickListConfig;
import com.velox.api.servermanager.PickListManager;
import com.velox.sapioutils.client.standalone.VeloxConnection;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * A queued task that takes a pick list name and returns the possible values 
 * 
 * @author Aaron Gabow
 */
public class GetPickList extends LimsTask {
    private String picklist;

    public void init(String picklist) {
        this.picklist = picklist;
    }

    @Override
    public Object execute(VeloxConnection conn) {
        List<String> values = new LinkedList<String>();
        try {
            PickListManager picklister = dataMgmtServer.getPickListManager(user);
            PickListConfig pickConfig = picklister.getPickListConfig(picklist);
            if (pickConfig != null) {
                values = pickConfig.getEntryList();
            }
        } catch (Throwable e) {
        }

        if (!values.equals("Exemplar Sample Type")) {
            return values;
        } else {
            String[] blacklist = {"cDNA", "cDNA Library", "Plasma"};
            values.removeAll(Arrays.asList(blacklist));
            return values;
        }
    }
}