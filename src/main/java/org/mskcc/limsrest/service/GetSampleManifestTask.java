package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.KAPALibPlateSetupProtocol1Model;
import com.velox.sloan.cmo.recmodels.SampleModel;
import com.velox.sloan.cmo.recmodels.SeqAnalysisSampleQCModel;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.model.SampleManifest;
import org.mskcc.limsrest.util.IGOTools;
import org.mskcc.limsrest.util.Utils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Traverse the LIMS & ngs_stats database to find all sample level metadata required for CMO pipelines.<BR>
 * The sample metadata pipeline requirements were originally gathered for running IMPACT, ACCESS & WES pipelines with
 * some special considerations for BIC
 */
public class GetSampleManifestTask {
    private static Log log = LogFactory.getLog(GetSampleManifestTask.class);

    private ConnectionLIMS conn;

    protected String [] igoIds;

    public GetSampleManifestTask(String [] igoIds, ConnectionLIMS conn) {
        this.igoIds = igoIds;
        this.conn = conn;
    }

    public static class SampleManifestResult {
        public List<SampleManifest> smList;
        public String error = null;

        public SampleManifestResult(List<SampleManifest> smList, String error) {
            this.smList = smList;
            this.error = error;
        }
    }

    /**
     * Any version of IMPACT, HemePact, ACCESS or Whole Exome
     * @param recipe
     * @return
     */
    public static boolean isPipelineRecipe(String recipe) {
        if (recipe == null)
            return false;
        // WES = 'WholeExomeSequencing', 'AgilentCapture_51MB', 'IDT_Exome_v1_FP', 'Agilent_v4_51MB_Human'
        if (recipe.contains("PACT") || recipe.contains("ACCESS") || recipe.contains("Exome") || recipe.contains("51MB")
                || recipe.contains("ShallowWGS") || recipe.contains("CMO-CH")) {
            return true;
        }
        return false;
    }

    public SampleManifestResult execute() {
        long startTime = System.currentTimeMillis();

        VeloxConnection vConn = conn.getConnection();
        User user = vConn.getUser();
        DataRecordManager dataRecordManager = vConn.getDataRecordManager();

        try {
            List<SampleManifest> smList = new ArrayList<>();
            for (String igoId : igoIds) {
                smList.add(getSampleManifest(igoId, user, dataRecordManager));
            }
            log.info("Manifest generation time(ms):" + (System.currentTimeMillis() - startTime));
            return new SampleManifestResult(smList, null);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return null;
        }
    }

