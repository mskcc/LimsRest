package org.mskcc.limsrest.service.sequencingqc;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.IlluminaSeqExperimentModel;
import com.velox.sloan.cmo.recmodels.IndexBarcodeModel;
import com.velox.sloan.cmo.recmodels.SampleModel;
import com.velox.sloan.cmo.recmodels.SeqAnalysisSampleQCModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.controller.sequencingqc.SequencingStats;

import java.awt.dnd.DropTarget;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;

import static org.mskcc.limsrest.util.Utils.*;

public class UpdateTenXSampleLevelStatsTask extends SequencingStats {

    private Log log = LogFactory.getLog(UpdateLimsSampleLevelSequencingQcTask.class);

    DataRecordManager dataRecordManager;
    String appPropertyFile = "/app.properties";
    String inital_qc_status = "Under-Review";
    private ConnectionLIMS conn;
    User user;

    private String runId;
    private String projectId;

    public UpdateTenXSampleLevelStatsTask(String runId, ConnectionLIMS conn) {
        this.runId = runId;
        this.conn = conn;
    }

    public Map<String, String> execute() {
        VeloxConnection vConn = conn.getConnection();
        user = vConn.getUser();
        dataRecordManager = vConn.getDataRecordManager();
        user = conn.getConnection().getUser();

        Map<String, String> statsAdded = new HashMap<>();
        generateStats(runId, projectId, statsAdded);
        return statsAdded;
    }

