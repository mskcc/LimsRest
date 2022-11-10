package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;

import java.util.List;

/**
 * Find all samples associated with a project/request 
 */
public class GetProjectDetailsTask {
    private static Log log = LogFactory.getLog(GetProjectDetailsTask.class);
    protected String project;
    private ConnectionLIMS conn;

    public GetProjectDetailsTask(String project, ConnectionLIMS conn) {
        this.project = project;
        this.conn = conn;
    }

    public ProjectSummary execute() {
        VeloxConnection vConn = conn.getConnection();
        User user = vConn.getUser();
        DataRecordManager drm = vConn.getDataRecordManager();

        ProjectSummary ps = new ProjectSummary();
        RequestDetailed rd = new RequestDetailed(project);
        try {
            List<DataRecord> limsRequestList = drm.queryDataRecords("Request", "RequestId = '" + project + "'", user);
            for (DataRecord r : limsRequestList) {
                LimsTask.annotateRequestDetailed(rd, r, user);
                List<DataRecord> parents = r.getParentsOfType("Project", user);
                if (parents.size() > 0) {
                    LimsTask.annotateProjectSummary(ps, parents.get(0), user);
                }
                ps.addRequest(rd);
            }
        } catch (Throwable e) {
            log.error(e.getMessage());
            rd = RequestDetailed.errorMessage(e.getMessage());
            ps.addRequest(rd);
        }

        return ps;
    }
}