package org.mskcc.limsrest.limsapi;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.web.GetSampleManifest;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class GetSampleManifestTask extends LimsTask {
    private Log log = LogFactory.getLog(GetSampleManifestTask.class);

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

                // 06302_R_1 has no sampleCMOInfoRecord so use the same fields at the sample level
                DataRecord cmoInfo;
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
                s.setGender(cmoInfo.getStringVal("Gender", user));

                // library concentration & volume
                // often null in samplecmoinforecords then query KAPALibPlateSetupProtocol1.TargetMassAliq1
                //String dmpLibraryInput = cmoInfo.getStringVal("DMPLibraryInput", user); // often null
                //String dmpLibraryOutput = cmoInfo.getStringVal("DMPLibraryOutput", user); // LIBRARY_YIELD
                List<DataRecord> aliquots = sample.getDescendantsOfType("Sample", user);
                for (DataRecord aliquot : aliquots) {
                    String sampleType = aliquot.getStringVal("ExemplarSampleType", user);
                    if ("DNA Library".equals(sampleType)) {
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
                                    //SequencerRunFolder example /ifs/lola/150814_LOLA_1298_BC7259ACXX/
                                    DataRecord seqExperiment = possibleRun.get(0);
                                    String runMode  = seqExperiment.getStringVal("SequencingRunMode", user);
                                    String flowCellId = seqExperiment.getStringVal("FlowcellId", user);
                                    String readLength = seqExperiment.getStringVal("ReadLength", user); // TODO blank in LIMS prior to April 2019

                                    // TODO function to convert Illumna yymmdd date as yyyy-MM-dd ?
                                    String[] runFolderElements = seqExperiment.getStringVal("SequencerRunFolder", user).split("_");
                                    String runId = runFolderElements[1] + "_" + runFolderElements[2];
                                    String illuminaDate = runFolderElements[0].substring(runFolderElements[0].length()-6); // yymmdd
                                    String dateCreated = "20" + illuminaDate.substring(0,2) + "-" + illuminaDate.substring(2,4) + "-" + illuminaDate.substring(4,6);

                                    SampleManifest.Run r = new SampleManifest.Run(runMode, runId, flowCellId, laneNum.intValue(), readLength, dateCreated);
                                    library.runs.add(r);
                                }
                            }
                        }
                        // TODO add fastq /ifs/archive path

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
}