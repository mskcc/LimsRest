package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.analytics.SequencingSampleData;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

public class GetSequencingSampleDataTask {

    private Log log = LogFactory.getLog(GetWESSampleDataTask.class);
    private String timestamp;
    private ConnectionLIMS conn;
    private User user;
    DataRecordManager dataRecordManager;
    private List<String> sequencerSerialNumbers = Arrays.asList("A00227", "A00333", "K00217", "D00330", "SN835", "D00592", "K00121", "M06566", "M04636", "M00430", "M00477", "NB501987", "M06566");
    private List<String> libraryTypeValues = Arrays.asList("dna library", "cdna library", "pooled library");
    public GetSequencingSampleDataTask(String timestamp, ConnectionLIMS conn) {
        this.timestamp = timestamp;
        this.conn = conn;
    }


    public List<SequencingSampleData> execute() {
        long start = System.currentTimeMillis();
        try {
            VeloxConnection vConn = conn.getConnection();
            user = vConn.getUser();
            dataRecordManager = vConn.getDataRecordManager();

            log.info(" Starting GetSequencingSampleDataTask task using timestamp " + timestamp);
            List<DataRecord> sequenncingSampleRecords = new ArrayList<>();
            if (StringUtils.isBlank(timestamp)){
                sequenncingSampleRecords = dataRecordManager.queryDataRecords("SeqAnalysisSampleQC", null, user);
            }else{
                sequenncingSampleRecords = dataRecordManager.queryDataRecords("SeqAnalysisSampleQC", "DateCreated >= " + Long.parseLong(timestamp), user);
            }
            List<SequencingSampleData> sequencingData = new ArrayList<>();
            if (sequenncingSampleRecords.size()>0){
                for (DataRecord rec : sequenncingSampleRecords){
                    log.info("Sequencing QC record ID: " + rec.getLongVal("RecordId", user));
                    DataRecord sample = getFirstSampleUnderRequest(rec);
                    if (sample != null) {
                        log.info("sample record id: " + sample.getLongVal("RecordId", user));
                        String sampleId = sample.getStringVal("SampleId", user);
                        String otherSampleId = sample.getStringVal("OtherSampleId", user);
                        String sampleType = (String) getValueFromDataRecord(sample, "ExemplarSampleType", "String");
                        String specimenType = (String) getValueFromDataRecord(sample, "SampleOrigin", "String");
                        String recipe = (String) getValueFromDataRecord(sample, "Recipe", "String");
                        String tumornormal = (String) getValueFromDataRecord(sample, "TumorOrNormal", "String");
                        DataRecord sequencingRequirementsRec = getChildDataRecord("SeqRequirement", sample);
                        Double requestedReads = 0.0;
                        Integer requestedCoverage = 0;
                        String runType = "";
                        if (sequencingRequirementsRec != null) {
                            requestedReads = (Double) getValueFromDataRecord(sequencingRequirementsRec, "RequestedReads", "Double");
                            requestedCoverage = (Integer) getValueFromDataRecord(sequencingRequirementsRec, "CoverageTarget", "Integer");
                            runType = (String) getValueFromDataRecord(sequencingRequirementsRec, "SequencingRunType", "String");
                        }
                        String runName = (String) getValueFromDataRecord(rec, "SequencerRunFolder", "String");
                        log.info(runName);
                        DataRecord sequencingInstrument = null;
                        if (!StringUtils.isBlank(runName)) {
                            sequencingInstrument = getInstrument(runName);
                        }
                        String sequencingMachineName = null;
                        String sequencingMachineType = null;
                        if (sequencingInstrument != null) {
                            sequencingMachineName = (String) getValueFromDataRecord(sequencingInstrument, "InstrumentName", "String");
                            sequencingMachineType = (String) getValueFromDataRecord(sequencingInstrument, "InstrumentType", "String");
                        }
                        Double sampleConcentration = (Double) getValueFromDataRecord(sample, "Concentration", "Double");
                        Double concentrationLibrary = (Double) getLibraryConcentration(sample);
                        Long readsExamined = (Long) getValueFromDataRecord(rec, "ReadsExamined", "Long");
                        Long unpairedReadsExamined = (Long) getValueFromDataRecord(rec, "UnpairedReads", "Long");
                        Long totalReads = (Long) getValueFromDataRecord(rec, "TotalReads", "Long");
                        Double pctMrnaBases = (Double) getValueFromDataRecord(rec, "PercentMrnaBases", "Double");
                        Double pctIntragenicBases = (Double) getValueFromDataRecord(rec, "PercentIntergenicBases", "Double");
                        Double pctUtrBases = (Double) getValueFromDataRecord(rec, "PercentUtrBases", "Double");
                        Double pctCodingBases = (Double) getValueFromDataRecord(rec, "PercentCodingBases", "Double");
                        Double pctRiboBases = (Double) getValueFromDataRecord(rec, "PercentRibosomalBases", "Double");
                        Long unmappedReads = (Long) getValueFromDataRecord(rec, "UnmappedDupes", "Long");
                        Long readPairDuplicates = (Long) getValueFromDataRecord(rec, "ReadPairDupes", "Long");
                        Double percentDuplication = (Double) getValueFromDataRecord(rec, "PercentDuplication", "Double");
                        Double mskq = (Double) getValueFromDataRecord(rec, "Mskq", "Double");
                        Double pctTarget100x = (Double) getValueFromDataRecord(rec, "PercentTarget100X", "Double");
                        Double pctTarget80x = (Double) getValueFromDataRecord(rec, "PercentTarget80X", "Double");
                        Double pctTarget40x = (Double) getValueFromDataRecord(rec, "PercentTarget40X", "Double");
                        Double pctTarget30x = (Double) getValueFromDataRecord(rec, "PercentTarget30X", "Double");
                        Double pctTarget10x = (Double) getValueFromDataRecord(rec, "PercentTarget10X", "Double");
                        Double meanTargetCoverage = (Double) getValueFromDataRecord(rec, "MeanTargetCoverage", "Double");
                        Double pctOffBait = (Double) getValueFromDataRecord(rec, "PercentOffBait", "Double");
                        Double pctAdapters = (Double) getValueFromDataRecord(rec, "PercentAdapters", "Double");

                        SequencingSampleData seqData = new SequencingSampleData(sampleId, otherSampleId, sampleType, specimenType, recipe, tumornormal, requestedReads, requestedCoverage,
                                runType, sequencingMachineName, sequencingMachineType, sampleConcentration, concentrationLibrary, readsExamined, unpairedReadsExamined, totalReads, pctMrnaBases,
                                pctIntragenicBases, pctUtrBases, pctCodingBases, pctRiboBases, unmappedReads, readPairDuplicates, mskq, pctTarget100x, pctTarget80x, pctTarget40x, pctTarget30x,
                                pctTarget10x, meanTargetCoverage, pctOffBait, pctAdapters);
                        sequencingData.add(seqData);
                        log.info("created data for sample: " + seqData.getSampleId());
                    }
                }
            }
            log.info("Num dmpTracker Records: " + sequencingData.size());
            return sequencingData;
        }catch (Throwable e){
            log.error(e.getMessage(), e);
        }
        return null;
    }


