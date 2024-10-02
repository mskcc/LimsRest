package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.model.LeftoverMaterial;

import java.util.List;

/**
 *
 */
public class GetLeftoverMaterialTask {
    private static Log log = LogFactory.getLog(GetLeftoverMaterialTask.class);

    protected String baseIgoId;

    private ConnectionLIMS conn;

    public GetLeftoverMaterialTask(String baseIgoId, ConnectionLIMS conn) {
        this.baseIgoId = baseIgoId;
        this.conn = conn;
    }

    public Object execute() {
        try {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            DataRecordManager drm = vConn.getDataRecordManager();
            log.info("Querying sample table for all aliquots of the parent sample.");
            List<DataRecord> sampleList = drm.queryDataRecords("Sample", "SampleId LIKE '" + baseIgoId + "%'", user);
            log.info("Samples found: " + sampleList.size());
            String otherSampleId = "";
            Double remainingMassDNA = 0.0;
            Double remainingMassRNA = 0.0;
            Double remainingLibraryMass = 0.0;
            String remainingLibraryConcentrationUnits = "";

            for (DataRecord sample : sampleList) {
                String sampleType = sample.getStringVal("ExemplarSampleType", user);
                otherSampleId = sample.getStringVal("OtherSampleId", user);
                log.info(sampleType);
                if (sampleType == null)
                    sampleType = "";

                if (sampleType.endsWith("DNA")) { // hmwDNA, DNA
                    //sample.getValue("Volume", user);
                    Double mass = (Double) sample.getValue("TotalMass", user);
                    if (mass != null && mass > 0)
                        remainingMassDNA = mass;
                }
                if (sampleType.endsWith("RNA")) {
                    Double mass = (Double) sample.getValue("TotalMass", user);
                    if (mass != null && mass > 0)
                        remainingMassRNA = mass;
                }
                if (sampleType.endsWith("Library")) { // DNA Library, cDNA Library
                    Double mass = (Double) sample.getValue("TotalMass", user);
                    if (mass != null && mass > 0) {
                        remainingLibraryMass = mass;
                        remainingLibraryConcentrationUnits = sample.getStringVal("ConcentrationUnits", user);
                    }
                }
            }

            LeftoverMaterial m = new LeftoverMaterial(baseIgoId, otherSampleId, remainingMassDNA, remainingMassRNA, remainingLibraryMass, remainingLibraryConcentrationUnits);
            log.info("Leftover material found: " + m);
            return m;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}