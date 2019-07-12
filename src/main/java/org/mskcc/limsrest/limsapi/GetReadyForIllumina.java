package org.mskcc.limsrest.limsapi;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.InvalidValue;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.util.*;

/**
 * A queued task that takes shows all samples that need planned for Illumina runs. This endpoint will return the sample level information for individual Library samples and pooled Library samples.
 * The information is important for Pool planning for sequencing and making important pooling decisions.
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
            if (samplesToPool.size()>0){
                for (DataRecord sample: samplesToPool){
                    String sampleId = sample.getStringVal("SampleId", user);
                    if (sampleId.toLowerCase().startsWith("pool-")){
                        List<DataRecord> parentLibrarySamplesForPool = getNearestParentLibrarySamplesForPool(sample); // if sample is pool then get all the Library samples in the pool which live as parents of the pool.
                        for(DataRecord librarySample : parentLibrarySamplesForPool){
                            RunSummary summary = new RunSummary("DEFAULT", "DEFAULT");
                            //set some of the pool level fields on the summary object like pool
                            summary.setPool(sampleId); //preset poolID
                            summary.setConcentration(sample.getDoubleVal("Concentration", user)); //preset Pool Concentration
                            summary.setStatus(sample.getStringVal("ExemplarSampleStatus", user)); //preset Pool Status
                            if (sample.getValue("Volume", user) == null) //preset pool volume in this if else block
                                summary.setVolume("null");
                            else
                                summary.setVolume(sample.getValue("Volume", user).toString());
                            results.add(createRunSummaryForSampleInPool(librarySample, summary)); //pass the summary Object with preset pool level information "createRunSummaryForSampleInPool" method to add sample level information
                        }
                    }else{
                        RunSummary summary = new RunSummary("DEFAULT", "DEFAULT"); // if sample is not pool, then it is Library sample and work with it.
                        results.add(createRunSummaryForNonPooledSamples(sample,summary));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Annotate", e);
        }
        return results;
     }

    /**
     * If the sample is pool, get all the samples of type Library that are present in the pool.
     * @param pooledSample
     * @return List of Library samples in the pooled sample.
     * @throws IoError
     * @throws RemoteException
     * @throws NotFound
     */
    private List<DataRecord> getNearestParentLibrarySamplesForPool(DataRecord pooledSample) throws IoError, RemoteException, NotFound {
        List<DataRecord> parentLibrarySamplesForPool = new ArrayList<>();
        Stack<DataRecord> sampleTrackingStack = new Stack<>();
        sampleTrackingStack.add(pooledSample);
        while(!sampleTrackingStack.isEmpty()){
            List<DataRecord> parentSamples = sampleTrackingStack.pop().getParentsOfType("Sample", user);
            if (!parentSamples.isEmpty()){
                for (DataRecord sample : parentSamples){
                    if (sample.getStringVal("SampleId", user).toLowerCase().startsWith("pool-")){
                        sampleTrackingStack.push(sample);
                    }
                    else{
                        parentLibrarySamplesForPool.add(sample);
                    }
                }
            }
        }
        return parentLibrarySamplesForPool;
    }

    /**
     * This method is designed to find and return the Sample in hierarchy with a desired child DataType
     *
     * @param sample
     * @param childDataType
     * @return Sample DataRecord
     * @throws IoError
     * @throws RemoteException
     */
    private DataRecord getParentSampleWithDesiredChildTypeRecord(DataRecord sample, String childDataType) throws IoError, RemoteException, NotFound {
        if (sample.getChildrenOfType(childDataType, user).length>0){
            return sample;
        }
        DataRecord record = null;
        Stack<DataRecord> sampleTrackingPile = new Stack<>();
        sampleTrackingPile.push(sample);
        do {
            DataRecord startSample = sampleTrackingPile.pop();
            List<DataRecord> parentRecords = startSample.getParentsOfType("Sample", user);
            if (!parentRecords.isEmpty() && parentRecords.get(0).getChildrenOfType(childDataType, user).length>0) {
                record = parentRecords.get(0);
            }
            if (!parentRecords.isEmpty() && record == null) {
                sampleTrackingPile.push(parentRecords.get(0));
            }
        } while (!sampleTrackingPile.empty());
        return record;
    }

    /**
     * This method returns the comma separated value of indexId and IndexBarcode for the sample.
     *
     * @param sample
     * @return String IndexId value for Sample if found, else returns "" with a warning.
     * @throws NotFound
     * @throws RemoteException
     * @throws IoError
     */
    private String getSampleLibraryIndexIdAndBarcode(DataRecord sample) throws NotFound, RemoteException, IoError {
        DataRecord parentSample = getParentSampleWithDesiredChildTypeRecord(sample, "IndexBarcode");
        if (parentSample != null && parentSample.getChildrenOfType("IndexBarcode", user)[0].getValue("IndexId", user) != null) {
            DataRecord recordWithIndexBarcodeInfo = parentSample.getChildrenOfType("IndexBarcode", user)[0];
            String indexId = recordWithIndexBarcodeInfo.getStringVal("IndexId", user);
            String indexBarcode = recordWithIndexBarcodeInfo.getStringVal("IndexTag", user);
            return indexId + "," + indexBarcode;
        }else {
            log.info(String.format("IndexId not found for sample '%s'.\nPlease double check.", sample.getStringVal("SampleId", user)));
            return "";
        }
    }

    /**
     * This method returns the Requested Reads value for sample.
     *
     * @param sample
     * @return Double value RequestedReads value from SeqRequirement/SeqRequirementPooled record if found, else returns 0.0 with a warning.
     * @throws IoError
     * @throws RemoteException
     * @throws NotFound
     * @throws ServerException
     * @throws InvalidValue
     */

    private Double getRequestedReadsForSample(DataRecord sample) throws IoError, RemoteException, NotFound, ServerException, InvalidValue {
        DataRecord sampleWithSeqRequirementAsChild = getParentSampleWithDesiredChildTypeRecord(sample, "SeqRequirement");
        if (sampleWithSeqRequirementAsChild !=null && sampleWithSeqRequirementAsChild.getChildrenOfType("SeqRequirement", user)[0].getValue("RequestedReads", user) != null) {
            DataRecord seqRequirements = sampleWithSeqRequirementAsChild.getChildrenOfType("SeqRequirement", user)[0];
            return seqRequirements.getDoubleVal("RequestedReads", user);
        } else {
            log.error(String.format("Invalid Sequencing Requirements '%s' for sample '%s'.\nPlease double check.", null, sample.getStringVal("SampleId", user)));
            return 0.0;
        }
    }

    /**
     * This method returns the Sequencer name that the sample is planned to run on. This is a value which is not populated anymore, but the method is included to match the
     * legacy code and front end design.
     * @param sample
     * @return Sequencing Run Type value
     * @throws IoError
     * @throws RemoteException
     * @throws NotFound
     */

    private String getPlannedSequencerForSample(DataRecord sample) throws IoError, RemoteException, NotFound {
        DataRecord sampleWithBatchPlanningAsChild = getParentSampleWithDesiredChildTypeRecord(sample, "BatchPlanningProtocol");
        if (sampleWithBatchPlanningAsChild !=null && sampleWithBatchPlanningAsChild.getChildrenOfType("BatchPlanningProtocol",user)[0].getValue("SequencingRunType", user) != null){
            DataRecord batchPlanningRecord = sampleWithBatchPlanningAsChild.getChildrenOfType("BatchPlanningProtocol", user)[0]; //continue here
            return batchPlanningRecord.getStringVal("SequencingRunType", user);
        } else {
            log.error(String.format("Invalid Sequencing Run Type '%s' for sample '%s'.\nPlease double check.", "null", sample.getStringVal("SampleId", user)));
            return "";
        }
    }


    /**
     * This method returns the Week Number that the sample is planned to run on. This is a value which is not populated anymore, but the method is included to match the
     * legacy code and front end design.
     * @param sample
     * @return Week Number Value
     * @throws IoError
     * @throws RemoteException
     * @throws NotFound
     */
    private String getPlannedWeekSample(DataRecord sample) throws IoError, RemoteException, NotFound {
        DataRecord sampleWithBatchPlanningAsChild = getParentSampleWithDesiredChildTypeRecord(sample, "BatchPlanningProtocol");
        if (sampleWithBatchPlanningAsChild != null && sampleWithBatchPlanningAsChild.getChildrenOfType("BatchPlanningProtocol", user)[0].getValue("WeekPlan", user) !=null){
            DataRecord batchPlanningRecord = sampleWithBatchPlanningAsChild.getChildrenOfType("BatchPlanningProtocol", user)[0]; //continue here
            return  batchPlanningRecord.getStringVal("WeekPlan", user);
        } else {
            log.error(String.format("No 'PlannedWeek' info found for sample '%s'.\nPlease double check.", sample.getStringVal("SampleId", user)));
            return "";
        }
    }


    /**
     * This method returns the SequencingRunType value for the sample.
     * @param sample
     * @return Sequencing Run Type value
     * @throws IoError
     * @throws RemoteException
     * @throws NotFound
     */
    private String getSequencingRunTypeForSample(DataRecord sample) throws IoError, RemoteException, NotFound {
        DataRecord sampleWithSeqRequirementAsChild = getParentSampleWithDesiredChildTypeRecord(sample, "SeqRequirement");
        if (sampleWithSeqRequirementAsChild != null && sampleWithSeqRequirementAsChild.getChildrenOfType("SeqRequirement", user)[0].getValue("SequencingRunType", user) != null){
            DataRecord sequencingRunTypeRecord = sampleWithSeqRequirementAsChild.getChildrenOfType("SeqRequirement", user)[0]; //continue here
            return sequencingRunTypeRecord.getStringVal("SequencingRunType", user);
        } else {
            log.error(String.format("Invalid Sequencing RunType '%s' for sample '%s'.\nPlease double check.", null, sample.getStringVal("SampleId", user)));
            return "";
        }
    }

    private String getRecipeForSample(DataRecord sample){
        if
    }

    /**
     * This method will create the Summary Object for the sample which is added to the results list and passed to the user.
     * @param unpooledSample
     * @param summary
     * @return Run Summary for sample.
     * @throws NotFound
     * @throws RemoteException
     * @throws IoError
     * @throws InvalidValue
     */
    private RunSummary createRunSummaryForNonPooledSamples(DataRecord unpooledSample, RunSummary summary) throws NotFound, RemoteException, IoError, InvalidValue {
        Map<String, Object> sampleFieldValues = unpooledSample.getFields(user);
        summary.setSampleId((String) sampleFieldValues.get("SampleId"));
        summary.setOtherSampleId((String) sampleFieldValues.getOrDefault("OtherSampleId", ""));
        summary.setRequestId((String) sampleFieldValues.getOrDefault("RequestId", ""));
        summary.setTubeBarcode((String) sampleFieldValues.getOrDefault("MicronicTubeBarcode", ""));
        summary.setStatus((String) sampleFieldValues.getOrDefault("ExemplarSampleStatus", ""));
        summary.setTumor((String) sampleFieldValues.getOrDefault("TumorOrNormal", ""));
        summary.setWellPos(sampleFieldValues.getOrDefault("ColPosition", "") + (String) sampleFieldValues.getOrDefault("RowPosition", ""));
        summary.setConcentrationUnits((String) sampleFieldValues.getOrDefault("ConcentrationUnits", ""));
        Double concentration = (Double) sampleFieldValues.get("Concentration");
        if (concentration != null)
            summary.setAltConcentration(concentration);
        Double volume = (Double) sampleFieldValues.get("Volume");
        if (volume == null)
            summary.setVolume("null");
        else
            summary.setVolume(volume.toString());
        summary.setPlateId((String) sampleFieldValues.getOrDefault("RelatedRecord23", ""));
        String indexAndBarcode = getSampleLibraryIndexIdAndBarcode(unpooledSample);
        if (indexAndBarcode !=null && indexAndBarcode.split(",").length==2)
            summary.setBarcodeId(indexAndBarcode.split(",")[0]);
            summary.setBarcodeSeq(indexAndBarcode.split(",")[1]);
        summary.setReadNum(getRequestedReadsForSample(unpooledSample).toString());
        String plannedSequencer = getPlannedSequencerForSample(unpooledSample);
        if (plannedSequencer != null)
            summary.setSequencer(plannedSequencer);
        String batchWeek = getPlannedWeekSample(unpooledSample);
        if (batchWeek != null)
            summary.setBatch(batchWeek);
        summary.setRunType(getSequencingRunTypeForSample(unpooledSample));
        return summary;
    }

    private RunSummary createRunSummaryForSampleInPool(DataRecord sampleInPool, RunSummary summary) throws RemoteException, NotFound, IoError, InvalidValue {
        Map<String, Object> sampleFieldValues = sampleInPool.getFields(user);
        summary.setSampleId((String) sampleFieldValues.get("SampleId"));
        summary.setOtherSampleId((String) sampleFieldValues.getOrDefault("OtherSampleId", ""));
        summary.setRequestId((String) sampleFieldValues.getOrDefault("RequestId", ""));
        summary.setTubeBarcode((String) sampleFieldValues.getOrDefault("MicronicTubeBarcode", ""));
        summary.setTumor((String) sampleFieldValues.getOrDefault("TumorOrNormal", ""));
        summary.setWellPos(sampleFieldValues.getOrDefault("ColPosition", "") + (String) sampleFieldValues.getOrDefault("RowPosition", ""));
        summary.setConcentrationUnits((String) sampleFieldValues.getOrDefault("ConcentrationUnits", ""));
        Double concentration = (Double) sampleFieldValues.get("Concentration");
        if (concentration != null)
            summary.setAltConcentration(concentration);
        summary.setPlateId((String) sampleFieldValues.getOrDefault("RelatedRecord23", ""));
        String indexAndBarcode = getSampleLibraryIndexIdAndBarcode(sampleInPool);
        if (indexAndBarcode !=null && indexAndBarcode.split(",").length==2)
            summary.setBarcodeId(indexAndBarcode.split(",")[0]);
        summary.setBarcodeSeq(indexAndBarcode.split(",")[1]);
        summary.setReadNum(getRequestedReadsForSample(sampleInPool).toString());
        String plannedSequencer = getPlannedSequencerForSample(sampleInPool);
        if (plannedSequencer != null)
            summary.setSequencer(plannedSequencer);
        String batchWeek = getPlannedWeekSample(sampleInPool);
        if (batchWeek != null)
            summary.setBatch(batchWeek);
        summary.setRunType(getSequencingRunTypeForSample(sampleInPool));
        return summary;
    }
}