    public DataRecord getFirstSampleUnderRequest(DataRecord seqQcRec) throws IoError, RemoteException {
        DataRecord sampleRec = null;
        Stack <DataRecord> sampleStack = new Stack<>();
        List<DataRecord> parentSamples = seqQcRec.getParentsOfType("Sample", user);
        if (parentSamples.size() > 0){
            sampleStack.addAll(parentSamples);
        }
        while (sampleStack.size() > 0){
            DataRecord nextSample = sampleStack.pop();
            if (nextSample.getParentsOfType("Request", user).size()>0){
                return nextSample;
            }
            else if(nextSample.getParentsOfType("Sample", user).size()>0){
                sampleStack.push(nextSample.getParentsOfType("Sample", user).get(0));
            }
        }
        return sampleRec;
    }

    public DataRecord getParentRequest(DataRecord sample) throws IoError, RemoteException {
        List<DataRecord> requests = sample.getParentsOfType("Request", user);
        if (requests.size() > 0){
            return requests.get(0);
        }
        return null;
    }

    public SequencingSampleData createSequencingSampleData(DataRecord seqQcRec) throws IoError, RemoteException {
        DataRecord startSample = getFirstSampleUnderRequest(seqQcRec);
        DataRecord request = getParentRequest(startSample);

        return null;
    }