    /**
     * 06260_G_128 Currently Failing because archive has 06260_G_128_1_1
     * @param igoId
     * @param user
     * @param dataRecordManager
     * @return
     * @throws Exception
     */
    protected SampleManifest getSampleManifest(String igoId, User user, DataRecordManager dataRecordManager)
            throws Exception {
        log.info("Searching Sample table for SampleId ='" + igoId + "'");
        List<DataRecord> samples = dataRecordManager.queryDataRecords("Sample", "SampleId = '" + igoId + "'", user);
        if (samples.size() == 0) { // sample not found in sample table
            // TODO REMOVE this special case for 06302_AO and change the igo lims if it is correct
            // igo made a custom 06302_AO sample sheet with samples that are not in LIMS, they have a 06302_X parent
            if (igoId.contains("06302_AO_")) {
                String igoIdX = igoId.replace("_AO", "_X");
                List<DataRecord> samples_06302 = dataRecordManager.queryDataRecords("Sample", "SampleId = '" + igoIdX + "'", user);
                DataRecord sampleX = samples_06302.get(0);
                // query CMO level fields from 06302_X
                log.info("SPECIAL CASE - Querying sample CMO info records for: " + igoIdX);
                SampleManifest sampleManifest = setSampleCMOLevelFields(igoIdX, sampleX, samples_06302, dataRecordManager, user);
                // query fastqs for 06302_AO
                sampleManifest.setIgoId(igoId);
                sampleManifest.setBaitSet("MSK-ACCESS-v1_0-probesAllwFP_hg37_sort-BAITS");
                sampleManifest.setSpecimenType("cfDNA");
                SampleManifest result = fastqsOnlyManifest(sampleManifest, new HashSet<>());
                SampleSheet sampleSheet = map06302_AO.get(igoId);
                SampleManifest.Library l = result.getLibraries().toArray(new SampleManifest.Library[1])[0];
                l.barcodeId = sampleSheet.barcodeId;
                l.barcodeIndex = sampleSheet.barcodeIndex1+"-"+sampleSheet.barcodeIndex2;
                l.captureName = "Pool-06302_AO-A8_1";
                l.runs.get(0).setReadLength("101/8/8/101");
                l.runs.get(0).setRunMode("NovaSeq S4");
                Integer array[] = {1, 2, 3, 4};
                l.runs.get(0).setFlowCellLanes(Arrays.asList(array));
                setACCESS2dBarcode(user, dataRecordManager, sampleX, sampleManifest);
                sampleManifest.setTubeId(getTubeId(sampleX, user));

                return result;
            }
            return new SampleManifest();
        }
        DataRecord sample = samples.get(0);
        // fastq is named by sample level field not cmo record in case of a sample swap such as 07951_I_12
        String origSampleName = sample.getStringVal("OtherSampleId", user);

        String recipe = sample.getStringVal(SampleModel.RECIPE, user);
        // for example 07951_S_50_1 is Fingerprinting sample, skip for pipelines for now
        if ("Fingerprinting".equals(recipe))
            return new SampleManifest();
        // Recipe in LIMS is setup differently for the CMO-CH panel which is submitted as a CustomCapture with "CMO-CH" panel
        if ("CustomCapture".equals(recipe))  {
            // copy RUN-QC logic to get recipe from the last aliquot prior to pooling which is CMO-CH, for example project 12405_C
            List<DataRecord> qcRecords = dataRecordManager.queryDataRecords("SeqAnalysisSampleQC", "Request = '" + IGOTools.requestFromIgoId(igoId) + "'", user);
            if (qcRecords.size() > 0) {  // SeqAnalysisSampleQC will only have records after the samples are sequenced
                DataRecord parentSample = qcRecords.get(0).getParentsOfType("Sample", user).get(0);
                recipe = parentSample.getStringVal(SampleModel.RECIPE, user);
                log.info("Updated recipe from CustomCapture to " + recipe);
            }
        }

        SampleManifest sampleManifest = setSampleCMOLevelFields(igoId, sample, samples, dataRecordManager, user);
        // set fields specifically required for generating CMO style sample names they use to run pipelines
        sampleManifest.getCmoSampleIdFields().setRecipe(recipe);
        String naToExtract = sample.getStringVal("NAtoExtract", user);
        sampleManifest.getCmoSampleIdFields().setNaToExtract(naToExtract);
        String sampleType = sample.getStringVal("ExemplarSampleType", user);
        sampleManifest.getCmoSampleIdFields().setSampleType(sampleType);
        sampleManifest.setTubeId(getTubeId(sample, user));

        if (!isPipelineRecipe(recipe)) {
            return getFastqsAndCheckTheirQCStatus(igoId, user, sample, sampleManifest);
        }

        addIGOQcRecommendations(sampleManifest, sample, user);

        // query Picard QC records for bait set & "Passed" fastqs.
        List<DataRecord> qcs = sample.getDescendantsOfType(SeqAnalysisSampleQCModel.DATA_TYPE_NAME, user);
        Set<String> runPassedQC = new HashSet<>();
        String baitSet = null;
        Long dateBaitSetCreated = Long.MAX_VALUE;
        for (DataRecord dr : qcs) {
            String qcResult = dr.getStringVal(SeqAnalysisSampleQCModel.SEQ_QCSTATUS, user);
            if ("Passed".equals(qcResult)) {
                String run = dr.getStringVal(SeqAnalysisSampleQCModel.SEQUENCER_RUN_FOLDER, user);
                runPassedQC.add(run);
                log.info("Passed sample & run: " + run);
                // make sure to get correct baitset when samples are moved downstream i.e. 09687_N_8 WES has 09687_T_1 Methlyseq child
                // choose earliest created baitset to avoid later baitsets from other requests
                Long datecreated = dr.getLongVal(SeqAnalysisSampleQCModel.DATE_CREATED, user);
                if (datecreated < dateBaitSetCreated) {
                    baitSet = dr.getStringVal(SeqAnalysisSampleQCModel.BAIT_SET, user);
                    log.info("Saving baitSet: " + baitSet);
                    dateBaitSetCreated = datecreated;
                }
            }
        }

        if (baitSet == null || baitSet.isEmpty())
            log.warn("Missing bait set: " + igoId);
        sampleManifest.setBaitSet(baitSet);

        List<DataRecord> aliquots = sample.getDescendantsOfType("Sample", user);
        // 07260 Request the first sample are DNA Libraries like 07260_1 so can't just search descendants to find DNA libraries
        aliquots.add(sample);
        Map<String, LibraryDataRecord> dnaLibraries = findDNALibraries(aliquots, sampleManifest.getIgoId(), user);
        log.info("DNA Libraries found: " + dnaLibraries.size());

        // For these projects from 2016, LIMS DNA libraries are marked differently than newer projects.  In order to
        // find and return all fastq.gz files set the DNA Library as the base IGO ID
        Set<String> specialIgoIds = new HashSet<>(Arrays.asList(
                "06049_I_29", "06049_I_19", "06049_E_14", "06049_E_12", "06208_B_3", "06208_B_22", "06208_3", "93017_D_51", "06058_U_6", "05428_W_1"));
        if (dnaLibraries.size() == 0 || specialIgoIds.contains(igoId)) {
            dnaLibraries.clear();
            // 05500_FQ_1 was submitted as a pooled library, try to find fastqs
            log.info("No DNA libraries found, searching from base IGO ID.");
            dnaLibraries.put(igoId, new LibraryDataRecord(sample));
        }

        Double dnaInputNg = null;  // leave null for WES & IMPACT
        if (recipe.contains("ACCESS") || recipe.contains("CMO-CH")) {
            dnaInputNg = setACCESS2dBarcode(user, dataRecordManager, sample, sampleManifest);
        }

        // for each DNA Library traverse the records grab the fields we need and paths to fastqs.
        for (Map.Entry<String, LibraryDataRecord> aliquotEntry : dnaLibraries.entrySet()) {
            String libraryIgoId = aliquotEntry.getKey();
            DataRecord aliquot = aliquotEntry.getValue().record;
            DataRecord aliquotParent = aliquotEntry.getValue().parent;
            log.info("Processing DNA library: " + libraryIgoId);
            SampleManifest.Library library;
            if (recipe.contains("CMO-CH"))
                library = getLibraryFields(user, libraryIgoId, aliquotParent, dnaInputNg);
            else
                library = getLibraryFields(user, libraryIgoId, aliquot, dnaInputNg);

            if (recipe.contains("ACCESS") || recipe.contains("CMO-CH")) {
                List<DataRecord> indexBarcodes = aliquot.getDescendantsOfType("IndexBarcode", user);
                if (indexBarcodes == null || indexBarcodes.size() == 0) {
                    List<DataRecord> parentList = aliquot.getParentsOfType("Sample", user);
                    indexBarcodes = parentList.get(parentList.size() - 1).getDescendantsOfType("IndexBarcode", user);
                    log.info("indexBarcodes == null");
                }
                if (indexBarcodes != null && indexBarcodes.size() > 0) {
                    DataRecord bc = indexBarcodes.get(0);
                    library.setBarcodeId(bc.getStringVal("IndexId", user));
                    library.setBarcodeIndex(bc.getStringVal("IndexTag", user));
                    log.info("indexBarcodes != null");
                }
            }

            if (recipe.contains("CMO-CH")) { // capture info is in different protocol for CMO-CH
                List<DataRecord> records = aliquotParent.getDescendantsOfType("KAPAAgilentCaptureProtocol1", user);
                log.info("Found capture records: " + records.size());
                for (DataRecord n : records) {
                    Object valid = n.getValue("Valid", user); // 05359_B_1 null
                    if (valid != null && new Boolean(valid.toString())) {
                        String poolName = "";
                        Object val = n.getValue("Aliq1TargetMass", user);
                        if (val != null) {
                            Double captureInput = n.getDoubleVal("Aliq1TargetMass", user);
                            library.setCaptureInputNg(captureInput.toString());
                            library.setCaptureName(poolName);
                            Double captureVolume = n.getDoubleVal("Aliq1StartingConcentration", user);
                            library.setCaptureConcentrationNm(captureVolume.toString());
                            sampleManifest.addLibrary(library);
                        }
                    } else {
                        log.warn("Capture records not valid.");
                    }
                }
            } else {
                // bait set recipe, capture input, capture name
                List<DataRecord> nimbleGen = aliquot.getDescendantsOfType("NimbleGenHybProtocol", user);
                log.info("Found nimbleGen records: " + nimbleGen.size());
                for (DataRecord n : nimbleGen) {
                    Object valid = n.getValue("Valid", user); // 05359_B_1 null
                    if (valid != null && new Boolean(valid.toString())) {
                        String poolName = n.getStringVal("Protocol2Sample", user);
                        String baitSetRecipe = n.getStringVal("Recipe", user); // LIMS display name "bait set"
                        Object val = n.getValue("SourceMassToUse", user);
                        if (val != null) {
                            Double captureInput = n.getDoubleVal("SourceMassToUse", user);
                            library.setCaptureInputNg(captureInput.toString());
                            library.setCaptureName(poolName);
                            Double captureVolume = n.getDoubleVal("VolumeToUse", user);
                            library.setCaptureConcentrationNm(captureVolume.toString());
                            sampleManifest.addLibrary(library);
                        }
                    } else {
                        log.warn("Nimblegen records not valid.");
                    }
                }
            }


            // for each flow cell ID a sample may be on multiple lanes
            // (currently all lanes are demuxed to same fastq file)
            Map<String, SampleManifest.Run> runsMap = new HashMap<>();
            // run Mode, runId, flow Cell & Lane Number
            // Flow Cell Lanes are far down the sample/pool hierarchy in LIMS
            List<DataRecord> reqLanes = aliquot.getDescendantsOfType("FlowCellLane", user);
            if (reqLanes.isEmpty()) {
                log.info("No flow cell lane info found for: " + aliquot.getStringVal("SampleId", user));
                if (aliquotParent != null)
                    reqLanes = aliquotParent.getDescendantsOfType("FlowCellLane", user);
            }
            for (DataRecord flowCellLane : reqLanes) {
                Integer laneNum = ((Long) flowCellLane.getLongVal("LaneNum", user)).intValue();
                log.info("Reviewing flow cell lane: " + laneNum);
                List<DataRecord> flowcell = flowCellLane.getParentsOfType("FlowCell", user);
                if (flowcell.size() > 0) {
                    log.info("Getting a flow cell");
                    List<DataRecord> possibleRun = flowcell.get(0).getParentsOfType("IlluminaSeqExperiment", user);
                    if (possibleRun.size() > 0) {
                        DataRecord seqExperiment = possibleRun.get(0);
                        String runMode = seqExperiment.getStringVal("SequencingRunMode", user);
                        String flowCellId = seqExperiment.getStringVal("FlowcellId", user);
                        // TODO NOTE: ReadLength blank in LIMS prior to April 2019
                        String readLength = seqExperiment.getStringVal("ReadLength", user);

                        // TODO function to convert Illumna yymmdd date as yyyy-MM-dd ?
                        // example: /ifs/pitt/161102_PITT_0089_AHFG3GBBXX/ or /ifs/lola/150814_LOLA_1298_BC7259ACXX/
                        String run = seqExperiment.getStringVal("SequencerRunFolder", user);
                        if (run == null || "".equals(run)) { // 04553_I_33 has empty SequencerRunFolder
                            log.warn("Skipping run: " + flowCellId);
                            continue;
                        }
                        String[] runFolderElements = run.split("_");
                        String runId = runFolderElements[1] + "_" + runFolderElements[2];
                        String runName = runId + "_" + runFolderElements[3];
                        runName = runName.replace("/", ""); // now PITT_0089_AHFG3GBBXX
                        String illuminaDate = runFolderElements[0].substring(runFolderElements[0].length() - 6); // yymmdd
                        String dateCreated = "20" + illuminaDate.substring(0, 2) + "-" + illuminaDate.substring(2, 4) + "-" + illuminaDate.substring(4, 6);

                        SampleManifest.Run r = new SampleManifest.Run(runMode, runId, flowCellId, readLength, dateCreated);
                        if (runsMap.containsKey(flowCellId)) { // already created, just add new lane num to list
                            runsMap.get(flowCellId).addLane(laneNum);
                        } else { // lookup fastq paths for this run, currently making extra queries for 06260_N_9 KIM & others
                            //06938_J_86 was demuxed by lane on 2017-06-16 16:49:08
                            List<String> fastqs = FastQPathFinder.search(runId, origSampleName, sampleManifest.getIgoId(), true, runPassedQC);
                            if (fastqs == null && aliquot.getLongVal("DateCreated", user) < 1455132132000L) { // try search again with pre-Jan 2016 naming convention, 06184_4
                                log.info("Searching fastq database again for pre-Jan. 2016 sample.");
                                fastqs = FastQPathFinder.search(runId, origSampleName, null, false, runPassedQC);
                            }

                            if (fastqs != null) {
                                r.addLane(laneNum);
                                r.setFastqs(fastqs);

                                runsMap.put(flowCellId, r);
                                library.addRun(r);
                            }
                        }
                    }
                }
            }

            runJax0004IsMissingFlowcellInfoInLIMS(origSampleName, sampleManifest, runPassedQC, library);

            // libraries that have no fastqs will not be reported such as 05257_BS_20
            if (library.hasFastqs()) {
                sampleManifest.getLibraries().add(library);
            }
        }
        return sampleManifest;
    }

