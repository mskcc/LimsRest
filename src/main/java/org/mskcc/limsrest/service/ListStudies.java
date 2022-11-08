package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Find all studies/projects and list them 
 */
public class ListStudies extends LimsTask {
    private static Log log = LogFactory.getLog(ListStudies.class);
    boolean cmoOnly = true;

    public void init(String cmoOnly) {
        if ("false".equals(cmoOnly.toLowerCase())) {
            this.cmoOnly = false;
        }
    }

    @Override
    public Object execute(VeloxConnection conn) {
        LinkedList<ProjectSummary> allProjects = new LinkedList<>();
        try {
            List<DataRecord> limsProjectList = dataRecordManager.queryDataRecords("Project", null, user);
            for (DataRecord p : limsProjectList) {
                ProjectSummary ps = new ProjectSummary(p.getStringVal("ProjectId", user));
                LimsTask.annotateProjectSummary(ps, p, conn.getUser());
                DataRecord[] requests = p.getChildrenOfType("Request", user);
                for (int i = 0; i < requests.length; i++) {
                    try {
                        String pm = "";
                        try {
                            pm = requests[i].getStringVal("ProjectManager", user);
                        } catch (NullPointerException npe) {
                        }
                        if (!cmoOnly || (cmoOnly && (!pm.equals("") && !pm.equals("NO PM")))) {
                            RequestSummary rs = new RequestSummary(requests[i].getStringVal("RequestId", user));
                            rs.setRecordId(requests[i].getRecordId());
                            ps.addRequestSummary(rs);
                        }
                    } catch (NullPointerException npe) {
                    }
                }
                allProjects.add(ps);
            }
        } catch (Throwable e) {
            log.info(e.getMessage());
            ProjectSummary errorProject = new ProjectSummary("ERROR");
            errorProject.setCmoProjectId(e.getMessage());
            allProjects.add(errorProject);
        }

        return allProjects;
    }
}