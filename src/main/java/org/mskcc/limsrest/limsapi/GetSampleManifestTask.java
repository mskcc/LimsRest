package org.mskcc.limsrest.limsapi;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
        try {
            List<SampleManifest> smList = new ArrayList<>();

            for (String igoId : igoIds) {
                log.info("Creating sample manifest for IGO ID:" + igoId);
                // TODO check sample exists
                List<DataRecord> sampleCMOInfoRecords = dataRecordManager.queryDataRecords("SampleCMOInfoRecords", "SampleId = '" + igoId +  "'", user);

                List<DataRecord> samples = dataRecordManager.queryDataRecords("Sample", "SampleId = '" + igoId + "'", user);
                DataRecord sample = samples.get(0);

                // 06302_R_1 has no sampleCMOInfoRecord so use the same fields at the sample level
                DataRecord cmoInfo;
                if (sampleCMOInfoRecords.size() == 0) {
                    cmoInfo = samples.get(0);
                } else {
                    cmoInfo = sampleCMOInfoRecords.get(0);
                }
                String sampleId = cmoInfo.getStringVal("SampleId", user);
                String altId = cmoInfo.getStringVal("AltId", user);
                SampleManifest s = new SampleManifest();
                s.setIgoId(igoId);

                String cmoPatientId = cmoInfo.getStringVal("CmoPatientId", user);
                s.setCmoPatientId(cmoPatientId);
                String userSampleId = cmoInfo.getStringVal("UserSampleID", user);
                s.setInvestigatorSampleId(userSampleId + "_IGO_" + sampleId);
                s.setOncotreeCode(cmoInfo.getStringVal("TumorType", user));
                s.setSampleClass(cmoInfo.getStringVal("TumorOrNormal", user));
                s.setTissueSite(cmoInfo.getStringVal("TissueLocation", user));
                s.setSampleType(cmoInfo.getStringVal("SampleOrigin", user));
                s.setPreservation(cmoInfo.getStringVal("Preservation", user));
                s.setCollectionYear(cmoInfo.getStringVal("CollectionYear", user));
                s.setGender(cmoInfo.getStringVal("Gender", user));

                // TODO create lists of items when multiple values are possible i.e. sample re-pooled

                // library input & library yield
                // if null in samplecmoinforecords then query KAPALibPlateSetupProtocol1.TargetMassAliq1
                String dmpLibraryInput = cmoInfo.getStringVal("DMPLibraryInput", user); // often null
                String dmpLibraryOutput = cmoInfo.getStringVal("DMPLibraryOutput", user); // LIBRARY_YIELD
                if (dmpLibraryInput != null) {
                    s.setLibraryInputNg(Double.parseDouble(dmpLibraryInput));
                    s.setLibraryYieldNg(Double.parseDouble(dmpLibraryOutput));
                } else {
                    long created = -1;
                    double mass = 0.00000;
                    DataRecord[] kProtocols = sample.getChildrenOfType("KAPALibPlateSetupProtocol1", this.user);
                    for (DataRecord protocol : kProtocols){
                        long protoCreate = protocol.getDateVal("DateCreated", this.user);
                        if (protoCreate  > created){
                            mass = protocol.getDoubleVal("TargetMassAliq1", this.user);
                            s.setLibraryInputNg(mass);
                            created = protoCreate;
                        }
                    }
                    // TODO library yield & library concentration
                    double concentration = 0.0;
                    double elutionVolume = 0.0;
                    Double libraryYield = concentration * elutionVolume;
                    s.setLibraryYieldNg(libraryYield);
                }

                List<DataRecord> indexBarcodes = sample.getDescendantsOfType("IndexBarcode", user);
                if (indexBarcodes != null && indexBarcodes.size() > 0) {
                    DataRecord bc = indexBarcodes.get(0);
                    s.setBarcodeId(bc.getStringVal("IndexId", user));
                    s.setBarcodeIndex(bc.getStringVal("IndexTag", user));
                    for (int i=1; i < indexBarcodes.size(); i++) {
                        bc = indexBarcodes.get(i);
                        s.setBarcodeId(s.getBarcodeId() + "," + bc.getStringVal("IndexId", user));
                        s.setBarcodeIndex(s.getBarcodeIndex() + "," + bc.getStringVal("IndexTag", user));
                    }
                }

                // recipe, capture input, capture name
                List<DataRecord> nimbleGen = sample.getDescendantsOfType("NimbleGenHybProtocol", user);
                log.info("Found nimbleGen records: " + nimbleGen.size());
                for (DataRecord n : nimbleGen) {
                    String poolName = n.getStringVal("Protocol2Sample", user);
                    if (poolName !=null && poolName.contains("Tube")) { // avoid SourceMassToUse == null
                        String recipe = n.getStringVal("Recipe", user);
                        s.setRecipe(recipe);
                        Double captureInput = n.getDoubleVal("SourceMassToUse", user);
                        s.setCaptureInputNg(captureInput.toString());
                        s.setCaptureName(poolName);
                        Double volume = n.getDoubleVal("VolumeToUse", user);
                        s.setCaptureConcentrationNm(volume.toString());
                    }
                }

                // run Mode, runId, flow Cell & Lane Number
                List<DataRecord> reqLanes = sample.getDescendantsOfType("FlowCellLane", user);
                for (DataRecord flowCellLane : reqLanes) {
                    Long laneNum = flowCellLane.getLongVal("LaneNum", user);
                    log.info("Getting a flow cell lane");
                    List<DataRecord> flowcell = flowCellLane.getParentsOfType("FlowCell", user);
                    if (flowcell.size() > 0) {
                        log.info("Getting a flow cell");
                        List<DataRecord> possibleRun = flowcell.get(0).getParentsOfType("IlluminaSeqExperiment", user);
                        if (possibleRun.size() > 0) {
                            log.info("Getting a run");
                            //SequencerRunFolder example /ifs/lola/150814_LOLA_1298_BC7259ACXX/
                            DataRecord runDR = possibleRun.get(0);
                            String runMode  = runDR.getStringVal("SequencingRunMode", user);
                            String flowCellId = runDR.getStringVal("FlowcellId", user);
                            String[] runFolderElements = runDR.getStringVal("SequencerRunFolder", user).split("_");
                            String runId = runFolderElements[1] + "_" + runFolderElements[2];
                            SampleManifest.Run r = new SampleManifest.Run(runMode, runId, flowCellId, laneNum.intValue());
                            List<SampleManifest.Run> l = s.getRuns();
                            l.add(r);
                            s.setRuns(l);
                        }
                    }
                }

                smList.add(s);
            }

            return smList;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}