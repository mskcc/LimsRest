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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 *
 */
public class SetStatsONTTask {
    private static Log log = LogFactory.getLog(SetStatsONTTask.class);
    private ConnectionLIMS conn;
    private SampleSequencingQcONT statsONT;

    public SetStatsONTTask(SampleSequencingQcONT statsONT, ConnectionLIMS conn) {
        this.statsONT = statsONT;
        this.conn = conn;
    }

    public String execute() {
        try {
            log.info("Adding/Updating ONT sequencing stats: " + statsONT);
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            DataRecordManager drm = vConn.getDataRecordManager();
            // query for unique row by IGOID & flowcell
            List<DataRecord> ontStat = drm.queryDataRecords("SequencingAnalysisONT",
                    "IGOID='" + statsONT.getIgoId() + "' AND Flowcell='" + statsONT.getFlowcell() +"'", user);
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

            drm.storeAndCommit("Add/Update ONT Stats", null, user);
            return "Added/Updated " + statsONT;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return e.getMessage();
        }
    }
}