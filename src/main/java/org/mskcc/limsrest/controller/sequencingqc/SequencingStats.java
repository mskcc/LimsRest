package org.mskcc.limsrest.controller.sequencingqc;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sloan.cmo.recmodels.IlluminaSeqExperimentModel;
import com.velox.sloan.cmo.recmodels.IndexBarcodeModel;
import com.velox.sloan.cmo.recmodels.SampleModel;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.sequencingqc.UpdateLimsSampleLevelSequencingQcTask;

import java.rmi.RemoteException;
import java.util.*;

import static org.mskcc.limsrest.util.Utils.*;

public class SequencingStats {

    private Log log = LogFactory.getLog(UpdateLimsSampleLevelSequencingQcTask.class);
    private final static List<String> POOLED_SAMPLE_TYPES = Collections.singletonList("pooled library");
    private final String POOLEDNORMAL_IDENTIFIER = "POOLEDNORMAL";
    private final String CONTROL_IDENTIFIER = "CTRL";
    private final String FAILED = "Failed";
    DataRecordManager dataRecordManager;
    String appPropertyFile = "/app.properties";
    String inital_qc_status = "Under-Review";
    private ConnectionLIMS conn;
    User user;

    private String runId;
    private String projectId;
    /**
     * Method to get all the Library Samples related to Sequencing Run.
     *
     * @param runId
     * @return
     */
    public List<DataRecord> getRelatedLibrarySamples(String runId) {
        Set<String> addedSampleIds = new HashSet<>();
        List<DataRecord> flowCellSamples = new ArrayList<>();
        try {
            log.info(String.format("Querying table %s where field %s LIKE RUN %s", IlluminaSeqExperimentModel.DATA_TYPE_NAME, IlluminaSeqExperimentModel.SEQUENCER_RUN_FOLDER, runId));
            List<DataRecord> illuminaSeqExperiments = dataRecordManager.queryDataRecords(IlluminaSeqExperimentModel.DATA_TYPE_NAME, IlluminaSeqExperimentModel.SEQUENCER_RUN_FOLDER + " LIKE '%" + runId + "%'", user);
            log.info("Calling getSamplesRelatedToSeqExperiment " + illuminaSeqExperiments.size());
            List<DataRecord> relatedSamples = getSamplesRelatedToSeqExperiment(illuminaSeqExperiments, runId, user);
            log.info(String.format("Total Related Samples for IlluminaSeq Run %s: %d", runId, relatedSamples.size()));
            Stack<DataRecord> sampleStack = new Stack<>();
            if (relatedSamples.isEmpty()) {
                return flowCellSamples;
            }
            sampleStack.addAll(relatedSamples);
            do {
                DataRecord stackSample = sampleStack.pop();
                Object sampleType = stackSample.getValue(SampleModel.EXEMPLAR_SAMPLE_TYPE, user);
                Object samId = stackSample.getValue(SampleModel.SAMPLE_ID, user);
                log.info("Sample Type: " + sampleType);
                if (!samId.toString().toLowerCase().startsWith("pool") || (sampleType != null && !POOLED_SAMPLE_TYPES.contains(sampleType.toString().toLowerCase()))) {
                    String sampleId = stackSample.getStringVal(SampleModel.SAMPLE_ID, user);
                    log.info("Adding sample to Library Samples List: " + sampleId);
                    if (addedSampleIds.add(sampleId)) {
                        flowCellSamples.add(stackSample);
                    }
                } else {
                    sampleStack.addAll(stackSample.getParentsOfType(SampleModel.DATA_TYPE_NAME, user));
                }
            } while (!sampleStack.isEmpty());
        } catch (NotFound | RemoteException | IoError | NullPointerException | ServerException notFound) {
            log.error(String.format("%s-> Error while getting related Library Samples for run %s:\n%s", ExceptionUtils.getRootCauseMessage(notFound), runId, ExceptionUtils.getStackTrace(notFound)));
        }
        log.info(String.format("Total Samples related to run %d , Sample Ids: %s", flowCellSamples.size(), Arrays.toString(addedSampleIds.toArray())));
        return flowCellSamples;
    }
    /**
     * Method to get a Library Sample from List of Library Samples related to the Sequencing Run with matching SampleId.
     *
     * @param relatedLibrarySamples
     * @return
     */
    public DataRecord getLibrarySample(List<DataRecord> relatedLibrarySamples, String sampleId) {
        try {
            for (DataRecord sample : relatedLibrarySamples) {
                String baseId = getBaseSampleId(sample.getStringVal(SampleModel.SAMPLE_ID, user));
                if (baseId.equalsIgnoreCase(sampleId)) {
                    return sample;
                }
                // Older SampleId values started with "CTRL" for POOLEDNORMAL control Samples. With last update, the control Samples now
                // start with actual control sample type eg "FFPEPOOLEDNORMAL, MOUSEPOOLEDNORMAL etc." we need to validate both old and new pattern
                // for POOLEDNORMAL control samples.
                if ((sampleId.contains(POOLEDNORMAL_IDENTIFIER) && baseId.toUpperCase().contains(POOLEDNORMAL_IDENTIFIER))
                        || (sampleId.contains(CONTROL_IDENTIFIER) && baseId.toUpperCase().contains(CONTROL_IDENTIFIER))) {
                    String[] pooleNormalIdVals = sampleId.split("_");
                    assert pooleNormalIdVals.length > 1;
                    String barcode = pooleNormalIdVals[pooleNormalIdVals.length - 1];
                    return getPooledNormalLibrarySample(relatedLibrarySamples, barcode);
                }
            }
        } catch (NotFound | RemoteException notFound) {
            log.error(String.format("%s-> Error while retrieving matching sample by sample id '%s' from list of samples:\n%s", ExceptionUtils.getRootCauseMessage(notFound), sampleId, ExceptionUtils.getStackTrace(notFound)));
        }
        return null;
    }

