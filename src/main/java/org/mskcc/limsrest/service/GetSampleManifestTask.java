package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.SampleModel;
import com.velox.sloan.cmo.recmodels.SeqAnalysisSampleQCModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.rmi.RemoteException;
import java.util.*;

/**
 *
 */
public class GetSampleManifestTask {
    private static Log log = LogFactory.getLog(GetSampleManifestTask.class);

    private FastQPathFinder fastQPathFinder;

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
        if (recipe.contains("PACT") || recipe.contains("ACCESS") || recipe.contains("Exome") || recipe.contains("51MB")) {
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
                log.info("Creating sample manifest for IGO ID:" + igoId);
                List<DataRecord> sampleCMOInfoRecords = dataRecordManager.queryDataRecords("SampleCMOInfoRecords", "SampleId = '" + igoId +  "'", user);

                log.info("Searching Sample table for SampleId ='" + igoId + "'");
                List<DataRecord> samples = dataRecordManager.queryDataRecords("Sample", "SampleId = '" + igoId + "'", user);
                if (samples.size() == 0) { // return error
                    return new SampleManifestResult(null, "Sample not found: " + igoId);
                }
                DataRecord sample = samples.get(0);
                String recipe = sample.getStringVal(SampleModel.RECIPE, user);
                if ("Fingerprinting".equals(recipe)) // for example 07951_S_50_1, skip for pipelines for now
                    continue;

                if (!isPipelineRecipe(recipe)) {
                    continue;
                }

                List<DataRecord> qcs = sample.getDescendantsOfType(SeqAnalysisSampleQCModel.DATA_TYPE_NAME, user);
                Set<String> failedRuns = new HashSet<>();
                for (DataRecord dr : qcs) {
                    String qcResult = dr.getStringVal(SeqAnalysisSampleQCModel.SEQ_QCSTATUS, user);
                    if ("Failed".equals(qcResult)) {
                        String run = dr.getStringVal(SeqAnalysisSampleQCModel.SEQUENCER_RUN_FOLDER, user);
                        failedRuns.add(run);
                        log.info("Failed sample & run: " + run);
                    }
                }

                // 06302_R_1 has no sampleCMOInfoRecord so use the same fields at the sample level
                DataRecord cmoInfo;  // assign the dataRecord to query either sample table or samplecmoinforecords
                if (sampleCMOInfoRecords.size() == 0) {
                    cmoInfo = samples.get(0);
                } else {
                    cmoInfo = sampleCMOInfoRecords.get(0);
                }

                SampleManifest sampleManifest = getSampleLevelFields(igoId, cmoInfo, user);

                // library concentration & volume
                // often null in samplecmoinforecords then query KAPALibPlateSetupProtocol1.TargetMassAliq1
                //String dmpLibraryInput = cmoInfo.getStringVal("DMPLibraryInput", user); // often null
                //String dmpLibraryOutput = cmoInfo.getStringVal("DMPLibraryOutput", user); // LIBRARY_YIELD

                List<DataRecord> aliquots = sample.getDescendantsOfType("Sample", user);
                // 07260 Request the first sample are DNA Libraries like 07260_1 so can't just search descendants to find DNA libraries
                aliquots.add(sample);
                Map<String, DataRecord> dnaLibraries = findDNALibraries(aliquots, user);

                // for each DNA Library traverse the records grab the fields we need and paths to fastqs.
                for (Map.Entry<String, DataRecord> aliquotEntry : dnaLibraries.entrySet()) {
                    String libraryIgoId = aliquotEntry.getKey();
                    DataRecord aliquot = aliquotEntry.getValue();
                    DataRecord aliquotParent = null;
                    log.info("Processing DNA library: " + libraryIgoId);

                    if (dnaLibraries.containsKey(libraryIgoId + "_1")) { // does this library have a child library?
                        log.info("Skipping:" + libraryIgoId);  // For example: 09641_70_1_1_1 & 09641_70_1_1_1_1
                        continue;
                    }
                    String possibleParentIgoId = libraryIgoId.substring(0,libraryIgoId.length()-2);
                    if (dnaLibraries.containsKey(possibleParentIgoId)) {
                        aliquotParent = dnaLibraries.get(possibleParentIgoId);
                        // TODO review capture concentration, libraryVolume, etc in this case
                    }

                    DataRecord[] libPrepProtocols = aliquot.getChildrenOfType("DNALibraryPrepProtocol3", user);
                    Double libraryVolume = null;
                    if (libPrepProtocols.length == 1)
                        libraryVolume = libPrepProtocols[0].getDoubleVal("ElutionVol", user);
                    Double libraryConcentration = aliquot.getDoubleVal("Concentration", user);

                    SampleManifest.Library library =
                            new SampleManifest.Library(libraryIgoId, libraryVolume, libraryConcentration);

                    List<DataRecord> indexBarcodes = aliquot.getDescendantsOfType("IndexBarcode", user);
                    if (aliquotParent != null) {
                        // parent DNA library may have the barcode records
                        indexBarcodes = aliquotParent.getDescendantsOfType("IndexBarcode", user);
                    }
                    if (indexBarcodes != null && indexBarcodes.size() > 0) {
                        DataRecord bc = indexBarcodes.get(0);
                        library.barcodeId = bc.getStringVal("IndexId", user);
                        library.barcodeIndex = bc.getStringVal("IndexTag", user);
                    }

                    // recipe, capture input, capture name
                    List<DataRecord> nimbleGen = aliquot.getDescendantsOfType("NimbleGenHybProtocol", user);
                    log.info("Found nimbleGen records: " + nimbleGen.size());
                    for (DataRecord n : nimbleGen) {
                        Object valid = n.getValue("Valid", user); // 05359_B_1 null
                        if (valid != null && new Boolean(valid.toString())) {
                            String poolName = n.getStringVal("Protocol2Sample", user);
                            String baitSet = n.getStringVal("Recipe", user); // LIMS display name "bait set"
                            sampleManifest.setBaitSet(baitSet);
                            Object val = n.getValue("SourceMassToUse", user);
                            if (val != null) {
                                Double captureInput = n.getDoubleVal("SourceMassToUse", user);
                                library.captureInputNg = captureInput.toString();
                                library.captureName = poolName;
                                Double captureVolume = n.getDoubleVal("VolumeToUse", user);
                                library.captureConcentrationNm = captureVolume.toString();
                            }
                        } else {
                            log.warn("Nimblegen records not valid.");
                        }
                    }

                    // TODO 08390_D_73

                    // for each flow cell ID a sample may be on multiple lanes
                    // (currently all lanes are demuxed to same fastq file)
                    Map<String, SampleManifest.Run> runsMap = new HashMap<>();
                    // run Mode, runId, flow Cell & Lane Number
                    // Flow Cell Lanes are far down the sample/pool hierarchy in LIMS
                    List<DataRecord> reqLanes = aliquot.getDescendantsOfType("FlowCellLane", user);
                    log.info("Found lanes: " + reqLanes);
                    for (DataRecord flowCellLane : reqLanes) {
                        Integer laneNum = ((Long) flowCellLane.getLongVal("LaneNum", user)).intValue();
                        log.info("Getting a flow cell lane");
                        List<DataRecord> flowcell = flowCellLane.getParentsOfType("FlowCell", user);
                        if (flowcell.size() > 0) {
                            log.info("Getting a flow cell");
                            List<DataRecord> possibleRun = flowcell.get(0).getParentsOfType("IlluminaSeqExperiment", user);
                            if (possibleRun.size() > 0) {
                                DataRecord seqExperiment = possibleRun.get(0);
                                String runMode = seqExperiment.getStringVal("SequencingRunMode", user);
                                String flowCellId = seqExperiment.getStringVal("FlowcellId", user);
                                String readLength = seqExperiment.getStringVal("ReadLength", user); // TODO blank in LIMS prior to April 2019

                                // TODO function to convert Illumna yymmdd date as yyyy-MM-dd ?
                                // example: /ifs/pitt/161102_PITT_0089_AHFG3GBBXX/ or /ifs/lola/150814_LOLA_1298_BC7259ACXX/
                                String run = seqExperiment.getStringVal("SequencerRunFolder", user);
                                String[] runFolderElements = run.split("_");
                                String runId = runFolderElements[1] + "_" + runFolderElements[2];
                                String runName = runId + "_" + runFolderElements[3];
                                runName = runName.replace("/", ""); // now PITT_0089_AHFG3GBBXX
                                String illuminaDate = runFolderElements[0].substring(runFolderElements[0].length() - 6); // yymmdd
                                String dateCreated = "20" + illuminaDate.substring(0, 2) + "-" + illuminaDate.substring(2, 4) + "-" + illuminaDate.substring(4, 6);

                                SampleManifest.Run r = new SampleManifest.Run(runMode, runId, flowCellId, readLength, dateCreated);
                                if (runsMap.containsKey(flowCellId)) { // already created, just add new lane num to list
                                    runsMap.get(flowCellId).addLane(laneNum);
                                } else { // lookup fastq paths for this run
                                    List<String> fastqs = null;
                                    // if QC was not failed, query Fastq database for path to the fastqs for that run
                                    if (!failedRuns.contains(runName)) { //
                                        fastqs = FastQPathFinder.search(runId, sampleManifest.getInvestigatorSampleId() + "_IGO_" + sampleManifest.getIgoId(), true);
                                        if (fastqs == null) { // try search again with pre-Jan 2016 naming convention, 06184_4
                                            log.info("Searching fastq database again"); // TODO maybe only search again for old samples?
                                            fastqs = FastQPathFinder.search(runId, sampleManifest.getInvestigatorSampleId(), false);
                                        }

                                        r.addLane(laneNum);
                                        r.fastqs = fastqs;

                                        runsMap.put(flowCellId, r);
                                        library.runs.add(r);
                                    }
                                }
                            }
                        }
                    }

                    if (reqLanes.size() > 0) { // only report this library if it made it to a sequencer/run
                        List<SampleManifest.Library> libraries = sampleManifest.getLibraries();
                        libraries.add(library);
                    }
                }

                smList.add(sampleManifest);
            }
            log.info("Manifest generation time(ms):" + (System.currentTimeMillis()-startTime));
            return new SampleManifestResult(smList, null);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    protected SampleManifest getSampleLevelFields(String igoId, DataRecord cmoInfo, User user) throws NotFound, RemoteException {
        SampleManifest s = new SampleManifest();
        s.setIgoId(igoId);
        s.setCmoPatientId(cmoInfo.getStringVal("CmoPatientId", user));
        // aka "Sample Name" in SampleCMOInfoRecords
        s.setInvestigatorSampleId(cmoInfo.getStringVal("OtherSampleId", user));
        String tumorOrNormal = cmoInfo.getStringVal("TumorOrNormal", user);
        s.setTumorOrNormal(tumorOrNormal);
        if ("Tumor".equals(tumorOrNormal))
            s.setOncoTreeCode(cmoInfo.getStringVal("TumorType", user));
        s.setTissueLocation(cmoInfo.getStringVal("TissueLocation", user));
        s.setSampleOrigin(cmoInfo.getStringVal("SampleOrigin", user)); // formerly reported as Sample Type
        s.setPreservation(cmoInfo.getStringVal("Preservation", user));
        s.setCollectionYear(cmoInfo.getStringVal("CollectionYear", user));
        s.setSex(cmoInfo.getStringVal("Gender", user));
        s.setSpecies(cmoInfo.getStringVal("Species", user));
        s.setCmoSampleName(cmoInfo.getStringVal("CorrectedCMOID", user));

        return s;
    }