    /**
     * Set Values unique to the ACCESS & CMO-CH recipes.
     * @param user
     * @param dataRecordManager
     * @param sample
     * @param sampleManifest
     * @return
     * @throws NotFound
     * @throws IoError
     * @throws RemoteException
     */
    private Double setACCESS2dBarcode(User user, DataRecordManager dataRecordManager, DataRecord sample, SampleManifest sampleManifest) throws NotFound, IoError, RemoteException {
        Double dnaInputNg = findDNAInputForLibraryForMSKACCESS(sample, user);
        log.info("Searching for ACCESS 2D barcode with base IGO sample ID=" + sampleManifest.getCmoInfoIgoId());
        List<DataRecord> baseSamples = dataRecordManager.queryDataRecords("Sample", "SampleId = '" + sampleManifest.getCmoInfoIgoId() + "'", user);
        if (baseSamples.size() > 0) {
            DataRecord baseSample = baseSamples.get(0);
            // example 2d barcode "8036707180"
            String barcode = baseSample.getStringVal("MicronicTubeBarcode", user);
            while (barcode == null || barcode.length() < 8) {
                // travel to child sample to look for the original barcode, for example 06302_AH_9
                log.info("No 2dbarcode in parent sample, checking child sample.");
                DataRecord [] sampleChildren = baseSample.getChildrenOfType("Sample", user);
                if (sampleChildren.length > 0) {
                    baseSample = sampleChildren[0];
                    barcode = baseSample.getStringVal("MicronicTubeBarcode", user);
                } else {
                    break;
                }
            }
            sampleManifest.setCfDNA2dBarcode(barcode);
        }
        return dnaInputNg;
    }

