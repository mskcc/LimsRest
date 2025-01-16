package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.model.SamplePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Given an IGO id, return all matching GLP_WES samples relating to the patient ID for the given IGO ID.
 */
public class GetSamplePairsTask {
    private static Log log = LogFactory.getLog(GetSamplePairsTask.class);

    private ConnectionLIMS conn;

    protected String igoId;

    public GetSamplePairsTask(String igoId, ConnectionLIMS conn) {
        this.igoId = igoId;
        this.conn = conn;
    }

    public SamplePair execute() {
        long startTime = System.currentTimeMillis();

        VeloxConnection vConn = conn.getConnection();
        User user = vConn.getUser();
        DataRecordManager dataRecordManager = vConn.getDataRecordManager();

        try {
            SamplePair result = getSamplePairs(igoId, user, dataRecordManager);
            log.info("Sample Pair generation time(ms):" + (System.currentTimeMillis() - startTime));
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }


    protected SamplePair getSamplePairs(String igoId, User user, DataRecordManager dataRecordManager)
            throws Exception {
        log.info("Searching for sample table for recipe GLP_WES and SampleId ='" + igoId + "'");
        // get all GLP_WES 16606 initial samples and no extra aliquots
        List<DataRecord> samples = dataRecordManager.queryDataRecords("Sample", "SampleId LIKE '16606%_%_%' and SampleId NOT LIKE '%\\_%\\_%\\_%' AND recipe = 'GLP_WES'", user);
        String patientId = "";
        HashMap<String, SamplePair> pairsByPatientId = new HashMap<>();
        for (DataRecord sample:samples) {
            String tumorOrNormal = sample.getStringVal("TumorOrNormal", user);
            String cmopatientId = sample.getStringVal("CmoPatientId", user);
            String sampleId = sample.getStringVal("SampleId", user);
            String recipe = sample.getStringVal("Recipe", user);
            String otherSampleId = sample.getStringVal("OtherSampleId", user);

            if (igoId.equals(sampleId)) {
                patientId = cmopatientId;
            }
            SamplePair sp = pairsByPatientId.get(cmopatientId);
            if (sp == null) {
                sp = new SamplePair(cmopatientId, recipe, new ArrayList<String>(), new ArrayList<String>());
                pairsByPatientId.put(cmopatientId, sp);
            }
            sp.addSample(tumorOrNormal, otherSampleId + "_IGO_" + sampleId);
            System.out.println("Added sample Pair: " + sp);
        }

        return pairsByPatientId.get(patientId);
    }
}