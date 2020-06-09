package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.FlowCellLaneModel;
import com.velox.sloan.cmo.recmodels.RequestModel;
import com.velox.sloan.cmo.recmodels.SampleModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

import static org.mskcc.limsrest.util.Utils.*;

public class GetSampleFlowCellLanesTask {
    private static final Log log = LogFactory.getLog(org.mskcc.limsrest.service.GetSampleFlowCellLanesTask.class);
    // TODO - this can actually be 8
    //      See - https://www.illumina.com/systems/sequencing-platforms/hiseq-3000-4000/specifications.html
    // For WGS there are 4 lanes max b/c a NovaSeq run has 4 lanes - 4 for S4 flowcell, 2 lanes for SP,
    // S1 and S2 flowcell
    private final int MAX_FLOW_CELL_LANES = 4;
    protected List<String> projectList;
    private ConnectionLIMS conn;

    @Value("${delphiRestUrl}")
    private String delphiRestUrl;

    public GetSampleFlowCellLanesTask(List<String> projectList, ConnectionLIMS conn) {
        this.conn = conn;
        this.projectList = projectList;
    }

    public List<Map<String, Object>> execute() {
        VeloxConnection vConn = this.conn.getConnection();
        User user = vConn.getUser();
        DataRecordManager dataRecordManager = vConn.getDataRecordManager();
        String joinedProjects = String.join("','", this.projectList);
        String capture = String.format("('%s')", joinedProjects);             // e.g. ('P1', 'P2')
        log.info("Projects: " + capture);

        List<DataRecord> requestList = new ArrayList<>();
        try {
            String query = String.format("RequestId in %s", capture);
            requestList = dataRecordManager.queryDataRecords(RequestModel.DATA_TYPE_NAME, query, user);
        } catch (Exception e) {
            log.error(String.format("Error fetching samples from projects: %s", projectList.toString()));
            return new ArrayList<>();
        }

        /**
         * response: {
         *     "projects": [
         *          "project": [Request ID] (String),
         *          "samples": [
         *              "sample": [Sample ID] (String),
         *              "flowCellLanes": Set<String>    # Any set of the possible lanes [1,2,3,4]
         *          ]
         *     ]
         * }
         */
        List<Map<String, Object>> response = new ArrayList<>();
        for (DataRecord request : requestList) {
            // Create Request entry - "projects"
            Map<String, Object> projectEntry = new HashMap<>();
            String requestId = getRecordStringValue(request, RequestModel.REQUEST_ID, user);
            projectEntry.put("project", requestId);

            // Create Request entry - "samples", which is a list of sampleMappings for each sample in the project
            List<Map<String, Object>> sampleMappings = new ArrayList<>();
            DataRecord[] samples = getChildrenofDataRecord(request, SampleModel.DATA_TYPE_NAME, user);
            for (DataRecord sample : samples) {
                Map<String, Object> sampleMapping = new HashMap<>();

                String sampleId = getRecordStringValue(sample, SampleModel.SAMPLE_ID, user);
                sampleMapping.put("sample", sampleId);

                // Extract all flow cell lanes
                Set<Long> flowCellLanes = new HashSet<>();
                List<DataRecord> queue = new LinkedList<>(Arrays.asList(samples));
                while (!queue.isEmpty()) {
                    DataRecord curr = queue.remove(0);

                    DataRecord[] sampleChildren = getChildrenofDataRecord(curr, SampleModel.DATA_TYPE_NAME, user);
                    queue.addAll(Arrays.asList(sampleChildren));

                    DataRecord[] flowCellLaneChildren = getChildrenofDataRecord(curr, FlowCellLaneModel.DATA_TYPE_NAME, user);
                    for (DataRecord flowCellLaneChild : flowCellLaneChildren) {
                        Long laneNumber = getRecordLongValue(flowCellLaneChild, FlowCellLaneModel.LANE_NUM, user);
                        flowCellLanes.add(laneNumber);


                        if (flowCellLanes.size() == MAX_FLOW_CELL_LANES) {
                            // If all lanes are present, empty queue and finish project. No need to add redundant lanes.
                            queue.clear();
                        }
                    }
                }
                sampleMapping.put("flowCellLanes", flowCellLanes);
                sampleMappings.add(sampleMapping);
            }
            projectEntry.put("samples", sampleMappings);

            response.add(projectEntry);
        }
        return response;

    }
}