    private String getTubeId(DataRecord sample, User user) throws IoError, RemoteException, NotFound {
        log.info("Looking up tube ID of the original sample received.");
        List<DataRecord> parentSample = sample.getParentsOfType("Sample", user);
        while (parentSample.size() > 0) { // keep checking if there is a parent of type sample until there is not
            sample = parentSample.get(0);
            parentSample = sample.getParentsOfType("Sample", user);
        }
        String tubeId = sample.getStringVal("TubeBarcode", user);
        log.info("Located Tube ID: " + tubeId);
        return tubeId;
    }

    private SampleManifest getFastqsAndCheckTheirQCStatus(String igoId, User user, DataRecord sample, SampleManifest sampleManifest) throws RemoteException, NotFound {
        log.info("Returning baitset & fastqs only for IGO ID: " + igoId);
        // query Picard QC records for bait set & "Failed" fastqs.
        // (exclude failed, less stringent than include only passed)
        List<DataRecord> qcs = sample.getDescendantsOfType(SeqAnalysisSampleQCModel.DATA_TYPE_NAME, user);
        Set<String> runFailedQC = new HashSet<>();
        String baitSet = null;
        for (DataRecord dr : qcs) {
            String qcResult = dr.getStringVal(SeqAnalysisSampleQCModel.SEQ_QCSTATUS, user);
            if ("Failed".equals(qcResult)) {
                String run = dr.getStringVal(SeqAnalysisSampleQCModel.SEQUENCER_RUN_FOLDER, user);
                runFailedQC.add(run);
                log.info("Failed sample & run: " + run);
            }
            baitSet = dr.getStringVal(SeqAnalysisSampleQCModel.BAIT_SET, user);
        }
        sampleManifest.setBaitSet(baitSet);

        return fastqsOnlyManifest(sampleManifest, runFailedQC);
    }

    /**
     * 05428_O has samples sequenced on two runs, one of those runs - JAX_0004 has no lane information present in the LIMS
     * although it does have passed QC LIMS records for that run
     */
    private void runJax0004IsMissingFlowcellInfoInLIMS(String origSampleName, SampleManifest sampleManifest,
                                                       Set<String> runPassedQC, SampleManifest.Library library) {
        if (runPassedQC.contains("JAX_0004_BH5GJYBBXX")) {
            String runID = "JAX_0004";
            SampleManifest.Run r = new SampleManifest.Run("", runID, "H5GJYBBXX", "", "2015-11-30");
            r.setFastqs(FastQPathFinder.search(runID, origSampleName, sampleManifest.getIgoId(), false, runPassedQC));
            if (r.getFastqs() != null) {
                library.addRun(r);
            }
        }
    }

    protected void addIGOQcRecommendations(SampleManifest sampleManifest, DataRecord sample, User user) {
        try {
            log.info("Searching for QcReportDna report");
            List<DataRecord> qcRecords = sample.getDescendantsOfType("QcReportDna", user);
            if (qcRecords.size() > 0) {
                DataRecord qcRecord = qcRecords.get(0);
                String igoQcRecommendation = qcRecord.getStringVal("IgoQcRecommendation", user);
                String comments = qcRecord.getStringVal("Comments", user);
                String id = qcRecord.getStringVal("InvestigatorDecision", user);
                SampleManifest.QcReport r = new SampleManifest.QcReport(SampleManifest.QcReportType.DNA, igoQcRecommendation, comments, id);
                sampleManifest.addQcReport(r);
            }

            log.info("Searching for QcReportLibrary");
            List<DataRecord> qcRecordsLib = sample.getDescendantsOfType("QcReportLibrary", user);
            if (qcRecordsLib.size() > 0) {
                DataRecord qcRecord = qcRecordsLib.get(0);
                int i=1;  // if the record has Hide=True look for the correct one
                while (qcRecord.getBooleanVal("HideFromSampleQC", user) && i < qcRecordsLib.size()) {
                    qcRecord = qcRecordsLib.get(i++);
                }
                String igoQcRecommendation = qcRecord.getStringVal("IgoQcRecommendation", user);
                String comments = qcRecord.getStringVal("Comments", user);
                String id = qcRecord.getStringVal("InvestigatorDecision", user);
                SampleManifest.QcReport r = new SampleManifest.QcReport(SampleManifest.QcReportType.LIBRARY, igoQcRecommendation, comments, id);
                sampleManifest.addQcReport(r);
            }
        } catch (RemoteException | NotFound e) {
            log.error("Failed to complete QC record searches.");
            e.printStackTrace();
        }
    }

