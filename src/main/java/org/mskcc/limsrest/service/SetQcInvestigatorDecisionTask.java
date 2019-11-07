package org.mskcc.limsrest.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.InvalidValue;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.sqlbuilder.*;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.util.Messages;
import org.springframework.security.access.prepost.PreAuthorize;

import java.rmi.RemoteException;
import java.util.*;

/*
 * A queued task that sets Investigator Decision in QC Report tables.
 *
 * @author Lisa Wagner
 */

public class SetQcInvestigatorDecisionTask extends LimsTask {

    private static Log log = LogFactory.getLog(SetQcInvestigatorDecisionTask.class);
    List<Map<String, Object>> data;

    public SetQcInvestigatorDecisionTask() {
    }

    public void init(final List<Map<String, Object>> data) {

        this.data = data;

    }


    @PreAuthorize("hasRole('READ')")
    @Override
    public Object execute(VeloxConnection conn) {
        int count = 0;
        try {

            for (Map entry : data) {
                String datatype = (String) entry.get("datatype");

                List<Object> records = (List<Object>) entry.get("records");
                List<Map<String, Object>> decisions = (List<Map<String, Object>>) entry.get("decisions");
                List<DataRecord> matched = dataRecordManager.queryDataRecords(datatype, "RecordId", records, user);

                for (DataRecord match :
                        matched) {
                    for (Map field : decisions) {
                        if (String.valueOf(match.getRecordId()).equals(String.valueOf(field.get("RecordId")))) {
                            match.setDataField("InvestigatorDecision", field.get("InvestigatorDecision"), user);
                            count++;
                        }
                    }
                }
                dataRecordManager.storeAndCommit("Storing QC Decision", null, user);
                log.info(count + " Investigator Decisions set in " + datatype);
            }


        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return Messages.ERROR_IN + " SETTING INVESTIGATOR DECISION: " + e.getMessage();
        }
        return count + " Investigator Decisions set";
    }

}
