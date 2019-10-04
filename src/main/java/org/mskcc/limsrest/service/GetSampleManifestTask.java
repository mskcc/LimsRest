package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.SampleModel;
import com.velox.sloan.cmo.recmodels.SeqAnalysisSampleQCModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.controller.GetSampleManifest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class GetSampleManifestTask extends LimsTask {
    private static Log log = LogFactory.getLog(GetSampleManifestTask.class);

    private FastQPathFinder fastQPathFinder;

    protected String [] igoIds;

    public void init(String [] igoIds) {
        this.igoIds = igoIds;
    }

    @Override
    public Object execute(VeloxConnection conn) {
        long startTime = System.currentTimeMillis();
        try {
            List<SampleManifest> smList = new ArrayList<>();

            for (String igoId : igoIds) {
                log.info("Creating sample manifest for IGO ID:" + igoId);
                List<DataRecord> sampleCMOInfoRecords = dataRecordManager.queryDataRecords("SampleCMOInfoRecords", "SampleId = '" + igoId +  "'", user);

                log.info("Searching Sample table for SampleId ='" + igoId + "'");
                List<DataRecord> samples = dataRecordManager.queryDataRecords("Sample", "SampleId = '" + igoId + "'", user);
                if (samples.size() == 0) { // return error
                    return new GetSampleManifest.SampleManifestResult(null, "Sample not found: " + igoId);
                }
                DataRecord sample = samples.get(0);

                List<DataRecord> qcs = sample.getDescendantsOfType(SeqAnalysisSampleQCModel.DATA_TYPE_NAME, user);
                Set<String> failedRuns = new HashSet<>();
                for (DataRecord dr : qcs) {
                    String qcResult = dr.getStringVal(SeqAnalysisSampleQCModel.SEQ_QCSTATUS, user);
                    if ("Failed".equals(qcResult)) {
                        String run = dr.getStringVal(SeqAnalysisSampleQCModel.SEQUENCER_RUN_FOLDER, user);
                        failedRuns.add(run);
                        System.out.println("Failed sample & run: " + run);
                    }
                }

                // 06302_R_1 has no sampleCMOInfoRecord so use the same fields at the sample level
                DataRecord cmoInfo;  // assign the dataRecord to query either sample table or samplecmoinforecords
                if (sampleCMOInfoRecords.size() == 0) {
                    cmoInfo = samples.get(0);
                } else {
                    cmoInfo = sampleCMOInfoRecords.get(0);
                }

                SampleManifest s = new SampleManifest();
                s.setIgoId(igoId);
                s.setCmoPatientId(cmoInfo.getStringVal("CmoPatientId", user));
                s.setInvestigatorSampleId(cmoInfo.getStringVal("UserSampleID", user));
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

                s.setCmoSampleId(cmoInfo.getStringVal("CorrectedCMOID", user));

                // library concentration & volume
                // often null in samplecmoinforecords then query KAPALibPlateSetupProtocol1.TargetMassAliq1
                //String dmpLibraryInput = cmoInfo.getStringVal("DMPLibraryInput", user); // often null
                //String dmpLibraryOutput = cmoInfo.getStringVal("DMPLibraryOutput", user); // LIBRARY_YIELD
                List<DataRecord> aliquots = sample.getDescendantsOfType("Sample", user);
                for (DataRecord aliquot : aliquots) {
                    String sampleType = aliquot.getStringVal("ExemplarSampleType", user);
                    if ("DNA Library".equals(sampleType)) {
                        String recipe = sample.getStringVal(SampleModel.RECIPE, user);
                        if ("Fingerprinting".equals(recipe)) // for example 07951_S_50_1, skip for pipelines for now
                            continue;
                        String sampleStatus = aliquot.getStringVal("ExemplarSampleStatus", user);
                        if (sampleStatus != null && sampleStatus.contains("Failed"))
                            continue;

                        DataRecord [] libPrepProtocols = aliquot.getChildrenOfType("DNALibraryPrepProtocol3", user);
                        Double volume = null;
                        if (libPrepProtocols.length == 1)
                            volume = libPrepProtocols[0].getDoubleVal("ElutionVol", user);
                        Double concentration = aliquot.getDoubleVal("Concentration", user);
                        String libraryIgoId = aliquot.getStringVal("SampleId", user);
                        SampleManifest.Library library = new SampleManifest.Library(libraryIgoId, volume, concentration);

                        List<DataRecord> indexBarcodes = aliquot.getDescendantsOfType("IndexBarcode", user);
                        if (indexBarcodes != null && indexBarcodes.size() > 0) {
                            DataRecord bc = indexBarcodes.get(0);
                            library.barcodeId = bc.getStringVal("IndexId", user);
                            library.barcodeIndex = bc.getStringVal("IndexTag", user);
                        }

                        // recipe, capture input, capture name
                        List<DataRecord> nimbleGen = aliquot.getDescendantsOfType("NimbleGenHybProtocol", user);
                        log.info("Found nimbleGen records: " + nimbleGen.size());
                        for (DataRecord n : nimbleGen) {
                            if (n.getBooleanVal("Valid", user)) {
                                String poolName = n.getStringVal("Protocol2Sample", user);
                                String baitSet = n.getStringVal("Recipe", user); // LIMS display name "bait set"
                                s.setBaitSet(baitSet);
                                Object val = n.getValue("SourceMassToUse", user);
                                if (val != null) {
                                    Double captureInput = n.getDoubleVal("SourceMassToUse", user);
                                    library.captureInputNg = captureInput.toString();
                                    library.captureName = poolName;
                                    Double captureVolume = n.getDoubleVal("VolumeToUse", user);
                                    library.captureConcentrationNm = captureVolume.toString();
                                }
                            }
                        }

                        // run Mode, runId, flow Cell & Lane Number
                        List<DataRecord> reqLanes = aliquot.getDescendantsOfType("FlowCellLane", user);
                        for (DataRecord flowCellLane : reqLanes) {
                            Long laneNum = flowCellLane.getLongVal("LaneNum", user);
                            //log.info("Getting a flow cell lane");
                            List<DataRecord> flowcell = flowCellLane.getParentsOfType("FlowCell", user);
                            if (flowcell.size() > 0) {
                                //log.info("Getting a flow cell");
                                List<DataRecord> possibleRun = flowcell.get(0).getParentsOfType("IlluminaSeqExperiment", user);
                                if (possibleRun.size() > 0) {
                                    //log.info("Getting a run");
                                    DataRecord seqExperiment = possibleRun.get(0);
                                    String runMode  = seqExperiment.getStringVal("SequencingRunMode", user);
                                    String flowCellId = seqExperiment.getStringVal("FlowcellId", user);
                                    String readLength = seqExperiment.getStringVal("ReadLength", user); // TODO blank in LIMS prior to April 2019

                                    // TODO function to convert Illumna yymmdd date as yyyy-MM-dd ?
                                    // example: /ifs/pitt/161102_PITT_0089_AHFG3GBBXX/ or /ifs/lola/150814_LOLA_1298_BC7259ACXX/
                                    String run = seqExperiment.getStringVal("SequencerRunFolder", user);
                                    String[] runFolderElements = run.split("_");
                                    String runId = runFolderElements[1] + "_" + runFolderElements[2];
                                    String runName = runId + "_" + runFolderElements[3];
                                    runName = runName.replace("/", ""); // now PITT_0089_AHFG3GBBXX
                                    String illuminaDate = runFolderElements[0].substring(runFolderElements[0].length()-6); // yymmdd
                                    String dateCreated = "20" + illuminaDate.substring(0,2) + "-" + illuminaDate.substring(2,4) + "-" + illuminaDate.substring(4,6);

                                    List<String> fastqs = null;
                                    // if QC was not failed query Fastq database for path to the fastqs for that run
                                    if (!failedRuns.contains(runName))
                                        fastqs = FastQPathFinder.search(runId, s.getInvestigatorSampleId() + "_IGO_" + s.getIgoId());
                                    SampleManifest.Run r = new SampleManifest.Run(runMode, runId, flowCellId, laneNum.intValue(), readLength, dateCreated, fastqs);
                                    library.runs.add(r);
                                }
                            }
                        }

                        if (reqLanes.size() > 0) { // only report this library if it made it to a sequencer/run
                            List<SampleManifest.Library> libraries = s.getLibraries();
                            libraries.add(library);
                        }
                    }
                }

                smList.add(s);
            }
            log.info("Manifest generation time(ms):" + (System.currentTimeMillis()-startTime));
            return new GetSampleManifest.SampleManifestResult(smList, null);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static class FastQPathFinder {
        // TODO url to properties & make interface for FastQPathFinder
        public static List<String> search(String run, String sample_IGO_igoid) {
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

                // Return only most recent R1&R2 in case of re-demux
                if (fastqList.size() > 2)
                    fastqList = fastqList.subList(0,2);
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