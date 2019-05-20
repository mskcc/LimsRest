package org.mskcc.limsrest.limsapi.interops;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.limsapi.LimsTask;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GetInterOpsDataTask extends LimsTask {
    private static final Log log = LogFactory.getLog(GetInterOpsDataTask.class);

    private String runId;

    public void init(String runId) {
        this.runId = runId;
    }

    @Override
    public Object execute(VeloxConnection conn) {
        List<Map<String, Object>> interOps = new ArrayList<>();
        try {
            List<DataRecord> interOpsRecords = dataRecordManager.queryDataRecords("Interopsdatum",
                    "i_Runwithnumberprefixremoved = '" + runId + "'", user);

            for (DataRecord interOpsRecord : interOpsRecords) {
                Map<String, Object> fields = interOpsRecord.getFields(user);
                interOps.add(fields);
            }
        } catch (Exception e) {
            log.error(String.format("Error while retrieving InterOpsDatum for run id: %s", runId), e);
        }
        return interOps;
    }
}