    /**
     * Get a DataField value from a DataRecord.
     *
     * @param record
     * @param fieldName
     * @param fieldType
     * @return Object
     * @throws NotFound
     * @throws RemoteException
     */
    private Object getValueFromDataRecord(DataRecord record, String fieldName, String fieldType) throws NotFound, RemoteException {
        if (record == null) {
            return null;
        }
        if (record.getValue(fieldName, user) != null) {
            if (fieldType.equals("String")) {
                return record.getStringVal(fieldName, user);
            }
            if (fieldType.equals("Integer")) {
                return record.getIntegerVal(fieldName, user);
            }
            if (fieldType.equals("Long")) {
                return record.getLongVal(fieldName, user);
            }
            if (fieldType.equals("Double")) {
                return record.getDoubleVal(fieldName, user);
            }
            if (fieldType.equals("Date")) {
                SimpleDateFormat dateFormatter = new SimpleDateFormat("MM-dd-yyyy");
                log.info(dateFormatter.format(new Date(record.getDateVal(fieldName, user))));
                return dateFormatter.format(new Date(record.getDateVal(fieldName, user)));
            }
        }
        return null;
    }

    public DataRecord getChildDataRecord(String dataRecordName, DataRecord sample) throws IoError, RemoteException {
        Stack<DataRecord> dataStack = new Stack<>();
        dataStack.push(sample);
        while (dataStack.size()>0){
            DataRecord nextSample = dataStack.pop();
            if (nextSample.getChildrenOfType(dataRecordName, user).length >0){
                return nextSample.getChildrenOfType(dataRecordName, user)[0];
            }
            else if (nextSample.getChildrenOfType("Sample",user).length>0){
                dataStack.addAll(Arrays.asList(nextSample.getChildrenOfType("Sample",user)));
            }
        }
        return null;
    }

    public DataRecord getInstrument(String runName) throws IoError, RemoteException, NotFound {
        if (!StringUtils.isBlank(runName)){
            List<String> nameValues = Arrays.asList(runName.split("/|_"));
            List<DataRecord> instruments = new ArrayList<>();
            for (String value : nameValues){
                if (sequencerSerialNumbers.contains(value)){
                    instruments = dataRecordManager.queryDataRecords("Instrument", "InstrumentName = '" + value + "'", user);
                }
                else {
                    instruments = dataRecordManager.queryDataRecords("Instrument", "InstrumentName = '" + value + "'", user);
                }
                if (instruments.size() > 0){
                    return instruments.get(0);
                }
            }
        }
        return null;
    }

    public Double getLibraryConcentration(DataRecord sample) throws NotFound, RemoteException, IoError {

        Stack<DataRecord> dataStack = new Stack<>();
        dataStack.push(sample);
        while (dataStack.size()>0){
            DataRecord nextSample = dataStack.pop();
            if (nextSample.getValue("ExemplarSampleType", user) !=null && libraryTypeValues.contains(nextSample.getStringVal("ExemplarSampleType", user).toLowerCase())
                    && nextSample.getValue("ExemplarSampleStatus", user) != null && !nextSample.getStringVal("ExemplarSampleStatus", user).toLowerCase().contains("failed")){
                return (Double) getValueFromDataRecord(nextSample, "Concentration", "Double");
            }
            if (nextSample.getChildrenOfType("Sample", user).length > 0){
                dataStack.addAll(Arrays.asList(nextSample.getChildrenOfType("Sample", user)));
            }
        }
        return null;
    }
}

