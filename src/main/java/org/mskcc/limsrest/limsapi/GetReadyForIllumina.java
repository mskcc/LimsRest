package org.mskcc.limsrest.limsapi;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.NotFound;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.limsapi.search.NearestAncestorSearcher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A queued task that takes shows all samples that need planned for Illumina runs
 * 
 * @author Aaron Gabow
 */
@Service
public class GetReadyForIllumina extends LimsTask {
    private Log log = LogFactory.getLog(GetReadyForIllumina.class);

    private HashMap<String, List<String>> request2OutstandingSamples;

    public void init(){
    request2OutstandingSamples  = new HashMap<>();
  }

    @PreAuthorize("hasRole('READ')")
    @Override
    public Object execute(VeloxConnection conn) {
        List<RunSummary> results = new LinkedList<>();
        try {
            List<DataRecord> samplesToPool = dataRecordManager.queryDataRecords("Sample", "ExemplarSampleStatus = 'Ready for - Pooling of Sample Libraries for Sequencing'", user);
            for (DataRecord sample : samplesToPool) {
                String sampleId = sample.getStringVal("SampleId", user);
                if (sampleId.startsWith("Pool-")) {
                    double poolConcentration = 0.0;
                    try {
                        poolConcentration = sample.getDoubleVal("Concentration", user);
                    } catch (NullPointerException npe) {
                    }
                    double poolVolume = 0.0;
                    try {
                        poolVolume = sample.getDoubleVal("Volume", user);
                    } catch (NullPointerException npe) {
                    }
                    String status = sample.getStringVal("ExemplarSampleStatus", user);

                    Set<DataRecord> visited = new HashSet<>();
                    Deque<DataRecord> queue = new LinkedList<>();
                    queue.add(sample);
                    // for every pool get parents of type sample that don't start with "Pool-"
                    while (!queue.isEmpty()) {
                        DataRecord current = queue.removeFirst();
                        String currentSampleId = current.getStringVal("SampleId", user);
                        if (currentSampleId.startsWith("Pool-")) {
                            List<DataRecord> parents = current.getParentsOfType("Sample", user);
                            for (DataRecord parent : parents) {
                                if (!visited.contains(parent)) {
                                    queue.addLast(parent);
                                }
                            }
                        } else {
                            RunSummary runSummary = annotateUnpooledSample(current);
                            runSummary.setPool(sampleId);
                            runSummary.setConcentration(poolConcentration);
                            runSummary.setVolume(Double.toString(poolVolume));
                            runSummary.setStatus(status);
                            results.add(runSummary);
                        }
                        visited.add(current);
                    }

                } else {
                    results.add(annotateUnpooledSample(sample));
                }
            }
        } catch (Throwable e) {
            log.error("plan runs", e);
        }

        return results;
    }

    private Long getCreateDate(DataRecord record) {
        try {
            return record.getDateVal("DateCreated", user);
        } catch (NotFound | RemoteException e) {
            log.info("Failed to find a date created for record: " + record.toString());
        }
        return 0L;
    }

    public RunSummary annotateUnpooledSample(DataRecord sample) {
        RunSummary summary = new RunSummary("DEFAULT", "DEFAULT");
        try {
            Map<String, Object> baseFields = sample.getFields(user);
            summary = new RunSummary("", (String) baseFields.get("SampleId"));
            summary.setOtherSampleId((String) baseFields.getOrDefault("OtherSampleId", ""));
            summary.setRequestId((String) baseFields.getOrDefault("RequestId", ""));
            summary.setTubeBarcode((String) baseFields.getOrDefault("MicronicTubeBarcode", ""));
            summary.setStatus((String) baseFields.getOrDefault("ExemplarSampleStatus", ""));
            summary.setTumor((String) baseFields.getOrDefault("TumorOrNormal", ""));
            summary.setWellPos(baseFields.getOrDefault("ColPosition", "") + (String) baseFields.getOrDefault("RowPosition", ""));
            summary.setConcentrationUnits((String) baseFields.getOrDefault("ConcentrationUnits", ""));
            Double concentration = (Double) baseFields.get("Concentration");
            if (concentration != null)
                summary.setAltConcentration(concentration);
            Double volume = (Double) baseFields.get("Volume");
            if (volume == null)
                summary.setVolume("null");
            else
                summary.setVolume(volume.toString());

            summary.setPlateId((String) baseFields.getOrDefault("RelatedRecord23", ""));

            List<DataRecord> ancestorSamples = sample.getAncestorsOfType("Sample", user);
            ancestorSamples.add(0, sample);
            List<Long> ancestorSamplesCreateDate = ancestorSamples.stream().
                    map(this::getCreateDate).
                    collect(Collectors.toList());
            List<List<Map<String, Object>>> ancestorBarcodes = dataRecordManager.getFieldsForChildrenOfType(ancestorSamples,"IndexBarcode", user);
            List<List<Map<String, Object>>> ancestorPlanningProtocols = dataRecordManager.getFieldsForChildrenOfType(ancestorSamples,"BatchPlanningProtocol", user);
            List<List<Map<String, Object>>> ancestorSeqRequirements = dataRecordManager.getFieldsForChildrenOfType(ancestorSamples,"SeqRequirement", user);

            Map<String, Map<String, Object>> nearestAncestorFields =
                    NearestAncestorSearcher.findMostRecentAncestorFields(ancestorSamplesCreateDate,
                            ancestorBarcodes, ancestorPlanningProtocols, ancestorSeqRequirements);
            Map<String, Object> barcodeFields = nearestAncestorFields.get("BarcodeFields");
            Map<String, Object> planFields = nearestAncestorFields.get("PlanFields");
            Map<String, Object> reqFields = nearestAncestorFields.get("ReqFields");

            summary.setBarcodeId((String) barcodeFields.getOrDefault("IndexId", ""));
            summary.setBarcodeSeq((String) barcodeFields.getOrDefault("IndexTag", ""));
            summary.setSequencer((String) planFields.getOrDefault("RunPlan", ""));
            summary.setBatch((String) planFields.getOrDefault("WeekPlan", ""));
            summary.setRunType((String) reqFields.getOrDefault("SequencingRunType", ""));
            Object reads = reqFields.getOrDefault("RequestedReads", "");
            if (reads instanceof String){
                summary.setReadNum((String) reads);
            } else if (reads instanceof Double){
                summary.setReadNum(((Double) reads).toString());
            } else{
                summary.setReadNum("");
            }
        } catch (Exception e) {
            log.error("Annotate", e);
        }
        return summary;
     }
}