package org.mskcc.limsrest.service.sequencingqc;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.RequestModel;
import com.velox.sloan.cmo.recmodels.SampleModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.model.RequestSample;
import org.mskcc.limsrest.model.RequestSampleList;
import org.mskcc.limsrest.service.ArchivedFastq;
import org.mskcc.limsrest.service.GetRequestPermissionsTask;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 *
 */
public class SetStatsONTTask {
    private static Log log = LogFactory.getLog(SetStatsONTTask.class);
    private ConnectionLIMS conn;
    private SampleSequencingQcONT statsONT;
    private String igoId;

    public SetStatsONTTask(SampleSequencingQcONT statsONT, String igoId, ConnectionLIMS conn) {
        this.statsONT = statsONT;
        this.igoId = igoId;
        this.conn = conn;
    }

    public String execute() {
        //Map<String, Object> qcDataVals = new HashMap<>();
        try {
            log.info("Adding/Updating ONT sequencing stats: " + statsONT);
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            DataRecordManager drm = vConn.getDataRecordManager();
            // query for unique row by IGOID & flowcell
            List<DataRecord> ontStat = drm.queryDataRecords("SequencingAnalysisONT",
                    "IGOID='" + statsONT.getIgoId() + "' AND Flowcell='" + statsONT.getFlowcell() +"'", user);
            List<DataRecord> sample = drm.queryDataRecords("Sample", "SampleId = '" + statsONT.getIgoId() + "' ", user);
            DataRecord dr = null;
            if (ontStat.size() == 1) {
                log.info("Updating existing ONT LIMS record.");
                dr = ontStat.get(0);
            } else {
                dr = drm.addDataRecord(SampleSequencingQcONT.TABLE_NAME, user);
                dr.setDataField("IGOID", statsONT.getIgoId(), user);
                dr.setDataField("Flowcell", statsONT.getFlowcell(), user);
            }
            dr.setDataField("ReadsNumber", statsONT.getReads(), user);
            dr.setDataField("Bases", statsONT.getBases(), user);
            dr.setDataField("N50", statsONT.getN50(), user);
            dr.setDataField("MedianReadLength", statsONT.getMedianReadLength(), user);
            dr.setDataField("EstimatedCoverage", statsONT.getEstimatedCoverage(), user);
            dr.setDataField("flowCellType", statsONT.getFlowCellType(), user);
            dr.setDataField("Chemistry", statsONT.getChemistry(), user);
            dr.setDataField("MinKNOWSoftwareVersion", statsONT.getMinKNOWSoftwareVersion(), user);
            dr.setDataField("OtherSampleId", statsONT.getSampleName(), user);
            dr.setDataField("Recipe", "Nanopore", user);
            dr.setDataField("SeqQCStatus", "Under-Review", user);

//            qcDataVals.put("IGOID", statsONT.getIgoId());
//            qcDataVals.put("Flowcell", statsONT.getFlowcell());
//            qcDataVals.put("ReadsNumber", statsONT.getReads());
//            qcDataVals.put("Bases", statsONT.getBases());
//            qcDataVals.put("N50", statsONT.getN50());
//            qcDataVals.put("MedianReadLength", statsONT.getMedianReadLength());
//            qcDataVals.put("EstimatedCoverage", statsONT.getEstimatedCoverage());
//            qcDataVals.put("flowCellType", statsONT.getFlowCellType());
//            qcDataVals.put("Chemistry", statsONT.getChemistry());
//            qcDataVals.put("MinKNOWSoftwareVersion", statsONT.getMinKNOWSoftwareVersion());
//            qcDataVals.put("OtherSampleId", statsONT.getSampleName());
//            qcDataVals.put("Recipe", "Nanopore");
//            qcDataVals.put("SeqQCStatus", "Under-Review");

            sample.get(0).addChild(dr, user);
            log.info("Added status" + dr.getStringVal("SeqQCStatus", user) + " to sample: " + sample.get(0).getStringVal("sampleId", user));
            drm.storeAndCommit("Add/Update ONT Stats", null, user);
            return "Added/Updated " + statsONT;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return e.getMessage();
        }
    }
}