    /*
     * ACCESS & CMO-CHO have requested dna Input for each library.
     */
    protected Double findDNAInputForLibraryForMSKACCESS(DataRecord sample, User user) {
        try {
            log.info("Searching for DNA Library Input Mass (ng).");
            DataRecord[] records = sample.getChildrenOfType(KAPALibPlateSetupProtocol1Model.DATA_TYPE_NAME, user);
            DataRecord record = records[records.length - 1];
            return record.getDoubleVal(KAPALibPlateSetupProtocol1Model.TARGET_MASS_ALIQ_1, user);
        } catch (Exception e) {
            log.error(e);
            return null;
        }
    }

    protected SampleManifest setSampleCMOLevelFields(String igoId, DataRecord sample, List<DataRecord> samples,
                                                     DataRecordManager dataRecordManager, User user) throws NotFound, RemoteException {
        List<DataRecord> cmoRecords = Utils.getRecordsOfTypeFromParents(sample, "Sample", "SampleCMOInfoRecords", user);
        DataRecord cmoInfo;
        String cmoInfoIgoId = "";
        if (cmoRecords == null || cmoRecords.isEmpty()) {
            log.info("No CMO info record found, using sample level fields for IGO ID: " + igoId);
            cmoInfo = samples.get(0);
        } else {
            cmoInfo = cmoRecords.get(0);
            cmoInfoIgoId = cmoInfo.getStringVal("SampleId", user);
            log.info("Found CMO Sample record linked to IGO ID " + cmoInfoIgoId);
        }
        return setSampleLevelFields(igoId, cmoInfoIgoId, cmoInfo, user);
    }

    protected SampleManifest fastqsOnlyManifest(SampleManifest sampleManifest, Set<String> runFailedQC) {
        List<SampleManifest.Run> runs = FastQPathFinder.searchForFastqs(sampleManifest.getIgoId(), runFailedQC);

        SampleManifest.Library library = new SampleManifest.Library();
        library.setRuns(runs);
        Set<SampleManifest.Library> libraries = new HashSet<>();
        libraries.add(library);
        sampleManifest.setLibraries(libraries);

        return sampleManifest;
    }

    private SampleManifest.Library getLibraryFields(User user, String libraryIgoId, DataRecord aliquot, Double dnaInputNg) throws IoError, RemoteException, NotFound {
        DataRecord[] libPrepProtocols = aliquot.getChildrenOfType("DNALibraryPrepProtocol3", user);
        Double libraryVolume = null;
        if (libPrepProtocols != null && libPrepProtocols.length == 1)
            libraryVolume = libPrepProtocols[0].getDoubleVal("ElutionVol", user);
        Double libraryConcentration = null;
        Object libraryConcentrationObj = aliquot.getValue("Concentration", user);
        if (libraryConcentrationObj != null)  // for example 06449_1 concentration is null
            libraryConcentration = aliquot.getDoubleVal("Concentration", user);

        return new SampleManifest.Library(libraryIgoId, libraryVolume, libraryConcentration, dnaInputNg);
    }

    protected SampleManifest setSampleLevelFields(String igoId, String cmoInfoIgoId, DataRecord cmoInfo, User user) throws NotFound, RemoteException {
        SampleManifest s = new SampleManifest();
        s.setIgoId(igoId);
        s.setCmoInfoIgoId(cmoInfoIgoId);
        s.setCmoPatientId(cmoInfo.getStringVal("CmoPatientId", user));
        // aka "Sample Name" in SampleCMOInfoRecords

        String sampleName = cmoInfo.getStringVal("OtherSampleId", user);
        if (sampleName == null || "".equals(sampleName.trim())) { // for example 05304_O_4 Agilent 51MB or update DB so this is not necessary?
            //sampleName = cmoInfo.getStringVal("UserSampleID", user);
        }
        s.setSampleName(sampleName);
        s.setCmoSampleClass(cmoInfo.getStringVal("CMOSampleClass", user));
        s.setInvestigatorSampleId(cmoInfo.getStringVal("UserSampleID", user));
        String tumorOrNormal = cmoInfo.getStringVal("TumorOrNormal", user);
        s.setTumorOrNormal(tumorOrNormal);
        if ("Tumor".equals(tumorOrNormal))
            s.setOncoTreeCode(cmoInfo.getStringVal("TumorType", user));
        s.setTissueLocation(cmoInfo.getStringVal("TissueLocation", user));
        s.setSpecimenType(cmoInfo.getStringVal("SpecimenType", user));
        s.setSampleOrigin(cmoInfo.getStringVal("SampleOrigin", user)); // formerly reported as Sample Type
        s.setPreservation(cmoInfo.getStringVal("Preservation", user));
        s.setCollectionYear(cmoInfo.getStringVal("CollectionYear", user));
        s.setSex(cmoInfo.getStringVal("Gender", user));
        s.setSpecies(cmoInfo.getStringVal("Species", user));
        s.setCmoSampleName(cmoInfo.getStringVal("CorrectedCMOID", user));
        String normalizedPatientId = cmoInfo.getStringVal("NormalizedPatientId", user);
        s.getCmoSampleIdFields().setNormalizedPatientId(normalizedPatientId);
        return s;
    }

    public static class LibraryDataRecord {
        public DataRecord record;
        public DataRecord parent;

        public LibraryDataRecord(DataRecord r) {
            this.record = r;
        }
        public LibraryDataRecord(DataRecord r, DataRecord p) {
            this.record = r;
            this.parent = p;
        }
    }

