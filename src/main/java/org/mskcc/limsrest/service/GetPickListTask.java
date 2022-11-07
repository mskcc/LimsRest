package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.servermanager.PickListConfig;
import com.velox.api.servermanager.PickListManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import lombok.Setter;
import org.mskcc.limsrest.ConnectionLIMS;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * A queued task that takes a pick list name and returns the possible values 
 * 
 * @author Aaron Gabow
 */
@Setter
public class GetPickListTask {
    private String picklist;
    private ConnectionLIMS conn;

    public GetPickListTask(String picklist, ConnectionLIMS conn) {
        this.picklist = picklist;
        this.conn = conn;
    }

    public List<String> execute() {
        List<String> values = new LinkedList<String>();
        try {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            DataRecordManager drm = vConn.getDataRecordManager();

            PickListManager picklister = vConn.getDataMgmtServer().getPickListManager(user);
            PickListConfig pickConfig = picklister.getPickListConfig(picklist);
            if (pickConfig != null) {
                values = pickConfig.getEntryList();
            }
        } catch (Throwable e) {
        }

        if (values.equals("Exemplar Sample Type")) {
            String[] blacklist = {"cDNA", "cDNA Library", "Plasma"};
            values.removeAll(Arrays.asList(blacklist));
        }
        return values;
    }
}