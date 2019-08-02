
package org.mskcc.limsrest.limsapi;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

/**
 * A queued task that returns all process names for the system 
 * 
 * @author Aaron Gabow
 * 
 */
@Service
public class GetProcessNames extends LimsTask {
    private Log log = LogFactory.getLog(GetProcessNames.class);

    //execute the velox call
    //@PreAuthorize("hasRole('READ')")
    @Override
    public Object execute(VeloxConnection conn) {
        List<String> names = new LinkedList<>();

        try {
            List<DataRecord> processList = dataRecordManager.queryDataRecords("Process", null, user);
            for (DataRecord p : processList) {
                names.add(p.getStringVal("ProcessName", user));
            }
        } catch (Throwable e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            log.info("ERROR IN SETTING REQUEST STATUS: " + e.getMessage() + "TRACE: " + sw.toString());
        }

        return names;
    }
}
