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

                // TODO often null here, query KAPALibPlateSetupProtocol1.TargetMassAliq1
                String dmpLibraryInput = cmoInfo.getStringVal("DMPLibraryInput", user); // often null
                String dmpLibraryOutput = cmoInfo.getStringVal("DMPLibraryOutput", user); // LIBRARY_YIELD
                if (dmpLibraryInput != null) {
                    s.setLibraryInputNg(dmpLibraryInput);
                    s.setLibraryYieldNg(dmpLibraryOutput);
                } else {
                    long created = -1;
                    double mass = 0.00000;
                    DataRecord[] kProtocols = sample.getChildrenOfType("KAPALibPlateSetupProtocol1", this.user);
                    for (DataRecord protocol : kProtocols){
                        long protoCreate = protocol.getDateVal("DateCreated", this.user);
                        if (protoCreate  > created){
                            mass = protocol.getDoubleVal("TargetMassAliq1", this.user);
                            s.setLibraryInputNg(Double.toString(mass));
                            created = protoCreate;
                        }
                    }
                }

                List<DataRecord> indexBarcodes = dataRecordManager.queryDataRecords("IndexBarcode", "SampleId = '" + igoId + "'", user);
                if (indexBarcodes != null && indexBarcodes.size() > 0) {
                    DataRecord bc = indexBarcodes.get(0);
                    s.setBarcodeId(bc.getStringVal("IndexId", user));
                    s.setBarcodeIndex(bc.getStringVal("IndexTag", user));
                    if (indexBarcodes.size() > 1)
                        System.err.println("ERROR, need LIST.");
                }


                List<DataRecord> nimbleGen = sample.getDescendantsOfType("NimbleGenHybProtocol", user);
                log.info("Found nimbleGen records: " + nimbleGen.size());
                for (DataRecord n : nimbleGen) {
                    String baitSet = n.getStringVal("Recipe", user);
                    System.out.println("B:" + baitSet);
                    s.setBaitSet(baitSet);
                }

                List<DataRecord> reqLanes = sample.getDescendantsOfType("FlowCellLane", user);
                for (DataRecord flowCellLane : reqLanes) {
                    Long laneNum = flowCellLane.getLongVal("LaneNum", user);
                    s.setLaneNumber(laneNum.toString());
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