    private Map<String, DataRecord> findDNALibraries(List<DataRecord> aliquots, User user) throws Exception {
        Map<String, DataRecord> dnaLibraries = new HashMap<>();
        for (DataRecord aliquot : aliquots) {
            String sampleType = aliquot.getStringVal("ExemplarSampleType", user);
            // VERY IMPORTANT, if no DNA LIBRARY NO RESULT generated
            if ("DNA Library".equals(sampleType)) {
                String sampleStatus = aliquot.getStringVal("ExemplarSampleStatus", user);
                if (sampleStatus != null && sampleStatus.contains("Failed"))
                    continue;

                String recipe = aliquot.getStringVal(SampleModel.RECIPE, user);
                if ("Fingerprinting".equals(recipe)) // for example 07951_AD_1_1
                    continue;

                String libraryIgoId = aliquot.getStringVal("SampleId", user);
                dnaLibraries.put(libraryIgoId, aliquot);
            }
        }
        return dnaLibraries;
    }

    public static class FastQPathFinder {
        // TODO url to properties & make interface for FastQPathFinder
        public static List<String> search(String run, String sample_IGO_igoid, boolean returnOnlyTwo) {
            String url = "http://delphi.mskcc.org:8080/ngs-stats/rundone/search/most/recent/fastqpath/" + run + "/" + sample_IGO_igoid;
            log.info("Finding fastqs in fastq DB for: " + url);

            try {
                RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<List<ArchivedFastq>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<ArchivedFastq>>() {});
                List<ArchivedFastq> fastqList = response.getBody();
                log.info("Fastq files found: " + fastqList.size());

                if (returnOnlyTwo) {
                    // Return only most recent R1&R2 in case of re-demux
                    if (fastqList.size() > 2)
                        fastqList = fastqList.subList(0,2);
                }

                List<String> result = new ArrayList<>();
                for (ArchivedFastq fastq : fastqList) {
                    result.add(fastq.fastq);
                }

                return result;
            } catch (Exception e) {
                log.error("ERROR:" + e.getMessage());
                return null;
            }
        }
    }
}