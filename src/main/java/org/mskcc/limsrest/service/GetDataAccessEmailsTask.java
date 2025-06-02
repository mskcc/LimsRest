package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;

import java.util.*;

/**
 *  Returns a list of all data access emails in LIMS created in the last 45 days.
 */
public class GetDataAccessEmailsTask {
    private static Log log = LogFactory.getLog(GetDataAccessEmailsTask.class);
    private ConnectionLIMS conn;

    public GetDataAccessEmailsTask(ConnectionLIMS conn) {
        this.conn = conn;
    }

    public List<String> execute() {
        try {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            DataRecordManager drm = vConn.getDataRecordManager();

            long fortyFiveDaysAgoMs = System.currentTimeMillis() - 45l*24*60*60*1000;
            String query = "RECENTDELIVERYDATE IS NULL AND DATECREATED > " + fortyFiveDaysAgoMs;
            /*
            Could restrict query to only requests that will receive data, therefore exclude requests with 'REQUESTNAME':
                CellLineAuthentication
                ddPCR
                DNA/RNASimultaneous
                DNAExtraction
                DNA-QC
                FragmentAnalysis
                Library-QC
                PATH-DNA/RNASimultaneous
                PATH-DNAExtraction
                PATH-RNAExtraction
                RNA-QC
             */
            log.info("Querying LIMS:  " + query);
            List<DataRecord> requestList = drm.queryDataRecords("Request", query, user);
            Set<String> emailSet = new HashSet<>();
            for (DataRecord request : requestList) {
                String investigatorEmail = request.getStringVal("Investigatoremail", user);
                if (investigatorEmail != null && investigatorEmail.endsWith("@mskcc.org"))
                    emailSet.add(investigatorEmail);
                String dataAccessEmails = request.getStringVal("DataAccessEmails", user);
                dataAccessEmails = dataAccessEmails.replace(' ',','); // replace spaces with commas
                String [] emails = dataAccessEmails.split(",");
                for (String email : emails) {
                    if (email.length() > 0 && email.endsWith("@mskcc.org"))
                        emailSet.add(email.toLowerCase(Locale.ROOT));
                }
            }
            return new ArrayList<>(emailSet);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
