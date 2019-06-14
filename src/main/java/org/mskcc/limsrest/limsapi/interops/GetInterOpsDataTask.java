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
            List<DataRecord> interOpsRecords = dataRecordManager.queryDataRecords("InterOpsDatum","i_Runwithnumberprefixremoved LIKE'%" + runId + "%'" , user);

            /*
            This end point receives Flowcell ID. It will return all the values under column 'i_Runwithnumberprefixremoved'
            which contain the Flowcell ID received. We cannot do LIKE query operations through LIMS API, therefore we will have to
            loop through all the records in the target DataType 'InterOpsDatum' to find records containing 'Flowcell ID'.
             */

            for (DataRecord  record: interOpsRecords) {
                Map<String, Object> fields = record.getFields(user);
                interOps.add(fields);
            }

        } catch (Exception e) {
            log.error(String.format("Error while retrieving InterOpsDatum for run id: %s", runId), e);
        }
        return interOps;
    }
}
