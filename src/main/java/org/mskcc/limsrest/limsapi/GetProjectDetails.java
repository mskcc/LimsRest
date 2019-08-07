package org.mskcc.limsrest.limsapi;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Find all samples associated with a project/request 
 */
public class GetProjectDetails extends LimsTask {
    private static Log log = LogFactory.getLog(GetProjectDetails.class);
    protected String project;

    public void init(String project) {
        this.project = project;
    }

    @Override
    public Object execute(VeloxConnection conn) {
        ProjectSummary ps = new ProjectSummary();
        RequestDetailed rd = new RequestDetailed(project);

        try {
            List<DataRecord> limsRequestList = dataRecordManager.queryDataRecords("Request", "RequestId = '" + project + "'", user);
            for (DataRecord r : limsRequestList) {
                annotateRequestDetailed(rd, r);
                List<DataRecord> parents = r.getParentsOfType("Project", user);
                if (parents.size() > 0) {
                    annotateProjectSummary(ps, parents.get(0));
                }
                ps.addRequest(rd);
            }
        } catch (Throwable e) {
            log.info(e.getMessage());
            rd = RequestDetailed.errorMessage(e.getMessage());
            ps.addRequest(rd);
        }

        return ps;
    }
}