    public void generateStats(String runId, String projectId, Map<String, String> stats) {
        Map<String, Object> tenXQCDataVals = new HashMap<>();
        JSONObject tenXData = getTenXStatsFromDb();
        if (tenXData.keySet().size() == 0) {
            log.error(String.format("Found no 10X NGS-STATS for run with run id %s using url %s", runId, getTenXStatsUrl()));
        }
        for (String key : tenXData.keySet()) {
            tenXQCDataVals = getTenXQcValues(tenXData);
            String sampleId = String.valueOf(tenXQCDataVals.get("SampleId")); // aka base IGO ID
            String requestId = String.valueOf(tenXQCDataVals.get("Request"));
            List<DataRecord> relatedLibrarySamples = getRelatedLibrarySamples(runId);
            log.info(String.format("10X Stats: Total Related Library Samples for run %s: %d", runId, relatedLibrarySamples.size()));
            DataRecord librarySample = getLibrarySample(relatedLibrarySamples, sampleId);
            String igoId = getRecordStringValue(librarySample, SampleModel.SAMPLE_ID, user);
            tenXQCDataVals.put(SampleModel.SAMPLE_ID, igoId);
            log.info(String.format("Adding new %s child record to %s with SampleId %s, values are : %s",
                    SeqAnalysisSampleQCModel.DATA_TYPE_NAME,
                    SampleModel.DATA_TYPE_NAME,
                    getRecordStringValue(librarySample, SampleModel.SAMPLE_ID, user),
                    tenXQCDataVals.toString()));
            try {
                List<DataRecord> requestList = dataRecordManager.queryDataRecords("Request", "RequestId = '" + requestId + "'", user);
                DataRecord requestDataRecord = requestList.get(0);
                DataRecord newSeqAnalysisDataRec = librarySample.addChild(SeqAnalysisSampleQCModel.DATA_TYPE_NAME, tenXQCDataVals, user);
                stats.putIfAbsent(tenXQCDataVals.get(SampleModel.SAMPLE_ID).toString(), "");
                stats.put(tenXQCDataVals.get(SampleModel.SAMPLE_ID).toString(), tenXQCDataVals.toString());
            } catch (Exception e) {
                String error = String.format("Failed to add new %s DataRecord Child for %s. ERROR: %s%s", SampleModel.OTHER_SAMPLE_ID, sampleId,
                        ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e));
                log.error(error);
            }
        }
        return;
    }

    /**
     * Method to get 10X stats url from ngs-stats DB.
     * */
    private String getTenXStatsUrl() {
        Properties properties = new Properties();
        String delphiRestUrl;
        try {
            properties.load(new FileReader(getResourceFile(appPropertyFile).replaceAll("%23", "#")));
            delphiRestUrl = properties.getProperty("delphiRestUrl");
        } catch (IOException e) {
            log.error(String.format("Error while parsing properties file:\n%s,%s", ExceptionUtils.getRootCauseMessage(e), ExceptionUtils.getStackTrace(e)));
            return null;
        }
        return StringUtils.join(delphiRestUrl, "ngs-stats/get10xStats?run=", this.runId);
    }

    /**
     * Method to get path to property file.
     *
     * @param propertyFile
     * @return
     */
    private String getResourceFile(String propertyFile) {
        return UpdateLimsSampleLevelSequencingQcTask.class.getResource(propertyFile).getPath();
    }

    /**
     * Get 10X run stats from ngs-stats DB.
     **/
    private JSONObject getTenXStatsFromDb() {
        HttpURLConnection con;
        String url = getTenXStatsUrl();
        StringBuilder response = new StringBuilder();
        try {
            assert url != null;
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            con.disconnect();
            JSONArray jsonArray = new JSONArray(response.toString());
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("10X stats", jsonArray);
            return jsonObject;
        } catch (Exception e) {
            log.info(String.format("Error while querying ngs-stats endpoint using url %s.\n%s:%s", url, ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e)));
            return new JSONObject();
        }
    }

    /**
     * Get and pars JSON 10X stats object into LIMS tenXstats table.
     * */
    private Map<String, Object> getTenXQcValues(JSONObject tenXStatsData) {
        String sampleId = getIgoId(String.valueOf(tenXStatsData.get("sampleId")));
        log.info("10X QC Vals Sample ID: " + sampleId);
        String otherSampleId = getIgoSampleName(String.valueOf(tenXStatsData.get("otherSampleId")));
        String sequencerRunFolder = getVersionLessRunId(String.valueOf(tenXStatsData.get("sequencerRunFolder")));
        String seqQCStatus = inital_qc_status;

        Long antibodyReadsPerCell = tenXStatsData.get("ANTIBODY_READS_PER_CELL") != JSONObject.NULL ? (Long) tenXStatsData.get("ANTIBODY_READS_PER_CELL") : 0;
        int cellNumber = tenXStatsData.get("CELL_NUMBER") != JSONObject.NULL ? (Integer) tenXStatsData.get("CELL_NUMBER") : 0;
        int chCellNumber = tenXStatsData.get("CH_CELL_NUMBER") != JSONObject.NULL ? (Integer) tenXStatsData.get("CH_CELL_NUMBER") : 0;
        String cellsAssignedToSample = tenXStatsData.get("CELLS_ASSIGNED_TO_SAMPLE") != JSONObject.NULL ? (String) tenXStatsData.get("CELLS_ASSIGNED_TO_SAMPLE") : "";
        int fractionUnrecognized = tenXStatsData.get("ABC_CH_FRACTION_UNRECOGNIZED") != JSONObject.NULL ? (Integer) tenXStatsData.get("ABC_CH_FRACTION_UNRECOGNIZED") : 0;
        int meanReadsPerCell = tenXStatsData.get("MEAN_READS_PER_CELL") != JSONObject.NULL ? (Integer) tenXStatsData.get("MEAN_READS_PER_CELL") : 0;
        int chMeanReadsPerCell = tenXStatsData.get("CH_MEAN_READS_PER_CELL") != JSONObject.NULL ? (Integer) tenXStatsData.get("CH_MEAN_READS_PER_CELL") : 0;
        int atacMeanRawReadsPerCell = tenXStatsData.get("ATAC_MEAN_RAW_READS_PER_CELL") != JSONObject.NULL ? (Integer) tenXStatsData.get("ATAC_MEAN_RAW_READS_PER_CELL") : 0;
        int meanReadsPerSpot = tenXStatsData.get("MEAN_READS_PER_SPOT") != JSONObject.NULL ? (Integer) tenXStatsData.get("MEAN_READS_PER_SPOT") : 0;
        int medianUMIsPerCellBarcode = tenXStatsData.get("MEDIAN_CH_UMIs_PER_CELL_BARCODE") != JSONObject.NULL ? (Integer) tenXStatsData.get("MEDIAN_CH_UMIs_PER_CELL_BARCODE") : 0;
        int medianGenesOrFragmentsPerCell = tenXStatsData.get("MEDIAN_GENES_OR_FRAGMENTS_PER_CELL") != JSONObject.NULL ? (Integer) tenXStatsData.get("MEDIAN_GENES_OR_FRAGMENTS_PER_CELL") : 0;
        int atacMedianHighQulityFragPerCell = tenXStatsData.get("ATAC_MEDIAN_HIGH_QUALITY_FRAGMENTS_PER_CELL") != JSONObject.NULL ? (Integer) tenXStatsData.get("ATAC_MEDIAN_HIGH_QUALITY_FRAGMENTS_PER_CELL") : 0;
        int medianIGLUmisPerCell = tenXStatsData.get("MEDIAN_IGL_UMIs_PER_CELL") != JSONObject.NULL ? (Integer) tenXStatsData.get("MEDIAN_IGL_UMIs_PER_CELL") : 0;
        int medianTraIghUmisPerCell = tenXStatsData.get("MEDIAN_TRA_IGH_UMIs_PER_CELL") != JSONObject.NULL ? (Integer) tenXStatsData.get("MEDIAN_TRA_IGH_UMIs_PER_CELL") : 0;
        int medianTrbIgkUmisPerCell = tenXStatsData.get("MEDIAN_TRB_IGK_UMIs_PER_CELL") != JSONObject.NULL ? (Integer) tenXStatsData.get("MEDIAN_TRB_IGK_UMIs_PER_CELL") : 0;
        int readsMappedConfidentlyToGenome = tenXStatsData.get("READS_MAPPED_CONFIDENTLY_TO_GENOME") != JSONObject.NULL ? (Integer) tenXStatsData.get("READS_MAPPED_CONFIDENTLY_TO_GENOME") : 0;
        int atacConfidentlyMappedReadsPair = tenXStatsData.get("ATAC_CONFIDENTLY_MAPPED_READ_PAIRS") != JSONObject.NULL ? (Integer) tenXStatsData.get("ATAC_CONFIDENTLY_MAPPED_READ_PAIRS") : 0;
        int readsMappedToTranscriptome = tenXStatsData.get("READS_MAPPED_TO_TRANSCRIPTOME") != JSONObject.NULL ? (Integer) tenXStatsData.get("READS_MAPPED_TO_TRANSCRIPTOME") : 0;
        int vdjReadsMapped = tenXStatsData.get("VDJ_READS_MAPPED") != JSONObject.NULL ? (Integer) tenXStatsData.get("VDJ_READS_MAPPED") : 0;
        int readMappedConfidentlyToProbSet = tenXStatsData.get("READS_MAPPED_CONFIDENTLY_TO_PROBE_SET") != JSONObject.NULL ? (Integer) tenXStatsData.get("READS_MAPPED_CONFIDENTLY_TO_PROBE_SET") : 0;
        int samplesAssignedAtLeastOneCell = tenXStatsData.get("SAMPLES_ASSIGNED_AT_LEAST_ONE_CELL") != JSONObject.NULL ? (Integer) tenXStatsData.get("SAMPLES_ASSIGNED_AT_LEAST_ONE_CELL") : 0;
        int seqSaturation = tenXStatsData.get("SEQUENCING_SATURATION") != JSONObject.NULL ? (Integer) tenXStatsData.get("SEQUENCING_SATURATION") : 0;
        int chSeqSaturation = tenXStatsData.get("CH_SEQUENCING_SATURATION") != JSONObject.NULL ? (Integer) tenXStatsData.get("CH_SEQUENCING_SATURATION") : 0;
        int totalReads = tenXStatsData.get("TOTAL_READS") != JSONObject.NULL ? (Integer) tenXStatsData.get("TOTAL_READS") : 0;
        int chTotalReads = tenXStatsData.get("CH_TOTAL_READS") != JSONObject.NULL ? (Integer) tenXStatsData.get("CH_TOTAL_READS") : 0;
        int atacTotalReads = tenXStatsData.get("ATAC_TOTAL_READS") != JSONObject.NULL ? (Integer) tenXStatsData.get("ATAC_TOTAL_READS") : 0;

        TenXSampleSequencingQc tenXQc = new TenXSampleSequencingQc(sampleId, otherSampleId, sequencerRunFolder,
                seqQCStatus, antibodyReadsPerCell, cellNumber, chCellNumber, cellsAssignedToSample, fractionUnrecognized,
                meanReadsPerCell, chMeanReadsPerCell, atacMeanRawReadsPerCell, meanReadsPerSpot, medianUMIsPerCellBarcode,
                medianGenesOrFragmentsPerCell, atacMedianHighQulityFragPerCell, medianIGLUmisPerCell, medianTraIghUmisPerCell,
                medianTrbIgkUmisPerCell, readsMappedConfidentlyToGenome, atacConfidentlyMappedReadsPair, readsMappedToTranscriptome,
                vdjReadsMapped, readMappedConfidentlyToProbSet, samplesAssignedAtLeastOneCell, seqSaturation, chSeqSaturation,
                totalReads, chTotalReads, atacTotalReads);

        return tenXQc.getTenXSequencingQcValues();
    }
}