    private Map<String, LibraryDataRecord> findDNALibraries(List<DataRecord> aliquots, String baseIGOId, User user) throws Exception {
        Map<String, DataRecord> dnaLibraries = new HashMap<>();
        for (DataRecord aliquot : aliquots) {
            String sampleType = aliquot.getStringVal("ExemplarSampleType", user);
            // VERY IMPORTANT, if no DNA LIBRARY NO RESULT generated
            if ("DNA Library".equals(sampleType)) {
                String libraryIgoId = aliquot.getStringVal("SampleId", user);
                if (libraryIgoId.startsWith("Pool"))
                    continue;

                String sampleStatus = aliquot.getStringVal("ExemplarSampleStatus", user);
                // 05684_M_2 has a returned to user library, ignore it.
                if (sampleStatus != null && (sampleStatus.contains("Failed") || sampleStatus.contains("Returned"))) {
                    log.info("Skipping failed or returned library: " + libraryIgoId);
                    continue;
                }

                String recipe = aliquot.getStringVal(SampleModel.RECIPE, user);
                if ("Fingerprinting".equals(recipe)) // for example 07951_AD_1_1
                    continue;

                if (sameRequest(baseIGOId, libraryIgoId)) {
                    log.info("Found DNA library: " + libraryIgoId);
                    dnaLibraries.put(libraryIgoId, aliquot);
                } else {
                    // 06345_B_26 has 06345_C_26, 06345_D_26 libraries for IMPACT & Custom Capture
                    log.info("Ignoring DNA library on different request: " + libraryIgoId);
                }
            }
        }

        Map<String, LibraryDataRecord> dnaLibrariesFinal = new HashMap<>();
        // For example: 09245_E_21_1_1_1, 09245_E_21_1_1_1_2 & Failed 09245_E_21_1_1_1_1
        for (String libraryName : dnaLibraries.keySet()) {
            if (!dnaLibraries.containsKey(libraryName + "_1") && !dnaLibraries.containsKey(libraryName + "_2")) {
                // link parent child DNA libraries if they exist
                LibraryDataRecord dr = new LibraryDataRecord(dnaLibraries.get(libraryName),
                        dnaLibraries.get(libraryName.substring(0, libraryName.length() - 2)));
                dnaLibrariesFinal.put(libraryName, dr);
            }
        }
        return dnaLibrariesFinal;
    }

    // compate 05500_A & 05500_B
    public static boolean sameRequest(String id1, String id2) {
        String [] parts1 = id1.split("_");
        String [] parts2 = id2.split("_");
        if (parts1[1] == null || parts2[1] == null)
            return false;
        return parts1[1].equals(parts2[1]);
    }

    /**
     * Returns null if no fastqs found.
     */
    public static class FastQPathFinder {

        public static List<SampleManifest.Run> searchForFastqs(String igoId, Set<String> runFailedQC) {
            String url = "http://delphi.mskcc.org:8080/ngs-stats/rundone/fastqsbyigoid/" + igoId;
            log.info("Finding fastqs for igoID: " + igoId);
            try {
                RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<List<ArchivedFastq>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<ArchivedFastq>>() {});
                List<ArchivedFastq> fastqList = response.getBody();
                if (fastqList == null) {
                    log.info("NO fastqs found for Igo ID: " + igoId);
                    return null;
                }

                log.info("Fastq files found: " + fastqList.size());
                List<ArchivedFastq> passedFastqs = new ArrayList<>();
                for (ArchivedFastq f : fastqList) {
                    if (!runFailedQC.contains(f.runBaseDirectory))
                        passedFastqs.add(f);
                    else
                        log.info("Ignoring failed fastq: " + f);
                }

                // for remaining fastqs separate list into each run and get flowcell information and run date
                HashMap<String, SampleManifest.Run> runMap = new HashMap<>();
                for (ArchivedFastq fastq : passedFastqs) {
                    if (runMap.containsKey(fastq.run)) {  // if the run already exists just add the fastq path
                        SampleManifest.Run run = runMap.get(fastq.run);
                        run.addFastq(fastq.fastq);
                    } else {
                        String runId = fastq.getRunId();
                        String flowCellId = fastq.getFlowCellId();
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        String runDate = dateFormat.format(fastq.fastqLastModified); // 2020-07-31
                        SampleManifest.Run run = new SampleManifest.Run(runId, flowCellId, runDate);
                        run.addFastq(fastq.fastq);

                        runMap.put(fastq.run, run);
                    }
                }

                return new ArrayList(runMap.values());
            } catch (Exception e) {
                log.error("FASTQ Search error:" + e.getMessage());
                return null;
            }
        }

