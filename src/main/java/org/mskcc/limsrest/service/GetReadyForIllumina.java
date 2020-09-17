package org.mskcc.limsrest.service;

import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.SampleModel;
import com.velox.sloan.cmo.recmodels.SeqRequirementModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;

import org.mskcc.limsrest.util.Utils;
import org.springframework.security.access.prepost.PreAuthorize;

import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.util.*;

import static org.mskcc.limsrest.util.Utils.getRecordsOfTypeFromParents;

/**
 * A queued task that takes shows all samples that need planned for Illumina runs. This endpoint will return the sample level information for individual Library samples and pooled Library samples.
 * The information is important for Pool planning for sequencing and making important pooling decisions.
 */
public class GetReadyForIllumina {
    private static Log log = LogFactory.getLog(GetReadyForIllumina.class);
    private ConnectionLIMS conn;

    public GetReadyForIllumina(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @PreAuthorize("hasRole('READ')")
    public List<RunSummary> execute() {
        List<RunSummary> results = new LinkedList<>();
        try {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            DataRecordManager dataRecordManager = vConn.getDataRecordManager();

            List<DataRecord> samplesToPool = dataRecordManager.queryDataRecords("Sample", "ExemplarSampleStatus = 'Ready for - Pooling of Sample Libraries for Sequencing'", user);
            if (samplesToPool.size()>0){
                for (DataRecord sample: samplesToPool){
                    String sampleId = sample.getStringVal("SampleId", user);
                    if (sampleId.toLowerCase().startsWith("pool-")){
                        List<DataRecord> parentLibrarySamplesForPool = getNearestParentLibrarySamplesForPool(sample, user); // if sample is pool then get all the Library samples in the pool which live as parents of the pool.
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
                            results.add(createRunSummaryForSampleInPool(librarySample, summary, user)); //pass the summary Object with preset pool level information "createRunSummaryForSampleInPool" method to add sample level information
                        }
                    } else{
                        RunSummary defaultSummary = new RunSummary("DEFAULT", "DEFAULT"); // if sample is not pool, then it is Library sample and work with it.
                        try {
                            results.add(createRunSummaryForNonPooledSamples(sample, defaultSummary, user));
                        } catch (IllegalStateException e){
                            // Continue processing remaining data records
                            log.error(e.getMessage());
                        }
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
    private List<DataRecord> getNearestParentLibrarySamplesForPool(DataRecord pooledSample, User user) throws IoError, RemoteException, NotFound {
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
    private DataRecord getParentSampleWithDesiredChildTypeRecord(DataRecord sample, String childDataType, User user) throws IoError, RemoteException, NotFound {
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
    private String getSampleLibraryIndexIdAndBarcode(DataRecord sample, User user) throws NotFound, RemoteException, IoError {
        DataRecord parentSample = getParentSampleWithDesiredChildTypeRecord(sample, "IndexBarcode", user);
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

    private Double getRequestedReadsForSample(DataRecord sample, User user) throws IoError, RemoteException, NotFound, ServerException, InvalidValue {
        DataRecord sampleWithSeqRequirementAsChild = getParentSampleWithDesiredChildTypeRecord(sample, "SeqRequirement", user);
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

    private String getPlannedSequencerForSample(DataRecord sample, User user) throws IoError, RemoteException, NotFound {
        DataRecord sampleWithBatchPlanningAsChild = getParentSampleWithDesiredChildTypeRecord(sample, "BatchPlanningProtocol", user);
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
    private String getPlannedWeekSample(DataRecord sample, User user) throws IoError, RemoteException, NotFound {
        DataRecord sampleWithBatchPlanningAsChild = getParentSampleWithDesiredChildTypeRecord(sample, "BatchPlanningProtocol", user);
        if (sampleWithBatchPlanningAsChild != null && sampleWithBatchPlanningAsChild.getChildrenOfType("BatchPlanningProtocol", user)[0].getValue("WeekPlan", user) != null){
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
    private String getSequencingRunTypeForSample(DataRecord sample, User user) throws IoError, RemoteException, NotFound {
        DataRecord sampleWithSeqRequirementAsChild = getParentSampleWithDesiredChildTypeRecord(sample, "SeqRequirement", user);
        if (sampleWithSeqRequirementAsChild != null && sampleWithSeqRequirementAsChild.getChildrenOfType("SeqRequirement", user)[0].getValue("SequencingRunType", user) != null){
            DataRecord sequencingRunTypeRecord = sampleWithSeqRequirementAsChild.getChildrenOfType("SeqRequirement", user)[0]; //continue here
            return sequencingRunTypeRecord.getStringVal("SequencingRunType", user);
        } else {
            log.error(String.format("Invalid Sequencing RunType '%s' for sample '%s'.\nPlease double check.", null, sample.getStringVal("SampleId", user)));
            return "";
        }
    }

    private String getRecipeForSample(DataRecord sample, User user) throws NotFound, RemoteException {
        if (sample.getValue("Recipe", user) !=null){
            return sample.getValue("Recipe",user).toString();
        }
        return "";
    }

    /**
     * This method will create the Summary Object for the sample not part of a pool.
     * @param unpooledSample
     * @param summary
     * @param user
     * @return Run Summary for sample.
     * @throws NotFound
     * @throws RemoteException
     * @throws IoError
     * @throws InvalidValue
     */
    private RunSummary createRunSummaryForNonPooledSamples(DataRecord unpooledSample, RunSummary summary, User user)
            throws NotFound, RemoteException, IoError, InvalidValue {
        Map<String, Object> sampleFieldValues = unpooledSample.getFields(user);
        String sampleId = (String) sampleFieldValues.get("SampleId");
        summary.setSampleId(sampleId);
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
        summary.setRecipe(getRecipeForSample(unpooledSample, user));
        summary.setPlateId((String) sampleFieldValues.getOrDefault("RelatedRecord23", ""));
        String indexAndBarcode = getSampleLibraryIndexIdAndBarcode(unpooledSample, user);
        if (indexAndBarcode !=null && indexAndBarcode.split(",").length==2){
            summary.setBarcodeId(indexAndBarcode.split(",")[0]);
            summary.setBarcodeSeq(indexAndBarcode.split(",")[1]);
        } else {
            throw new IllegalStateException(String.format("Failed to retrieve barcode ID & Seq from IGO Sample Id: %s. Not adding summary.", sampleId));
        }
        summary.setReadNum(getRequestedReadsForSample(unpooledSample, user).toString());
        List<DataRecord> seqReqrmts = getRecordsOfTypeFromParents(unpooledSample, SampleModel.DATA_TYPE_NAME, SeqRequirementModel.DATA_TYPE_NAME, user);
        if (seqReqrmts.size()>0){
            DataRecord seqReq = seqReqrmts.get(0);
            summary.setReadTotal(seqReq.getValue(SeqRequirementModel.READ_TOTAL, user) != null ? seqReq.getLongVal(SeqRequirementModel.READ_TOTAL, user): 0);
            summary.setRemainingReads(seqReq.getValue("RemainingReads", user) != null ? seqReq.getLongVal("RemainingReads", user) : 0);
        }
        String plannedSequencer = getPlannedSequencerForSample(unpooledSample, user);
        if (plannedSequencer != null)
            summary.setSequencer(plannedSequencer);
        String batchWeek = getPlannedWeekSample(unpooledSample, user);
        if (batchWeek != null)
            summary.setBatch(batchWeek);
        summary.setRunType(getSequencingRunTypeForSample(unpooledSample, user));
        return summary;
    }

    /**
     * This method adds sample level metadata to summary for samples that are part of the pool. This method expects a summary that has some pool level
     * information added to it.
     * @param sampleInPool
     * @param summary
     * @return Run Summary for sample in pool.
     * @throws RemoteException
     * @throws NotFound
     * @throws IoError
     * @throws InvalidValue
     */
    private RunSummary createRunSummaryForSampleInPool(DataRecord sampleInPool, RunSummary summary, User user) throws RemoteException, NotFound, IoError, InvalidValue {
        Map<String, Object> sampleFieldValues = sampleInPool.getFields(user);
        summary.setSampleId((String) sampleFieldValues.get("SampleId"));
        summary.setOtherSampleId((String) sampleFieldValues.getOrDefault("OtherSampleId", ""));
        summary.setRequestId((String) sampleFieldValues.getOrDefault("RequestId", ""));
        summary.setTubeBarcode((String) sampleFieldValues.getOrDefault("MicronicTubeBarcode", ""));
        summary.setTumor((String) sampleFieldValues.getOrDefault("TumorOrNormal", ""));
        summary.setWellPos(sampleFieldValues.getOrDefault("ColPosition", "") + (String) sampleFieldValues.getOrDefault("RowPosition", ""));
        summary.setConcentrationUnits((String) sampleFieldValues.getOrDefault("ConcentrationUnits", ""));
        summary.setRecipe(getRecipeForSample(sampleInPool, user));
        Double concentration = (Double) sampleFieldValues.get("Concentration");
        if (concentration != null)
            summary.setAltConcentration(concentration);
        summary.setPlateId((String) sampleFieldValues.getOrDefault("RelatedRecord23", ""));
        String indexAndBarcode = getSampleLibraryIndexIdAndBarcode(sampleInPool, user);
        if (indexAndBarcode !=null && indexAndBarcode.split(",").length==2)
            summary.setBarcodeId(indexAndBarcode.split(",")[0]);
        List<DataRecord> seqReqrmts = getRecordsOfTypeFromParents(sampleInPool, SampleModel.DATA_TYPE_NAME, SeqRequirementModel.DATA_TYPE_NAME, user);
        if (seqReqrmts.size()>0){
            DataRecord seqReq = seqReqrmts.get(0);
            summary.setReadTotal(seqReq.getValue(SeqRequirementModel.READ_TOTAL, user) != null ? seqReq.getLongVal(SeqRequirementModel.READ_TOTAL, user): 0);
            summary.setRemainingReads(seqReq.getValue("RemainingReads", user) != null ? seqReq.getLongVal("RemainingReads", user) : 0);
        }
        summary.setBarcodeSeq(indexAndBarcode.split(",")[1]);
        summary.setReadNum(getRequestedReadsForSample(sampleInPool, user).toString());
        String plannedSequencer = getPlannedSequencerForSample(sampleInPool, user);
        if (plannedSequencer != null)
            summary.setSequencer(plannedSequencer);
        String batchWeek = getPlannedWeekSample(sampleInPool, user);
        if (batchWeek != null)
            summary.setBatch(batchWeek);
        summary.setRunType(getSequencingRunTypeForSample(sampleInPool, user));
        return summary;
    }
}