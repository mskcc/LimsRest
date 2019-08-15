package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

@Service
public class GetDeliveryEmailDetails extends LimsTask {
    private static Log log = LogFactory.getLog(GetDeliveryEmailDetails.class);
    protected String requestID;

    public void init(String requestID) {
        this.requestID = requestID;
    }

    @Override
    @PreAuthorize("hasRole('READ')")
    public Object execute(VeloxConnection conn) {
        try {
            List<DataRecord> requests = dataRecordManager.queryDataRecords("Request", "RequestID='" + requestID +"'", this.user);
            if (requests.size() != 1) {
                throw new Exception("Request " + requestID + " not found.");
            }
            DataRecord request = requests.get(0);
            DataRecord project = request.getParentsOfType("Project", this.user).get(0);

            String projectTitle = project.getStringVal("CMOFinalProjectTitle", this.user);
            String requestIDLIMS = request.getStringVal("ProjectId", this.user);
            String mergeID = project.getStringVal("CMOProjectID", this.user);
            String investigatorEmail = request.getStringVal("Investigatoremail", this.user);
            String sequencingApplication = request.getStringVal("PlatformApplication", this.user); // display name "Project Applications"

            return new DeliveryEmail(requestIDLIMS, projectTitle, mergeID, investigatorEmail, sequencingApplication);
        } catch (Throwable e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            log.info(e.getMessage());
            log.info(sw.toString());
            return new DeliveryEmail(e.getMessage());
        }
    }
}