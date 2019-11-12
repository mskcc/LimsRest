package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.SampleModel;
import com.velox.sloan.cmo.recmodels.SeqAnalysisSampleQCModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.util.IGOTools;
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
                || recipe.contains("ShallowWGS")) {
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
            return new SampleManifest();
        }
        DataRecord sample = samples.get(0);
        // fastq is named by sample level field not cmo record in case of a sample swap such as 07951_I_12
        String origSampleName = sample.getStringVal("OtherSampleId", user);

        String recipe = sample.getStringVal(SampleModel.RECIPE, user);
        // for example 07951_S_50_1 is Fingerprinting sample, skip for pipelines for now
        if ("Fingerprinting".equals(recipe))
            return new SampleManifest();

        SampleManifest sampleManifest = getSampleLevelFields(igoId, samples, sample, dataRecordManager, user);

        if (!isPipelineRecipe(recipe)) {
            log.info("Returning fastqs only for IGO ID: " + igoId);
            // query Picard QC records for bait set & "Failed" fastqs.
            // (exclude failed less stringent than include only passed)
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

        // query Picard QC records for bait set & "Passed" fastqs.
        List<DataRecord> qcs = sample.getDescendantsOfType(SeqAnalysisSampleQCModel.DATA_TYPE_NAME, user);
        Set<String> runPassedQC = new HashSet<>();
        String baitSet = null;
        for (DataRecord dr : qcs) {
            String qcResult = dr.getStringVal(SeqAnalysisSampleQCModel.SEQ_QCSTATUS, user);
            if ("Passed".equals(qcResult)) {
                String run = dr.getStringVal(SeqAnalysisSampleQCModel.SEQUENCER_RUN_FOLDER, user);
                runPassedQC.add(run);
                log.info("Passed sample & run: " + run);
                baitSet = dr.getStringVal(SeqAnalysisSampleQCModel.BAIT_SET, user);
            }
        }

        if (baitSet == null || baitSet.isEmpty())
            System.err.println("Missing bait set: " + igoId);
        sampleManifest.setBaitSet(baitSet);

        List<DataRecord> aliquots = sample.getDescendantsOfType("Sample", user);
        // 07260 Request the first sample are DNA Libraries like 07260_1 so can't just search descendants to find DNA libraries
        aliquots.add(sample);
        Map<String, LibraryDataRecord> dnaLibraries = findDNALibraries(aliquots, sampleManifest.getIgoId(), user);

        log.info("DNA Libraries found: " + dnaLibraries.size());
        if (dnaLibraries.size() == 0) {
            // 05500_FQ_1 was submitted as a pooled library, try to find fastqs
            log.info("No DNA libraries found, searching from base IGO ID.");
            dnaLibraries.put(igoId, new LibraryDataRecord(sample));
        }

        // for each DNA Library traverse the records grab the fields we need and paths to fastqs.
        for (Map.Entry<String, LibraryDataRecord> aliquotEntry : dnaLibraries.entrySet()) {
            String libraryIgoId = aliquotEntry.getKey();
            DataRecord aliquot = aliquotEntry.getValue().record;
            DataRecord aliquotParent = aliquotEntry.getValue().parent;
            log.info("Processing DNA library: " + libraryIgoId);
            SampleManifest.Library library = getLibraryFields(user, libraryIgoId, aliquot);

            List<DataRecord> indexBarcodes = aliquot.getDescendantsOfType("IndexBarcode", user);
            if (aliquotParent != null) { // TODO get barcodes for WES samples
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
                    String baitSetRecipe = n.getStringVal("Recipe", user); // LIMS display name "bait set"
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
                            log.warn("Skipping run: " + seqExperiment.getStringVal("folowCellId", user));
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
                            List<String> fastqs = FastQPathFinder.search(runId, origSampleName, sampleManifest.getIgoId(), true, runPassedQC);
                            if (fastqs == null && aliquot.getLongVal("DateCreated", user) < 1455132132000L) { // try search again with pre-Jan 2016 naming convention, 06184_4
                                log.info("Searching fastq database again for pre-Jan. 2016 sample.");
                                fastqs = FastQPathFinder.search(runId, origSampleName, null, false, runPassedQC);
                            }

                            if (fastqs != null) {
                                r.addLane(laneNum);
                                r.fastqs = fastqs;

                                runsMap.put(flowCellId, r);
                                library.runs.add(r);
                            }
                        }
                    }
                }
            }

            // only report this library if it made it to a sequencer/run and has passed fastqs
            // for example 05257_BS_20 has a library which was sequenced then failed so skip
            if (library.hasFastqs()) {
                List<SampleManifest.Library> libraries = sampleManifest.getLibraries();
                libraries.add(library);
            }
        }
        return sampleManifest;
    }

    protected SampleManifest getSampleLevelFields(String igoId, List<DataRecord> samples, DataRecord sample,
                                                  DataRecordManager dataRecordManager, User user)
            throws NotFound, RemoteException, IoError {
        // try to find the CMO Sample Level record if it exists, if not use the sample level fields.

        String cmoInfoIgoId = getCMOSampleIGOID(sample, igoId, dataRecordManager, user);
        // 06302_R_1 has no sampleCMOInfoRecord so use the same fields at the sample level
        log.info("Searching for CMO info record by IGO ID:" + cmoInfoIgoId);
        List<DataRecord> sampleCMOInfoRecords = dataRecordManager.queryDataRecords("SampleCMOInfoRecords", "SampleId = '" + cmoInfoIgoId +  "'", user);
        DataRecord cmoInfo;  // assign the dataRecord to query either sample table or samplecmoinforecords
        if (sampleCMOInfoRecords.size() == 0) {
            log.info("No CMO info record found, using sample level fields for IGO ID: " + igoId);
            cmoInfo = samples.get(0);
        } else {
            cmoInfo = sampleCMOInfoRecords.get(0);
        }
        return getSampleLevelFields(igoId, cmoInfo, user);
    }

    // source IGO ID field often has '0', blank or null
    private String getCMOSampleIGOID(DataRecord sample, String igoId, DataRecordManager dataRecordManager, User user)
            throws NotFound, RemoteException, IoError {
        // for example sample: 10049_B_1->10049_1_1 source LIMS sample ID
        // Also possible parent's parent has the CMO Info record: 07724_D_8->07724_B_8->07724_8
        // or no CMO level record exists: 06230_C_5
        String sourceSampleID = sample.getStringVal("SourceLimsId", user);
        if (sourceSampleID == null || sourceSampleID.isEmpty() || sourceSampleID.equals("0"))
            return igoId;
        else {
            String baseIGOID = IGOTools.baseIgoSampleId(sourceSampleID);
            List<DataRecord> samples = dataRecordManager.queryDataRecords("Sample", "SampleId = '" + baseIGOID + "'", user);
            return getCMOSampleIGOID(samples.get(0), baseIGOID, dataRecordManager, user);
        }
    }

    protected SampleManifest fastqsOnlyManifest(SampleManifest sampleManifest, Set<String> runFailedQC) {
        List<String> fastqs = FastQPathFinder.search(sampleManifest.getIgoId(), runFailedQC);

        SampleManifest.Run run = new SampleManifest.Run(fastqs);
        SampleManifest.Library library = new SampleManifest.Library();
        library.runs = new ArrayList<SampleManifest.Run>();
        library.runs.add(run);
        List<SampleManifest.Library> libraries = new ArrayList<>();
        libraries.add(library);

        sampleManifest.setLibraries(libraries);
        return sampleManifest;
    }

    private SampleManifest.Library getLibraryFields(User user, String libraryIgoId, DataRecord aliquot) throws IoError, RemoteException, NotFound {
        DataRecord[] libPrepProtocols = aliquot.getChildrenOfType("DNALibraryPrepProtocol3", user);
        Double libraryVolume = null;
        if (libPrepProtocols != null && libPrepProtocols.length == 1)
            libraryVolume = libPrepProtocols[0].getDoubleVal("ElutionVol", user);
        Double libraryConcentration = null;
        Object libraryConcentrationObj = aliquot.getValue("Concentration", user);
        if (libraryConcentrationObj != null)  // for example 06449_1 concentration is null
            libraryConcentration = aliquot.getDoubleVal("Concentration", user);

        return new SampleManifest.Library(libraryIgoId, libraryVolume, libraryConcentration);
    }

    protected SampleManifest getSampleLevelFields(String igoId, DataRecord cmoInfo, User user) throws NotFound, RemoteException {
        SampleManifest s = new SampleManifest();
        s.setIgoId(igoId);
        s.setCmoPatientId(cmoInfo.getStringVal("CmoPatientId", user));
        // aka "Sample Name" in SampleCMOInfoRecords
        String sampleName = cmoInfo.getStringVal("OtherSampleId", user);
        if (sampleName == null || "".equals(sampleName.trim())) // for example 05304_0_4 Agilent 51MB or update DB so this is not necessary?
            sampleName = cmoInfo.getStringVal("UserSampleID", user);
        s.setInvestigatorSampleId(sampleName);
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
            if (!dnaLibraries.containsKey(libraryName + "_1") &&
                    !dnaLibraries.containsKey(libraryName + "_2")) {
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

        public static List<String> search(String igoId, Set<String> runFailedQC) {
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
                List<String> result = new ArrayList<>();
                for (ArchivedFastq f : fastqList) {
                    if (!runFailedQC.contains(f.runBaseDirectory))
                        result.add(f.fastq);
                    else
                        log.info("Ignoring failed fastq: " + f);
                }
                return result;
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

            String url = "http://delphi.mskcc.org:8080/ngs-stats/rundone/search/most/recent/fastqpath/" + run + "/" + sample_IGO_igoid;
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
                    if (runPassedQC.contains(fastq.runBaseDirectory))
                        passedQCList.add(fastq);
                    else {
                        // for example 08106_C_35 has fastq PITT_0214_AHVHVFBBXX_A1 BUT PASSED PITT_0214_AHVHVFBBXX
                        if (fastq.runBaseDirectory.endsWith("_A1") ||
                                fastq.runBaseDirectory.endsWith("_A2") ||
                                fastq.runBaseDirectory.endsWith("_A3")) {
                            if (runPassedQC.contains(fastq.run))
                                passedQCList.add(fastq);
                        }
                    }
                }

                if (returnOnlyTwo) {
                    // Return only most recent R1&R2 in case of re-demux
                    if (passedQCList.size() > 2)
                        passedQCList = passedQCList.subList(0,2);
                }

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
}