        // TODO url to properties & make interface for FastQPathFinder
        public static List<String> search(String run,
                                          String sampleName, String igoId,
                                          boolean returnOnlyTwo,
                                          Set<String> runPassedQC) {
            String sample_IGO_igoid;
            if (igoId == null)
                sample_IGO_igoid = sampleName;
            else
                sample_IGO_igoid = sampleName + "_IGO_" + IGOTools.baseIgoSampleId(igoId);

            String url = "http://igodb.mskcc.org:8080/ngs-stats/rundone/search/most/recent/fastqpath/" + run + "/" + sample_IGO_igoid;
            log.info("Finding fastqs in fastq DB for: " + url);

            try {
                // some fingerprinting samples like 08390_D_73 excluded here by searching for fastqs and failing to
                // find any

                RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<List<ArchivedFastq>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<ArchivedFastq>>() {});
                List<ArchivedFastq> fastqList = response.getBody();
                if (fastqList == null) {
                    log.info("NO fastqs found for run: " + run);
                    return null;
                }
                log.info("Fastq files found: " + fastqList.size());

                // so compare only full run directory to exclude failed runs
                List<ArchivedFastq> passedQCList = new ArrayList<>();
                for (ArchivedFastq fastq : fastqList) {
                    // in the absence of QC data automatically assume it is 'passed'
                    // The LIMS has no QC data for samples prior to Oct. 2015
                    if (runPassedQC.size() == 0)
                        passedQCList.add(fastq);
                    else if (runPassedQC.contains(fastq.run))
                        passedQCList.add(fastq);
                }
                log.info("fastqs passed QC: " + passedQCList.size());
                passedQCList = filterMultipleDemuxes(passedQCList);
                log.info("fastqs passed QC: " + passedQCList.size());
                List<String> result = new ArrayList<>();
                for (ArchivedFastq fastq : passedQCList) {
                    result.add(fastq.fastq);
                }

                if (result.size() == 0) {
                    log.info("NO passed fastqs found for run: " + run);
                    return null;
                }
                return result;
            } catch (Exception e) {
                log.error("FASTQ Search error:" + e.getMessage());
                return null;
            }
        }
    }

    /**
     * If the list of fastqs contains multiple demuxes return only the most recent demuxed fastqs.
     * Fastqs may be across many lanes or lanes all merged to R1 & R2 fastqs only

      If fastqs were demuxed by lane we should return all fastqs, ie: L005 & L006 fastqs should be included from this run in in Oct. 2016
        '/ifs/archive/GCL/hiseq/FASTQ/JAX_0039_BHCKCHBBXX/Project_06208_C/Sample_P-1234567-T01-WES_IGO_06208_C_80/P-1234567-T01-WES_IGO_06208_C_80_S25_L005_R1_001.fastq.gz'
        '/ifs/archive/GCL/hiseq/FASTQ/JAX_0039_BHCKCHBBXX/Project_06208_C/Sample_P-1234567-T01-WES_IGO_06208_C_80/P-1234567-T01-WES_IGO_06208_C_80_S25_L005_R2_001.fastq.gz'
        '/ifs/archive/GCL/hiseq/FASTQ/JAX_0039_BHCKCHBBXX/Project_06208_C/Sample_P-1234567-T01-WES_IGO_06208_C_80/P-1234567-T01-WES_IGO_06208_C_80_S25_L006_R1_001.fastq.gz'
        '/ifs/archive/GCL/hiseq/FASTQ/JAX_0039_BHCKCHBBXX/Project_06208_C/Sample_P-1234567-T01-WES_IGO_06208_C_80/P-1234567-T01-WES_IGO_06208_C_80_S25_L006_R2_001.fastq.gz'

        BUT only return the most recent "_A2" run fastqs for this run:

        /ifs/archive/GCL/hiseq/FASTQ/A00227_0011_BH2YHKDMXX_A2/Project_93017_F/Sample_P-8765432-T01-WES_IGO_93017_F_74/P-8765432-T01-WES_IGO_93017_F_74_S11_R2_001.fastq.gz
        not this fastq
           /ifs/archive/GCL/hiseq/FASTQ/A00227_0011_BH2YHKDMXX/Project_93017_F/Sample_P-0020689-T01-WES_IGO_93017_F_74/P-0020689-T01-WES_IGO_93017_F_74_S88_R1_001.fastq.gz

         Illumina sample sheet S** number will likely change when redemuxed:
           https://support.illumina.com/help/BaseSpace_OLH_009008/Content/Source/Informatics/BS/NamingConvention_FASTQ-files-swBS.htm
         */
    protected static List<ArchivedFastq> filterMultipleDemuxes(List<ArchivedFastq> fastqs) {
        fastqs.sort(Comparator.comparing(ArchivedFastq::getFastqLastModified).reversed());

        List<ArchivedFastq> onlyMostRecentDemux = new ArrayList();
        onlyMostRecentDemux.add(fastqs.get(0));
        for (int i=1; i < fastqs.size(); i++) {
            if (fastqs.get(i).getRunBaseDirectory().equals(fastqs.get(i-1).getRunBaseDirectory()))
                onlyMostRecentDemux.add(fastqs.get(i));
            else
                break;
        }
        return onlyMostRecentDemux;
    }

    static class SampleSheet {
        public String id1;
        public String igoId;
        public String barcodeId;
        public String barcodeIndex1;
        public String barcodeIndex2;

        public SampleSheet(String id1, String igoId, String barcodeId, String barcodeIndex1, String barcodeIndex2) {
            this.id1 = id1;
            this.igoId = igoId;
            this.barcodeId = barcodeId;
            this.barcodeIndex1 = barcodeIndex1;
            this.barcodeIndex2 = barcodeIndex2;
        }
    }

    private static ArrayList<SampleSheet> samples06302_AO = new ArrayList<SampleSheet>() {
        {
            add(new SampleSheet("MSK-ML-0023-CF5-MSK13395A-P", "06302_AO_962", "IDTdual339", "TACCAGGA", "GAGAGTAC"));
            add(new SampleSheet("MSK-ML-0007-CF5-MSK5003894D-P", "06302_AO_966", "IDTdual340", "CGTCAATG", "CCAACACT"));
            add(new SampleSheet("MSK-ML-0092-CF3-MSK5004862D-P", "06302_AO_1007", "IDTdual348", "TCGAACCA", "AACCGTGT"));
            add(new SampleSheet("MSK-MB-0012-CF6-msk5001817d-p", "06302_AO_135", "IDTdual312", "CTGACACA", "AGAAGGAC"));
            add(new SampleSheet("MSK-MB-0080-CF8-msk5005638d-p", "06302_AO_182", "IDTdual326", "GCTCTGTA", "CTTACAGC"));
            add(new SampleSheet("MSK-ML-0086-CF3-MSK5003804D-P", "06302_AO_1028", "IDTdual331", "CCAAGTTG", "TACATCGG"));
            add(new SampleSheet("MSK-ML-0070-CF6-MSK5004852C-P", "06302_AO_967", "IDTdual341", "GAAGAGGT", "CAGGTTCA"));
            add(new SampleSheet("MSK-ML-0091-CF3-MSK5001430D-P", "06302_AO_968", "IDTdual342", "GACGAATG", "GTCCTTGA"));
            add(new SampleSheet("MSK-MB-0027-CF2-msk5000429c-p", "06302_AO_134", "IDTdual311", "ACTCGTTG", "GCGTATCA"));
            add(new SampleSheet("MSK-MB-0030-CF2-MSK14012a-p", "06302_AO_158", "IDTdual319", "GACTATGC", "ACTCAACG"));
            add(new SampleSheet("MSK-MB-0072-CF6-msk5005272c-p", "06302_AO_176", "IDTdual324", "GCCATAAC", "CACGCAAT"));
            add(new SampleSheet("MSK-ML-0036-CF5-MSK5001255D-P", "06302_AO_1036", "IDTdual334", "TGCCATTC", "TGATCACG"));
            add(new SampleSheet("MSK-ML-0076-CF8-MSK5004712D-P", "06302_AO_980", "IDTdual343", "AGGAGGAA", "GTAAGCAC"));
            add(new SampleSheet("MSK-ML-0006-CF10-MSK5003812C-P", "06302_AO_1014", "IDTdual351", "ATGGTTGC", "AGCCAACT"));
            add(new SampleSheet("MSK-MB-0045-CF1-MSK10461a-p", "06302_AO_128", "IDTdual310", "AGCTCCTA", "CAACACAG"));
            add(new SampleSheet("MSK-MB-0008-CF3-MSK11534a-p", "06302_AO_155", "IDTdual317", "AGTCTGTG", "CCTACCTA"));
            add(new SampleSheet("MSK-MB-0087-CF6-msk5004058d-p", "06302_AO_175", "IDTdual323", "CTCAGAGT", "AGCTTCAG"));
            add(new SampleSheet("MSK-ML-0036-CF5-MSK5001255C-P", "06302_AO_1046", "IDTdual337", "GACTTAGG", "GTTCTTCG"));
            add(new SampleSheet("MSK-ML-0008-CF8-MSK5005402D-P", "06302_AO_982", "IDTdual344", "CTTACAGC", "AACACCAC"));
            add(new SampleSheet("MSK-ML-0008-CF3-MSK5000441C-P", "06302_AO_1004", "IDTdual347", "CTATCGCA", "CGCGTATT"));
            add(new SampleSheet("MSK-ML-0080-CF4-MSK5004863C-P", "06302_AO_1010", "IDTdual349", "GAACGCTT", "AAGTGCAG"));
            add(new SampleSheet("MSK-ML-0033-CF5-5000075D-P", "06302_AO_1012", "IDTdual350", "CAGAATCG", "CCAGTTGA"));
            add(new SampleSheet("MSK-MB-0034-CF7-msk5002820c-p", "06302_AO_118", "IDTdual308", "CACAAGTC", "ATCGCAAC"));
            add(new SampleSheet("MSK-MB-0088-CF6-msk5005643c-p", "06302_AO_180", "IDTdual325", "TTACCGAG", "AGTCTTGG"));
            add(new SampleSheet("MSK-ML-0008-CF6-MSK5003805C-P", "06302_AO_1021", "IDTdual328", "GTCTGATC", "CTCAAGCT"));
            add(new SampleSheet("MSK-ML-0013-CF4-MSK5003861C-P", "06302_AO_1022", "IDTdual329", "TAGTTGCG", "TCTGTCGT"));
            add(new SampleSheet("MSK-ML-0033-CF6-MSK5001283D-P", "06302_AO_986", "IDTdual345", "GAGATGTC", "TAGTCAGC"));
            add(new SampleSheet("MSK-ML-0041-CF3-MSK13929A-P", "06302_AO_988", "IDTdual346", "TACGGTTG", "AGTTCGCA"));
            add(new SampleSheet("MSK-MB-0030-CF8-msk5005621d-p", "06302_AO_109", "IDTdual307", "CGGATTGA", "ACGTCGTT"));
            add(new SampleSheet("MSK-MB-0045-CF5-msk5001092d-p", "06302_AO_185", "IDTdual327", "CGTTATGC", "AACCACTC"));
            add(new SampleSheet("MSK-ML-0092-CF3-MSK5004862C-P", "06302_AO_1041", "IDTdual336", "AGTGCAGT", "ATTCCGCT"));
            add(new SampleSheet("positivecontrol_100819", "06302_AO_1056", "IDTdual338", "CGTACGAA", "AGATACGG"));
            add(new SampleSheet("MSK-MB-0030-CF3-MSK14055a-p", "06302_AO_123", "IDTdual309", "TACATCGG", "TCCACGTT"));
            add(new SampleSheet("MSK-MB-0018-CF2-MSK12430a-p", "06302_AO_149", "IDTdual316", "ACCTAAGG", "ATCTCCTG"));
            add(new SampleSheet("MSK-MB-0061-CF4-msk5001133c-p", "06302_AO_156", "IDTdual318", "AGGTTCGA", "AAGCGACT"));
            add(new SampleSheet("MSK-MB-0085-CF2-msk5001123c-p", "06302_AO_167", "IDTdual321", "TGTGCGTT", "TGAGACGA"));
            add(new SampleSheet("MSK-ML-0054-CF1-MSK12746A-P", "06302_AO_1035", "IDTdual333", "CTTGCTGT", "CAATGCGA"));
            add(new SampleSheet("MSK-ML-0061-CF3-MSK5001254D-P", "06302_AO_1040", "IDTdual335", "TTGATCCG", "AAGCTCAC"));
            add(new SampleSheet("MSK-MB-0085-CF3-msk5001143d-p", "06302_AO_140", "IDTdual313", "CAACCTAG", "AGGTCTGT"));
            add(new SampleSheet("MSK-MB-0051-CF6-msk5001144d-p", "06302_AO_141", "IDTdual314", "AAGGACAC", "CCACAACA"));
            add(new SampleSheet("MSK-MB-0099-CF1-msk5000961d-p", "06302_AO_142", "IDTdual315", "TGCAGGTA", "TCACGATG"));
            add(new SampleSheet("MSK-MB-0085-CF2-msk5001123d-p", "06302_AO_166", "IDTdual320", "TTCAGGAG", "CACAGGAA"));
            add(new SampleSheet("MSK-MB-0104-CF8-msk5005634c-p", "06302_AO_170", "IDTdual322", "CGAGACTA", "CCTCGTTA"));
            add(new SampleSheet("MSK-ML-0062-CF1-MSK5003813D-P", "06302_AO_1023", "IDTdual330", "TGATCGGA", "ACTGCGAA"));
        }
    };
    static HashMap<String, SampleSheet> map06302_AO = new HashMap<>();
    static {
        for (SampleSheet x : samples06302_AO) {
            map06302_AO.put(x.igoId,x);
        }
    }
}