    /**
     * Method to get parent poolednormal sample based on barcode. NGS-STATS sampleid for poolednormals is named to contain
     * type of normal (POOLEDNORMAL, FFPEPOOLEDNORMAL, MOUSEPOOLEDNORMAL etc.), Recipe and Index barcode (eg: FFPEPOOLEDNORMAL_IGO_IMPACT468_GTGAAGTG)
     * The only way to find correct pooled normal is to traverse through the library samples on the run and find pooled normal with same barcode.
     *
     * @param relatedLibrarySamples
     * @param barcode
     * @return
     */
    public DataRecord getPooledNormalLibrarySample(List<DataRecord> relatedLibrarySamples, String barcode) {
        try {
            log.info("Given POOLEDNORMAL barcode: " + barcode);
            for (DataRecord sam : relatedLibrarySamples) {
                Object sampleId = sam.getStringVal(SampleModel.SAMPLE_ID, user);
                Object otherSampleId = sam.getStringVal(SampleModel.OTHER_SAMPLE_ID, user);
                if ((sampleId != null && sampleId.toString().contains(POOLEDNORMAL_IDENTIFIER)) || (otherSampleId != null && otherSampleId.toString().contains(POOLEDNORMAL_IDENTIFIER))) {
                    List<DataRecord> indexBarcodeRecs = getRecordsOfTypeFromParents(sam, SampleModel.DATA_TYPE_NAME, IndexBarcodeModel.DATA_TYPE_NAME, user);
                    if (!indexBarcodeRecs.isEmpty()) {
                        Object samBarcode = indexBarcodeRecs.get(0).getValue(IndexBarcodeModel.INDEX_TAG, user);
                        log.info(indexBarcodeRecs.get(0).getDataTypeName());
                        log.info("IndexBarcode SampleId: " + indexBarcodeRecs.get(0).getStringVal("SampleId", user));
                        log.info("Assigned Index Barcode POOLEDNORMAL: " + samBarcode);
                        if (samBarcode != null) {
                            String i7Barcode = samBarcode.toString().split("-")[0];
                            if (i7Barcode.equalsIgnoreCase(barcode)) {
                                log.info("Found Library Sample for Pooled Normal");
                                return sam;
                            }
                        }
                    }
                }
            }
        } catch (NotFound | RemoteException | NullPointerException notFound) {
            log.error(String.format("%s-> Error while getting related Library Samples for run %s:\n%s", ExceptionUtils.getRootCauseMessage(notFound), runId, ExceptionUtils.getStackTrace(notFound)));
        }
        return null;
    }

    /**
     * Method to extract SampleId (IGO_ID) sampleId value in qc data. Qc data sample name is concatenation of
     * OtherSampleId , _IGO_, SampleID.
     *
     * @param id
     * @return
     */
    public String getIgoId(String id) {
        log.info("Stats IGO ID: " + id);
        List<String> idVals = Arrays.asList(id.split("_IGO_"));
        if (idVals.size() == 2) {
            String igoId = idVals.get(1);
            if (igoId.contains(POOLEDNORMAL_IDENTIFIER) || igoId.contains(CONTROL_IDENTIFIER)) {
                String pooledNormalName = idVals.get(0);
                String[] barcodeVals = idVals.get(1).split("_");
                String barcode = barcodeVals[barcodeVals.length - 1];
                return pooledNormalName + "_" + barcode;
            }
            return igoId;
        } else {
            throw new IllegalArgumentException(String.format("Cannot extract IGO ID from given Sample Name value %s in QC data.", id));
        }
    }

    /**
     * Method to get Run Name without Version Number
     *
     * @param runName
     * @return
     */
    public String getVersionLessRunId(String runName) {
        return runName.replaceFirst("_[A-Z][0-9]+$", "");
    }

    /**
     * Method to extract SampleId (IGO_ID) sampleId value in qc data. Qc data sample name is concatenation of
     * OtherSampleId , _IGO_, SampleID. And for POOLEDNORMALS it is 'POOLEDNORMAL', _IGO_, RECIPE, i7 barcode.
     *
     * @param id
     * @return
     */
    public String getIgoSampleName(String id) {
        List<String> idVals = Arrays.asList(id.split("_IGO_"));
        if (idVals.size() == 2) {
            return idVals.get(0).replaceFirst("_rescue$", "");
        } else {
            throw new IllegalArgumentException(String.format("Cannot extract IGO ID from given Sample Name value %s in QC data.", id));
        }
    }
}
