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
import org.json.JSONObject;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.util.BasicMail;
import org.mskcc.limsrest.util.IGOTools;

import static org.mskcc.limsrest.util.Utils.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.mskcc.limsrest.util.Utils.getBaseSampleId;
import static org.mskcc.limsrest.util.Utils.getRecordsOfTypeFromParents;

public class UpdateLimsSampleLevelSequencingQcTask {
    private Log log = LogFactory.getLog(UpdateLimsSampleLevelSequencingQcTask.class);

    private final static List<String> POOLED_SAMPLE_TYPES = Collections.singletonList("pooled library");
    private final String POOLEDNORMAL_IDENTIFIER = "POOLEDNORMAL";
    private final String CONTROL_IDENTIFIER = "CTRL";

    DataRecordManager dataRecordManager;
    String appPropertyFile = "/app.properties";
    String inital_qc_status = "Under-Review";
    private ConnectionLIMS conn;
    User user;

    private String runId;
    private String projectId;

    public UpdateLimsSampleLevelSequencingQcTask(String runId, String projectId, ConnectionLIMS conn) {
        this.runId = runId;
        this.conn = conn;
        this.projectId = projectId;
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

    /**
     * Method for Adding/Updating records for sequencing runs and projects
     *
     * @param runId
     * @param projectId
     * @param stats
     */
    private void generateStats(String runId, String projectId, Map<String, String> stats) {
        Map<String, Object> qcDataVals = new HashMap<>();
        if (projectId == null || projectId.isEmpty()) {
            //get stats from ngs-stats db.
            JSONObject data = getStatsFromDb();
            if (data.keySet().size() == 0) {
                log.error(String.format("Found no NGS-STATS for run with run id %s using url %s", runId, getStatsUrl()));
            }
            //get all the Library samples that are present on the run
            List<DataRecord> relatedLibrarySamples = getRelatedLibrarySamples(runId);
            log.info(String.format("Total Related Library Samples for run %s: %d", runId, relatedLibrarySamples.size()));
            //loop through stats data and add/update lims SeqAnalysisSampleQc records
            for (String key : data.keySet()) {
                //get qcDataVals as HashMap
                qcDataVals = getQcValues(data.getJSONObject(key));
                String sampleName = String.valueOf(qcDataVals.get("OtherSampleId"));
                String sampleId = String.valueOf(qcDataVals.get("SampleId"));
                // first find the library sample that is parent of Pool Sample that went on Sequencer.
                DataRecord librarySample = getLibrarySample(relatedLibrarySamples, sampleId);
                if (librarySample == null) {
                    log.error("Could not find related Library Sample for Sample with Stats SampleId: " + sampleId);
                    continue;
                }

                String igoId = getRecordStringValue(librarySample, SampleModel.SAMPLE_ID, user);
                log.info(String.format("Found Library Sample with Sample ID : %s", igoId));
                //add AltId to the values to be updated.
                Object altId = "";
                try {
                    altId = getValueFromDataRecord(librarySample, "AltId", "String", user);
                } catch (Exception e) {
                    log.error(String.format("Failed to retrieve AltId from Library Sample: %s", igoId));
                }
                qcDataVals.putIfAbsent("AltId", altId);
                //check if there is an existing SeqAnalysisSampleQc record. If present update it.
                String versionLessRunId = getVersionLessRunId(runId);
                DataRecord existingQc = getExistingSequencingQcRecord(relatedLibrarySamples, sampleName, igoId, versionLessRunId);
                if (existingQc == null) {
                    log.info(String.format("Existing %s record not found for Sample with Id %s", SeqAnalysisSampleQCModel.DATA_TYPE_NAME, igoId));
                }
                if (existingQc != null) {
                    log.info(String.format("Updating values on existing %s record with OtherSampleId %s, and Record Id %d, values are : %s",
                            SeqAnalysisSampleQCModel.DATA_TYPE_NAME, getRecordStringValue(existingQc, SampleModel.OTHER_SAMPLE_ID, user),
                            existingQc.getRecordId(), qcDataVals.toString()));
                    log.info(String.format("Existing %s datatype Record ID: %d", existingQc.getDataTypeName(), existingQc.getRecordId()));
                    // remove SeqQcStatus Key,Value from new values so that it does not overwrite existing value.
                    qcDataVals.remove("SeqQCStatus");
                    qcDataVals.put(SampleModel.SAMPLE_ID, igoId);
                    // TODO - else has same logic. Remove duplication and move after else?
                    try {
                        existingQc.setFields(qcDataVals, user);
                        stats.putIfAbsent(qcDataVals.get(SampleModel.SAMPLE_ID).toString(), "");
                        stats.put(qcDataVals.get(SampleModel.SAMPLE_ID).toString(), qcDataVals.toString());
                    } catch (ServerException | RemoteException e) {
                        String error = String.format("Failed to modify %s DataRecord: %s. ERROR: %s%s", SampleModel.OTHER_SAMPLE_ID, igoId,
                                ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e));
                        log.error(error);
                    }
                } else { //if there is no existing SeqAnalysisSampleQc record, create a new one on Library Sample
                    qcDataVals.put(SampleModel.SAMPLE_ID, igoId);
                    log.info(String.format("Adding new %s child record to %s with SampleId %s, values are : %s",
                            SeqAnalysisSampleQCModel.DATA_TYPE_NAME,
                            SampleModel.DATA_TYPE_NAME,
                            getRecordStringValue(librarySample, SampleModel.SAMPLE_ID, user),
                            qcDataVals.toString()));
                    try {
                        librarySample.addChild(SeqAnalysisSampleQCModel.DATA_TYPE_NAME, qcDataVals, user);
                        stats.putIfAbsent(qcDataVals.get(SampleModel.SAMPLE_ID).toString(), "");
                        stats.put(qcDataVals.get(SampleModel.SAMPLE_ID).toString(), qcDataVals.toString());
                    } catch (ServerException | RemoteException e) {
                        String error = String.format("Failed to add new %s DataRecord Child for %s. ERROR: %s%s", SampleModel.OTHER_SAMPLE_ID, sampleId,
                                ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e));
                        log.error(error);
                    }
                }
            }
        } else {
            try {

                List<DataRecord> relatedLibrarySamples = dataRecordManager.queryDataRecords(SampleModel.DATA_TYPE_NAME,
                        SampleModel.EXEMPLAR_SAMPLE_STATUS +
                                " = 'Completed - Illumina Sequencing' AND " + SampleModel.SAMPLE_ID + " LIKE 'Pool-%' AND " +
                                SampleModel.REQUEST_ID + " LIKE '%" + projectId + "%'", user);

                log.info("relatedLibrarySamples: " + relatedLibrarySamples.size());

                for (DataRecord sample : relatedLibrarySamples) {
                    String sampleName = (String) sample.getDataField("OtherSampleId", user);
                    String sampleId = (String) sample.getDataField("SampleId", user);
                    DataRecord librarySample = getLibrarySample(relatedLibrarySamples, sampleId);
                    if (librarySample == null) {
                        log.error("Could not find related Library Sample for Sample with SampleId: " + sampleId);
                    }

                    String igoId = getRecordStringValue(librarySample, SampleModel.SAMPLE_ID, user);
                    String requestId = IGOTools.requestFromIgoId(igoId);
                    log.info("requestId: " + requestId);
                    log.info(String.format("Found Library Sample with Sample ID : %s", igoId));

                    List<DataRecord> requestList = dataRecordManager.queryDataRecords("Request", "RequestId = '" + projectId + "'", user);
                    if (requestList.size() > 0) {
                        DataRecord[] samples = requestList.get(0).getChildrenOfType("Sample", user);
                        String versionLessRunId = getVersionLessRunId(runId);
                        DataRecord existingQc = getExistingSequencingQcRecord(relatedLibrarySamples, sampleName, igoId, versionLessRunId);
                        if (existingQc == null) {
                            log.info(String.format("Existing %s record not found for Sample with Id %s",
                                    SeqAnalysisSampleQCModel.DATA_TYPE_NAME, igoId));
                            qcDataVals.put(SampleModel.SAMPLE_ID, igoId);
                            log.info("igoId: " + igoId);
                            qcDataVals.put(SeqAnalysisSampleQCModel.OTHER_SAMPLE_ID, sampleName);
                            log.info("Sample name: " + sampleName);
                            qcDataVals.put(SeqAnalysisSampleQCModel.SEQUENCER_RUN_FOLDER, runId);
                            log.info("run id:" + runId);
                            qcDataVals.put(SeqAnalysisSampleQCModel.REQUEST, requestId);
                            log.info("request id:" + requestId);
                            qcDataVals.put("seqQCStatus", inital_qc_status);
                            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                            LocalDateTime now = LocalDateTime.now();
                            qcDataVals.put(SeqAnalysisSampleQCModel.DATE_CREATED, dtf.format(now));

                            try {
                                samples[0].addChild(SeqAnalysisSampleQCModel.DATA_TYPE_NAME, qcDataVals, user);
                                log.info("Added record to seq analysis table other sample id: " + SeqAnalysisSampleQCModel.OTHER_SAMPLE_ID);
                            } catch (ServerException | RemoteException e) {
                                String error = String.format("Failed to add new %s DataRecord Child for %s. ERROR: %s%s", SampleModel.OTHER_SAMPLE_ID, sampleId,
                                        ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e));
                                log.error(error);
                            }
                            stats.put(sampleId, (String) librarySample.getDataField("igoId", user));
                        }
                    }
                }

            } catch (NotFound | IoError | RemoteException e) {
                log.error(String.format("Error while querying sample for finding completed - illumina sequencing, pooled" +
                                " samples with projectId: %s.\n%s:%s", SampleModel.REQUEST_ID, ExceptionUtils.getMessage(e),
                        ExceptionUtils.getStackTrace(e)));
            }

        }
        try {
            dataRecordManager.storeAndCommit(String.format("Added/updated new %s records for Sequencing Run %s",
                    SeqAnalysisSampleQCModel.DATA_TYPE_NAME, runId), null, user);
        } catch (RemoteException | ServerException e) {
            log.error("ERROR Message: " + e.getMessage());
            log.error(String.format("Failed to commit changes for %s\nERROR:\n%s", this.runId, ExceptionUtils.getStackTrace(e)));
            return;
        }

        log.info(String.format("Added/Updated total %d %s records for Sequencing Run %s", stats.size(),
                SeqAnalysisSampleQCModel.DATA_TYPE_NAME, runId));
    }

    /**
     * Method to get url to get stats from run-stats db.
     *
     * @return
     */
    private String getStatsUrl() {
        Properties properties = new Properties();
        String delphiRestUrl;
        try {
            properties.load(new FileReader(getResourceFile(appPropertyFile).replaceAll("%23", "#")));
            delphiRestUrl = properties.getProperty("delphiRestUrl");
        } catch (IOException e) {
            log.error(String.format("Error while parsing properties file:\n%s,%s", ExceptionUtils.getRootCauseMessage(e), ExceptionUtils.getStackTrace(e)));
            return null;
        }
        return StringUtils.join(delphiRestUrl, "ngs-stats/picardstats/run/", this.runId);
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
     * get run Stats from ngs-stats database.
     *
     * @return
     */
    private JSONObject getStatsFromDb() {
        HttpURLConnection con;
        String url = getStatsUrl();
        StringBuilder response = new StringBuilder();
        try {
            assert url != null;
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            con.disconnect();
            return new JSONObject(response.toString());
        } catch (Exception e) {
            log.info(String.format("Error while querying ngs-stats endpoint using url %s.\n%s:%s", url, ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e)));
            return new JSONObject();
        }
    }

    /**
     * get SampleQcValues to store in LIMS from QC Stats JSONObject.
     *
     * @param statsData
     * @return
     */
    private Map<String, Object> getQcValues(JSONObject statsData) {
        String sampleId = getIgoId(String.valueOf(statsData.get("sample")));
        log.info("QC Vals Sample ID: " + sampleId);
        String otherSampleId = getIgoSampleName(String.valueOf(statsData.get("sample")));
        String request = String.valueOf(statsData.get("request"));
        String baitSet = String.valueOf(statsData.get("bait_SET"));
        String sequencerRunFolder = getVersionLessRunId(String.valueOf(statsData.get("run")));
        String seqQCStatus = inital_qc_status;
        long readsExamined = statsData.get("read_PAIRS_EXAMINED") != JSONObject.NULL ? Long.parseLong(String.valueOf(statsData.get("read_PAIRS_EXAMINED"))) : 0;
        long totalReads = statsData.get("TOTAL_READS") != JSONObject.NULL ? Long.parseLong(String.valueOf(statsData.get("TOTAL_READS"))) : 0;
        long unmappedDupes = statsData.get("unmapped_READS") != JSONObject.NULL ? Long.parseLong(String.valueOf(statsData.get("unmapped_READS"))) : 0;
        long readPairDupes = statsData.get("read_PAIRS_DUPLICATES") != JSONObject.NULL ? Long.parseLong(String.valueOf(statsData.get("read_PAIRS_DUPLICATES"))) : 0;
        long unpairedReads = statsData.get("unpairedReads") != JSONObject.NULL ? Long.parseLong(String.valueOf(statsData.get("unpairedReads"))) : 0;
        double meanCoverage = statsData.get("mean_COVERAGE") != JSONObject.NULL ? Double.parseDouble(String.valueOf(statsData.get("mean_COVERAGE"))) : 0.0;
        double meanTargetCoverage = statsData.get("mean_TARGET_COVERAGE") != JSONObject.NULL ? Double.parseDouble(String.valueOf(statsData.get("mean_TARGET_COVERAGE"))) : 0.0;
        double percentTarget100X = statsData.get("pct_TARGET_BASES_100X") != JSONObject.NULL ? Double.parseDouble(String.valueOf(statsData.get("pct_TARGET_BASES_100X"))) : 0.0;
        double percentTarget30X = statsData.get("pct_TARGET_BASES_30X") != JSONObject.NULL ? Double.parseDouble(String.valueOf(statsData.get("pct_TARGET_BASES_30X"))) : 0.0;
        double percentTarget10X = statsData.get("pct_TARGET_BASES_10X") != JSONObject.NULL ? Double.parseDouble(String.valueOf(statsData.get("pct_TARGET_BASES_10X"))) : 0.0;
        double percentAdapters = statsData.get("pct_ADAPTER") != JSONObject.NULL ? Double.parseDouble(String.valueOf(statsData.get("pct_ADAPTER"))) : 0.0;
        double percentCodingBases = statsData.get("pct_CODING_BASES") != JSONObject.NULL ? Double.parseDouble(String.valueOf(statsData.get("pct_CODING_BASES"))) : 0.0;
        double percentExcBaseQ = statsData.get("pct_EXC_BASEQ") != JSONObject.NULL ? Double.parseDouble(String.valueOf(statsData.get("pct_EXC_BASEQ"))) : 0.0;
        double percentExcDupe = statsData.get("pct_EXC_DUPE") != JSONObject.NULL ? Double.parseDouble(String.valueOf(statsData.get("pct_EXC_DUPE"))) : 0.0;
        double percentExcMapQ = statsData.get("pct_EXC_MAPQ") != JSONObject.NULL ? Double.parseDouble(String.valueOf(statsData.get("pct_EXC_MAPQ"))) : 0.0;
        double percentExcTotal = statsData.get("pct_EXC_TOTAL") != JSONObject.NULL ? Double.parseDouble(String.valueOf(statsData.get("pct_EXC_TOTAL"))) : 0.0;
        double percentIntergenicBases = statsData.get("pct_INTERGENIC_BASES") != JSONObject.NULL ? Double.parseDouble(String.valueOf(statsData.get("pct_INTERGENIC_BASES"))) : 0.0;
        double percentIntronicBases = statsData.get("pct_INTRONIC_BASES") != JSONObject.NULL ? Double.parseDouble(String.valueOf(statsData.get("pct_INTRONIC_BASES"))) : 0.0;
        double percentMrnaBases = statsData.get("pct_MRNA_BASES") != JSONObject.NULL ? Double.parseDouble(String.valueOf(statsData.get("pct_MRNA_BASES"))) : 0.0;
        double percentOffBait = statsData.get("pct_OFF_BAIT") != JSONObject.NULL ? Double.parseDouble(String.valueOf(statsData.get("pct_OFF_BAIT"))) : 0.0;
        double percentRibosomalBases = statsData.get("pct_RIBOSOMAL_BASES") != JSONObject.NULL ? Double.parseDouble(String.valueOf(statsData.get("pct_RIBOSOMAL_BASES"))) : 0.0;
        double percentUtrBases = statsData.get("pct_UTR_BASES") != JSONObject.NULL ? Double.parseDouble(String.valueOf(statsData.get("pct_UTR_BASES"))) : 0.0;
        double percentDuplication = statsData.get("percent_DUPLICATION") != JSONObject.NULL ? Double.parseDouble(String.valueOf(statsData.get("percent_DUPLICATION"))) : 0.0;
        double zeroCoveragePercent = statsData.get("zero_CVG_TARGETS_PCT") != JSONObject.NULL ? Double.parseDouble(String.valueOf(statsData.get("zero_CVG_TARGETS_PCT"))) : 0.0;
        double mskq = statsData.get("msk_Q") != JSONObject.NULL ? Double.parseDouble(String.valueOf(statsData.get("msk_Q"))) : 0.0;
        long genomeTerritory = statsData.get("genome_TERRITORY") != JSONObject.NULL ? Long.parseLong(String.valueOf(statsData.get("genome_TERRITORY"))) : 0;
        double gRefOxoQ = statsData.get("g_REF_OXO_Q") != JSONObject.NULL ? Double.parseDouble(String.valueOf(statsData.get("g_REF_OXO_Q"))) : 0.0;
        SampleSequencingQc qc = new SampleSequencingQc(sampleId, otherSampleId, request,
                baitSet, sequencerRunFolder, seqQCStatus, readsExamined,
                totalReads, unmappedDupes, readPairDupes, unpairedReads, meanCoverage,
                meanTargetCoverage, percentTarget100X, percentTarget30X, percentTarget10X,
                percentAdapters, percentCodingBases, percentExcBaseQ, percentExcDupe,
                percentExcMapQ, percentExcTotal, percentIntergenicBases, percentIntronicBases,
                percentMrnaBases, percentOffBait, percentRibosomalBases, percentUtrBases,
                percentDuplication, zeroCoveragePercent, mskq, genomeTerritory, gRefOxoQ);
        return qc.getSequencingQcValues();
    }

    /**
     * Method to extract SampleId (IGO_ID) sampleId value in qc data. Qc data sample name is concatenation of
     * OtherSampleId , _IGO_, SampleID.
     *
     * @param id
     * @return
     */
    private String getIgoId(String id) {
        log.info("Stats IGO ID: " + id);
        List<String> idVals = Arrays.asList(id.split("_IGO_"));
        if (idVals.size() == 2) {
            String igoId = idVals.get(1);
            if (igoId.contains(POOLEDNORMAL_IDENTIFIER) || igoId.contains(CONTROL_IDENTIFIER)) {
                String pooledNormalName = idVals.get(0);
                String[] barcodeVals = idVals.get(1).split("_");
                String barcode = barcodeVals[barcodeVals.length - 1];
                return pooledNormalName + "_" + barcode;
            }
            return igoId;
        } else {
            throw new IllegalArgumentException(String.format("Cannot extract IGO ID from given Sample Name value %s in QC data.", id));
        }
    }

    /**
     * Method to extract SampleId (IGO_ID) sampleId value in qc data. Qc data sample name is concatenation of
     * OtherSampleId , _IGO_, SampleID. And for POOLEDNORMALS it is 'POOLEDNORMAL', _IGO_, RECIPE, i7 barcode.
     *
     * @param id
     * @return
     */
    private String getIgoSampleName(String id) {
        List<String> idVals = Arrays.asList(id.split("_IGO_"));
        if (idVals.size() == 2) {
            return idVals.get(0).replaceFirst("_rescue$", "");
        } else {
            throw new IllegalArgumentException(String.format("Cannot extract IGO ID from given Sample Name value %s in QC data.", id));
        }
    }

    /**
     * Method to get the SeqAnalysisSampleQC DataRecord for a sample if already exists.
     *
     * @param librarySamples
     * @param otherSampleId
     * @param igoId
     * @param runId
     * @return
     */
    private DataRecord getExistingSequencingQcRecord(List<DataRecord> librarySamples, String otherSampleId, String igoId, String runId) {
        List<DataRecord> seqAnalysisSampleQCs;
        try {
            log.info(String.format("Searching for existing %s records for %s, %s and %s combination", SeqAnalysisSampleQCModel.DATA_TYPE_NAME, igoId, otherSampleId, runId));
            if (igoId.contains(POOLEDNORMAL_IDENTIFIER) || igoId.contains(CONTROL_IDENTIFIER) || otherSampleId.contains(POOLEDNORMAL_IDENTIFIER)) {
                String[] igoIdVals = igoId.split("_");
                String barcode = igoIdVals[igoIdVals.length - 1];
                DataRecord librarySample = getPooledNormalLibrarySample(librarySamples, barcode);
                assert librarySample != null;
                if (librarySample.getChildrenOfType(SeqAnalysisSampleQCModel.DATA_TYPE_NAME, user) != null && librarySample.getChildrenOfType(SeqAnalysisSampleQCModel.DATA_TYPE_NAME, user).length > 0) {
                    DataRecord qcrec = librarySample.getChildrenOfType(SeqAnalysisSampleQCModel.DATA_TYPE_NAME, user)[0];
                    log.info(String.format("Found %s record with recordid: %d for poolednormal sample.", SeqAnalysisSampleQCModel.DATA_TYPE_NAME, qcrec.getRecordId()));
                    return qcrec;
                }
            }
            seqAnalysisSampleQCs = dataRecordManager.queryDataRecords(SeqAnalysisSampleQCModel.DATA_TYPE_NAME, SeqAnalysisSampleQCModel.OTHER_SAMPLE_ID +
                    " = '" + otherSampleId + "' AND " + SeqAnalysisSampleQCModel.SEQUENCER_RUN_FOLDER + " = '" + runId + "' AND SampleId = '" + igoId + "'", user);
            log.info("Existing qc records: " + seqAnalysisSampleQCs.size());
        } catch (Exception e) {
            log.error(String.format("Exception thrown while retrieving SeqAnalysisSampleQC records: %s", ExceptionUtils.getStackTrace(e)));
            return null;
        }
        if (seqAnalysisSampleQCs.size() > 0) {
            log.info(String.format("Found already existing SeqAnalysisSampleQC for sample with Sample Name: %s and run: %s",
                    otherSampleId, runId));
            return seqAnalysisSampleQCs.get(0);
        }
        return null;
    }

    /**
     * Method to get all the Library Samples related to Sequencing Run.
     *
     * @param runId
     * @return
     */
    private List<DataRecord> getRelatedLibrarySamples(String runId) {
        Set<String> addedSampleIds = new HashSet<>();
        List<DataRecord> flowCellSamples = new ArrayList<>();
        try {
            List<DataRecord> illuminaSeqExperiments = dataRecordManager.queryDataRecords(IlluminaSeqExperimentModel.DATA_TYPE_NAME, IlluminaSeqExperimentModel.SEQUENCER_RUN_FOLDER + " LIKE '%" + runId + "%'", user);
            List<DataRecord> relatedSamples = getSamplesRelatedToSeqExperiment(illuminaSeqExperiments, runId, user);
            log.info(String.format("Total Related Samples for IlluminaSeq Run %s: %d", runId, relatedSamples.size()));
            Stack<DataRecord> sampleStack = new Stack<>();
            if (relatedSamples.isEmpty()) {
                return flowCellSamples;
            }
            sampleStack.addAll(relatedSamples);
            do {
                DataRecord stackSample = sampleStack.pop();
                Object sampleType = stackSample.getValue(SampleModel.EXEMPLAR_SAMPLE_TYPE, user);
                Object samId = stackSample.getValue(SampleModel.SAMPLE_ID, user);
                log.info("Sample Type: " + sampleType);
                if (!samId.toString().toLowerCase().startsWith("pool") || (sampleType != null && !POOLED_SAMPLE_TYPES.contains(sampleType.toString().toLowerCase()))) {
                    String sampleId = stackSample.getStringVal(SampleModel.SAMPLE_ID, user);
                    log.info("Adding sample to Library Samples List: " + sampleId);
                    if (addedSampleIds.add(sampleId)) {
                        flowCellSamples.add(stackSample);
                    }
                } else {
                    sampleStack.addAll(stackSample.getParentsOfType(SampleModel.DATA_TYPE_NAME, user));
                }
            } while (!sampleStack.isEmpty());
        } catch (NotFound | RemoteException | IoError | NullPointerException notFound) {
            log.error(String.format("%s-> Error while getting related Library Samples for run %s:\n%s", ExceptionUtils.getRootCauseMessage(notFound), runId, ExceptionUtils.getStackTrace(notFound)));
        }
        log.info(String.format("Total Samples related to run %d , Sample Ids: %s", flowCellSamples.size(), Arrays.toString(addedSampleIds.toArray())));
        return flowCellSamples;
    }

    /**
     * Method to get a Library Sample from List of Library Samples related to the Sequencing Run with matching SampleId.
     *
     * @param relatedLibrarySamples
     * @return
     */
    private DataRecord getLibrarySample(List<DataRecord> relatedLibrarySamples, String sampleId) {
        try {
            for (DataRecord sample : relatedLibrarySamples) {
                String baseId = getBaseSampleId(sample.getStringVal(SampleModel.SAMPLE_ID, user));
                if (baseId.equalsIgnoreCase(sampleId)) {
                    return sample;
                }
                // Older SampleId values started with "CTRL" for POOLEDNORMAL control Samples. With last update, the control Samples now
                // start with actual control sample type eg "FFPEPOOLEDNORMAL, MOUSEPOOLEDNORMAL etc." we need to validate both old and new pattern
                // for POOLEDNORMAL control samples.
                if ((sampleId.contains(POOLEDNORMAL_IDENTIFIER) && baseId.toUpperCase().contains(POOLEDNORMAL_IDENTIFIER))
                        || (sampleId.contains(CONTROL_IDENTIFIER) && baseId.toUpperCase().contains(CONTROL_IDENTIFIER))) {
                    String[] pooleNormalIdVals = sampleId.split("_");
                    assert pooleNormalIdVals.length > 1;
                    String barcode = pooleNormalIdVals[pooleNormalIdVals.length - 1];
                    return getPooledNormalLibrarySample(relatedLibrarySamples, barcode);
                }
            }
        } catch (NotFound | RemoteException notFound) {
            log.error(String.format("%s-> Error while retrieving matching sample by sample id '%s' from list of samples:\n%s", ExceptionUtils.getRootCauseMessage(notFound), sampleId, ExceptionUtils.getStackTrace(notFound)));
        }
        return null;
    }

    /**
     * Method to get parent poolednormal sample based on barcode. NGS-STATS sampleid for poolednormals is named to contain
     * type of normal (POOLEDNORMAL, FFPEPOOLEDNORMAL, MOUSEPOOLEDNORMAL etc.), Recipe and Index barcode (eg: FFPEPOOLEDNORMAL_IGO_IMPACT468_GTGAAGTG)
     * The only way to find correct pooled normal is to traverse through the library samples on the run and find pooled normal with same barcode.
     *
     * @param relatedLibrarySamples
     * @param barcode
     * @return
     */
    private DataRecord getPooledNormalLibrarySample(List<DataRecord> relatedLibrarySamples, String barcode) {
        try {
            log.info("Given POOLEDNORMAL barcode: " + barcode);
            for (DataRecord sam : relatedLibrarySamples) {
                Object sampleId = sam.getStringVal(SampleModel.SAMPLE_ID, user);
                Object otherSampleId = sam.getStringVal(SampleModel.OTHER_SAMPLE_ID, user);
                if ((sampleId != null && sampleId.toString().contains(POOLEDNORMAL_IDENTIFIER)) || (otherSampleId != null && otherSampleId.toString().contains(POOLEDNORMAL_IDENTIFIER))) {
                    List<DataRecord> indexBarcodeRecs = getRecordsOfTypeFromParents(sam, SampleModel.DATA_TYPE_NAME, IndexBarcodeModel.DATA_TYPE_NAME, user);
                    if (!indexBarcodeRecs.isEmpty()) {
                        Object samBarcode = indexBarcodeRecs.get(0).getValue(IndexBarcodeModel.INDEX_TAG, user);
                        log.info(indexBarcodeRecs.get(0).getDataTypeName());
                        log.info("IndexBarcode SampleId: " + indexBarcodeRecs.get(0).getStringVal("SampleId", user));
                        log.info("Assigned Index Barcode POOLEDNORMAL: " + samBarcode);
                        if (samBarcode != null) {
                            String i7Barcode = samBarcode.toString().split("-")[0];
                            if (i7Barcode.equalsIgnoreCase(barcode)) {
                                log.info("Found Library Sample for Pooled Normal");
                                return sam;
                            }
                        }
                    }
                }
            }
        } catch (NotFound | RemoteException | NullPointerException notFound) {
            log.error(String.format("%s-> Error while getting related Library Samples for run %s:\n%s", ExceptionUtils.getRootCauseMessage(notFound), runId, ExceptionUtils.getStackTrace(notFound)));
        }
        return null;
    }

    /**
     * Method to get Run Name without Version Number
     *
     * @param runName
     * @return
     */
    private String getVersionLessRunId(String runName) {
        return runName.replaceFirst("_[A-Z][0-9]+$", "");
    }
}
