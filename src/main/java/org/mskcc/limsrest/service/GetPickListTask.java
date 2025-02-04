package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.servermanager.PickListConfig;
import com.velox.api.servermanager.PickListManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import lombok.Setter;
import org.mskcc.limsrest.ConnectionLIMS;

import java.util.*;

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
        List<String> resultList = new LinkedList<String>();
        try {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            DataRecordManager drm = vConn.getDataRecordManager();

            PickListManager picklister = vConn.getDataMgmtServer().getPickListManager(user);
            PickListConfig pickConfig = picklister.getPickListConfig(picklist);


            if (pickConfig != null) {
                resultList = pickConfig.getEntryList();
            }
        } catch (Throwable e) {
        }

        if (resultList.equals("Exemplar Sample Type")) {
            String[] blacklist = {"cDNA", "DNA/cDNA Library", "Plasma"};
            resultList.removeAll(Arrays.asList(blacklist));
        }
        if (picklist.startsWith("ddPCR")) {
            System.out.println("Sorting picklist values for " + picklist);
            resultList.sort(String.CASE_INSENSITIVE_ORDER);
        }
        return resultList;
    }
}