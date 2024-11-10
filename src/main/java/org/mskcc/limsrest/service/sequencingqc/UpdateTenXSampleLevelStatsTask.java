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
    public ConnectionLIMS conn;
    private String runId;

    public UpdateTenXSampleLevelStatsTask(String runId, ConnectionLIMS conn) {
        super(conn);
        this.runId = runId;
    }

    public List<Map<String, String>> execute() {
        VeloxConnection vConn = conn.getConnection();
        user = vConn.getUser();
        dataRecordManager = vConn.getDataRecordManager();
        user = conn.getConnection().getUser();

        List<Map<String, String>> statsAdded = new LinkedList<>();
        generateStats(runId, statsAdded);
        return statsAdded;
    }

    public void generateStats(String runId, List<Map<String, String>> stats) {
        List<Map<String, Object>> tenXQCDataVals = new LinkedList<>();
        JSONObject tenXData = getTenXStatsFromDb();
        tenXQCDataVals = getTenXQcValues(tenXData);
        if (tenXData.keySet().size() == 0) {
            log.error(String.format("Found no 10X NGS-STATS for run with run id %s using url %s", runId, getTenXStatsUrl()));
        }
        for (int i = 0 ; i < tenXQCDataVals.size(); i++) {
            Map<String, Object> eachSampleQcVals = new HashMap<>();
            String sampleId = String.valueOf(eachSampleQcVals.get("SampleId")); // aka base IGO ID
            String requestId = String.valueOf(eachSampleQcVals.get("Request"));
            List<DataRecord> relatedLibrarySamples = getRelatedLibrarySamples(runId);
            log.info(String.format("10X Stats: Total Related Library Samples for run %s: %d", runId, relatedLibrarySamples.size()));
            DataRecord librarySample = getLibrarySample(relatedLibrarySamples, sampleId);
            String igoId = getRecordStringValue(librarySample, SampleModel.SAMPLE_ID, user);
            eachSampleQcVals.put(SampleModel.SAMPLE_ID, igoId);
            log.info(String.format("Adding new %s child record to %s with SampleId %s, values are : %s",
                    SeqAnalysisSampleQCModel.DATA_TYPE_NAME,
                    SampleModel.DATA_TYPE_NAME,
                    getRecordStringValue(librarySample, SampleModel.SAMPLE_ID, user),
                    eachSampleQcVals.toString()));
            try {
                List<DataRecord> requestList = dataRecordManager.queryDataRecords("Request", "RequestId = '" + requestId + "'", user);
                DataRecord requestDataRecord = requestList.get(0);
                DataRecord newSeqAnalysisDataRec = librarySample.addChild(SeqAnalysisSampleQCModel.DATA_TYPE_NAME, eachSampleQcVals, user);
                stats.get(i).putIfAbsent(eachSampleQcVals.get(SampleModel.SAMPLE_ID).toString(), "");
                stats.get(i).put(eachSampleQcVals.get(SampleModel.SAMPLE_ID).toString(), eachSampleQcVals.toString());
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
            System.out.println(jsonObject.toString(4));
            return jsonObject;
        } catch (Exception e) {
            log.info(String.format("Error while querying ngs-stats endpoint using url %s.\n%s:%s", url, ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e)));
            return new JSONObject();
        }
    }

    /**
     * Get and pars JSON 10X stats object into LIMS tenXstats table.
     * */
    private List<Map<String, Object>> getTenXQcValues(JSONObject tenXStatsData) {
        List<Map<String, Object>> tenxQcVals = new LinkedList<>();
        JSONArray arrayOfJsons = tenXStatsData.getJSONArray("10X stats");
        for (int i = 0; i < arrayOfJsons.length(); i++) {
            JSONObject eachSample = arrayOfJsons.getJSONObject(i);
            String sampleId = getIgoId(String.valueOf(eachSample.get("igo_SAMPLE_ID")));
            log.info("10X QC Vals Sample ID: " + sampleId);
            String otherSampleId = getIgoSampleName(String.valueOf(eachSample.get("igo_SAMPLE_ID")));
            String sequencerRunFolder = getVersionLessRunId(String.valueOf(eachSample.get("run_ID")));
            String seqQCStatus = inital_qc_status;

            Long antibodyReadsPerCell = eachSample.get("antibody_READS_PER_CELL") != JSONObject.NULL ? (Long) eachSample.get("antibody_READS_PER_CELL") : 0;
            double cellNumber = eachSample.get("cell_NUMBER") != JSONObject.NULL ? (Double) eachSample.get("cell_NUMBER") : 0;
            double chCellNumber = eachSample.get("ch_CELL_NUMBER") != JSONObject.NULL ? (Double) eachSample.get("ch_CELL_NUMBER") : 0;
            String cellsAssignedToSample = eachSample.get("cells_ASSIGNED_TO_SAMPLE") != JSONObject.NULL ? (String) eachSample.get("cells_ASSIGNED_TO_SAMPLE") : "";
            int fractionUnrecognized = eachSample.get("abc_CH_FRACTION_UNRECOGNIZED") != JSONObject.NULL ? (Integer) eachSample.get("abc_CH_FRACTION_UNRECOGNIZED") : 0;
            double meanReadsPerCell = eachSample.get("mean_READS_PER_CELL") != JSONObject.NULL ? (Double) eachSample.get("mean_READS_PER_CELL") : 0;
            int chMeanReadsPerCell = eachSample.get("ch_MEAN_READS_PER_CELL") != JSONObject.NULL ? (Integer) eachSample.get("ch_MEAN_READS_PER_CELL") : 0;
            double atacMeanRawReadsPerCell = eachSample.get("atac_MEAN_RAW_READS_PER_CELL") != JSONObject.NULL ? (Double) eachSample.get("atac_MEAN_RAW_READS_PER_CELL") : 0;
            double meanReadsPerSpot = eachSample.get("mean_READS_PER_SPOT") != JSONObject.NULL ? (Double) eachSample.get("mean_READS_PER_SPOT") : 0;
            int medianUMIsPerCellBarcode = eachSample.get("median_CH_UMIs_PER_CELL_BARCODE") != JSONObject.NULL ? (Integer) eachSample.get("median_CH_UMIs_PER_CELL_BARCODE") : 0;
            double medianGenesOrFragmentsPerCell = eachSample.get("median_GENES_OR_FRAGMENTS_PER_CELL") != JSONObject.NULL ? (Double) eachSample.get("median_GENES_OR_FRAGMENTS_PER_CELL") : 0;
            double atacMedianHighQulityFragPerCell = eachSample.get("atac_MEDIAN_HIGH_QUALITY_FRAGMENTS_PER_CELL") != JSONObject.NULL ? (Double) eachSample.get("atac_MEDIAN_HIGH_QUALITY_FRAGMENTS_PER_CELL") : 0;
            double medianIGLUmisPerCell = eachSample.get("median_IGL_UMIs_PER_CELL") != JSONObject.NULL ? (Double) eachSample.get("median_IGL_UMIs_PER_CELL") : 0;
            double medianTraIghUmisPerCell = eachSample.get("median_TRA_IGH_UMIs_PER_CELL") != JSONObject.NULL ? (Double) eachSample.get("median_TRA_IGH_UMIs_PER_CELL") : 0;
            double medianTrbIgkUmisPerCell = eachSample.get("median_TRB_IGK_UMIs_PER_CELL") != JSONObject.NULL ? (Double) eachSample.get("median_TRB_IGK_UMIs_PER_CELL") : 0;
            double readsMappedConfidentlyToGenome = eachSample.get("reads_MAPPED_CONFIDENTLY_TO_GENOME") != JSONObject.NULL ? (Double) eachSample.get("reads_MAPPED_CONFIDENTLY_TO_GENOME") : 0;
            double atacConfidentlyMappedReadsPair = eachSample.get("atac_CONFIDENTLY_MAPPED_READ_PAIRS") != JSONObject.NULL ? (Double) eachSample.get("atac_CONFIDENTLY_MAPPED_READ_PAIRS") : 0;
            double readsMappedToTranscriptome = eachSample.get("reads_MAPPED_TO_TRANSCRIPTOME") != JSONObject.NULL ? (Double) eachSample.get("reads_MAPPED_TO_TRANSCRIPTOME") : 0;
            double vdjReadsMapped = eachSample.get("vdj_READS_MAPPED") != JSONObject.NULL ? (Double) eachSample.get("vdj_READS_MAPPED") : 0;
            double readMappedConfidentlyToProbSet = eachSample.get("reads_MAPPED_CONFIDENTLY_TO_PROBE_SET") != JSONObject.NULL ? (Double) eachSample.get("reads_MAPPED_CONFIDENTLY_TO_PROBE_SET") : 0;
            int samplesAssignedAtLeastOneCell = eachSample.get("samples_ASSIGNED_AT_LEAST_ONE_CELL") != JSONObject.NULL ? (Integer) eachSample.get("samples_ASSIGNED_AT_LEAST_ONE_CELL") : 0;
            double seqSaturation = eachSample.get("sequencing_SATURATION") != JSONObject.NULL ? (Double) eachSample.get("sequencing_SATURATION") : 0;
            double chSeqSaturation = eachSample.get("ch_SEQUENCING_SATURATION") != JSONObject.NULL ? (Double) eachSample.get("ch_SEQUENCING_SATURATION") : 0;
            int totalReads = eachSample.get("total_READS") != JSONObject.NULL ? (Integer) eachSample.get("total_READS") : 0;
            int chTotalReads = eachSample.get("ch_TOTAL_READS") != JSONObject.NULL ? (Integer) eachSample.get("ch_TOTAL_READS") : 0;
            int atacTotalReads = eachSample.get("atac_TOTAL_READS") != JSONObject.NULL ? (Integer) eachSample.get("atac_TOTAL_READS") : 0;

            TenXSampleSequencingQc tenXQc = new TenXSampleSequencingQc(sampleId, otherSampleId, sequencerRunFolder,
                    seqQCStatus, antibodyReadsPerCell, cellNumber, chCellNumber, cellsAssignedToSample, fractionUnrecognized,
                    meanReadsPerCell, chMeanReadsPerCell, atacMeanRawReadsPerCell, meanReadsPerSpot, medianUMIsPerCellBarcode,
                    medianGenesOrFragmentsPerCell, atacMedianHighQulityFragPerCell, medianIGLUmisPerCell, medianTraIghUmisPerCell,
                    medianTrbIgkUmisPerCell, readsMappedConfidentlyToGenome, atacConfidentlyMappedReadsPair, readsMappedToTranscriptome,
                    vdjReadsMapped, readMappedConfidentlyToProbSet, samplesAssignedAtLeastOneCell, seqSaturation, chSeqSaturation,
                    totalReads, chTotalReads, atacTotalReads);

            tenxQcVals.add(tenXQc.getTenXSequencingQcValues());
        }
        return tenxQcVals;
    }
}
