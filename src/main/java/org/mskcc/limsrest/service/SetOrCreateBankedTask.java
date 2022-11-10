package org.mskcc.limsrest.service;

import com.velox.api.datarecord.AuditLog;
import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.mskcc.domain.sample.TumorNormalType;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.util.Messages;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.HashMap;
import java.util.List;


/**
 * A queued task that takes a sample id and other values and looks for a banked sample with that id
 * If no such sample is present but there is an appropriate request it will create it
 * The other values are then set
 *
 * @author Aaron Gabow
 */
public class SetOrCreateBankedTask {
    String[] assay;
    String barcodePosition;
    String cellCount;
    String clinicalInfo;
    String collectionYear;
    String concentrationUnits;
    String igoUser;
    String investigator;
    String gender;
    String geneticAlterations;
    String naToExtract;
    String numTubes;
    String organism;
    String platform;
    String preservation;
    String sampleId;
    String userId;
    String sequencingReadLength;
    String specimenType;
    String spikeInGenes;
    String tissueType;
    String cancerType;
    String micronicTubeBarcode;
    String barcodeId;
    String recipe;
    String capturePanel;
    String runType;
    String serviceId;
    String sampleType;
    String sampleClass;
    String sampleOrigin;
    String tubeId;
    String patientId;
    String normalizedPatientId;
    String cmoPatientId;
    String rowPosition;
    String colPosition;
    String plateId;
    String requestedReads;
    String requestedCoverage;
    Double estimatedPurity;
    float vol;
    double concentration;
    int rowIndex;
    long transactionId;
    int numberOfAmplicons;
    private ConnectionLIMS conn;

    public SetOrCreateBankedTask() {}

    public SetOrCreateBankedTask(
            String igoUser,
            String investigator,
            String sampleId,
            String[] assay,
            String clinicalInfo,
            String collectionYear,
            String concentrationUnits,
            String gender,
            String geneticAlterations,
            String organism,
            String platform,
            String preservation,
            String specimenType,
            String sampleType,
            String sampleOrigin,
            String sampleClass,
            String spikeInGenes,
            String tissueType,
            String cancerType,
            String micronicTubeBarcode,
            String barcodeId,
            String barcodePosition,
            String recipe,
            String capturePanel,
            String runType,
            String serviceId,
            String tubeId,
            String patientId,
            String normalizedPatientId,
            String cmoPatientId,
            String rowPos,
            String colPos,
            String plateId,
            String requestedReads,
            String requestedCoverage,
            String cellCount,
            String sequencingReadLength,
            String numTubes,
            String naToExtract,
            Double estimatedPurity,
            float vol,
            double concentration,
            int rowIndex,
            long transactionId,
            int numberOfAmplicons,
            ConnectionLIMS conn) {

        this.igoUser = igoUser;
        this.investigator = investigator;
        this.sampleId = sampleId;
        this.userId = sampleId;
        if (assay != null)
            this.assay = assay.clone();
        this.barcodePosition = barcodePosition;
        this.cellCount = cellCount;
        this.clinicalInfo = clinicalInfo;
        this.collectionYear = collectionYear;
        this.concentrationUnits = concentrationUnits;
        this.gender = gender;
        this.geneticAlterations = geneticAlterations;
        this.naToExtract = naToExtract;
        this.numTubes = numTubes;
        this.organism = organism;
        this.patientId = patientId;
        this.normalizedPatientId = normalizedPatientId;
        this.cmoPatientId = cmoPatientId;
        this.platform = platform;
        this.preservation = preservation;
        this.specimenType = specimenType;
        this.spikeInGenes = spikeInGenes;
        this.tubeId = tubeId;
        this.tissueType = tissueType;
        this.cancerType = cancerType;
        this.micronicTubeBarcode = micronicTubeBarcode;
        this.barcodeId = barcodeId;
        this.recipe = recipe;
        this.capturePanel = capturePanel;
        this.runType = runType;
        this.sequencingReadLength = sequencingReadLength;
        this.serviceId = serviceId;
        this.sampleType = sampleType;
        this.sampleClass = sampleClass;
        this.sampleOrigin = sampleOrigin;
        this.rowPosition = rowPos;
        this.colPosition = colPos;
        this.plateId = plateId;
        this.requestedReads = requestedReads;
        this.requestedCoverage = requestedCoverage;
        this.estimatedPurity = estimatedPurity;
        this.vol = vol;
        this.concentration = concentration;
        this.rowIndex = rowIndex;
        this.transactionId = transactionId;
        this.numberOfAmplicons = numberOfAmplicons;
        this.conn = conn;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public String execute() {

        synchronized (SetOrCreateBankedTask.class) {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            DataRecordManager dataRecordManager = vConn.getDataRecordManager();

            String recordId;
            try {
                if (sampleId == null || sampleId.equals("")) {
                    throw new LimsException("Must have a sample id to set banked sample");
                }
                List<DataRecord> matchedBanked = dataRecordManager.queryDataRecords("BankedSample", "OtherSampleId = '" +
                        sampleId + "' and ServiceId = '" + serviceId + "'", user);

                DataRecord banked;
                HashMap<String, Object> bankedFields = new HashMap<>();
                boolean setInvestigator = true;
                if (matchedBanked.size() < 1) {
                    banked = dataRecordManager.addDataRecord("BankedSample", user);
                    bankedFields.put("OtherSampleId", sampleId);
                    bankedFields.put("RowIndex", rowIndex);
                    bankedFields.put("TransactionId", transactionId);
                } else {
                    banked = matchedBanked.get(0);
                    try {
                        if (!"".equals(banked.getStringVal("Investigator", user))) {
                            setInvestigator = false;
                        }
                    } catch (NullPointerException npe) {
                    }
                }

                //default species values for recipes
                if (recipe.startsWith("IMPACT") || recipe.startsWith("HemePACT")) {
                    organism = "Human";
                } else if (recipe.startsWith("M-IMPACT")) {
                    organism = "Mouse";
                }
                if (assay != null && assay.length > 0 && !"".equals(assay[0])) {
                    StringBuffer sb = new StringBuffer();
                    for (int i = 0; i < assay.length - 1; i++) {
                        sb.append("'");
                        sb.append(assay[i]);
                        sb.append("',");
                    }
                    sb.append(assay[assay.length - 1]);
                    String assayString = sb.toString();
                    bankedFields.put("Assay", assayString);
                }
                if (!"NULL".equals(sampleId)) {
                    bankedFields.put("OtherSampleId", userId);
                    bankedFields.put("UserSampleID", userId);
                }
                if (!"NULL".equals(investigator) && setInvestigator) {
                    bankedFields.put("Investigator", investigator);
                }
                if (!"NULL".equals(clinicalInfo)) {
                    bankedFields.put("ClinicalInfo", clinicalInfo);
                }
                if (!"NULL".equals(collectionYear)) {
                    bankedFields.put("CollectionYear", collectionYear);
                }
                if (!"NULL".equals(concentrationUnits)) {
                    bankedFields.put("ConcentrationUnits", concentrationUnits);
                }
                if (!"NULL".equals(gender)) {
                    bankedFields.put("Gender", gender);
                }
                if (!"NULL".equals(geneticAlterations)) {
                    bankedFields.put("GeneticAlterations", geneticAlterations);
                }
                if (!"NULL".equals(platform)) {
                    bankedFields.put("Platform", platform);
                }
                if (!"NULL".equals(preservation)) {
                    bankedFields.put("Preservation", preservation);
                }
                if (!"NULL".equals(specimenType)) {
                    bankedFields.put("SpecimenType", specimenType);
                }
                if (!"NULL".equals(sampleType)) {
                    bankedFields.put("SampleType", sampleType);
                }
                if (!"NULL".equals(sampleClass)) {
                    bankedFields.put("SampleClass", sampleClass);
                }
                if (!"NULL".equals(organism)) {
                    bankedFields.put("Species", organism);
                }
                if (!"NULL".equals(spikeInGenes)) {
                    bankedFields.put("SpikeInGenes", spikeInGenes);
                }
                if (!"NULL".equals(tissueType)) {
                    bankedFields.put("TissueSite", tissueType);
                }
                if (!"NULL".equals(micronicTubeBarcode)) {
                    bankedFields.put("MicronicTubeBarcode", micronicTubeBarcode);
                }
                if (!"NULL".equals(barcodeId)) {
                    bankedFields.put("BarcodeId", barcodeId);
                }
                if (!"NULL".equals(recipe)) {
                    bankedFields.put("Recipe", recipe);
                }
                if (!"NULL".equals(capturePanel)) {
                    bankedFields.put("CapturePanel", capturePanel);
                }
                if (!"NULL".equals(runType)) {
                    bankedFields.put("RunType", runType);
                }
                if (!"NULL".equals(serviceId)) {
                    bankedFields.put("ServiceId", serviceId);
                }
                if (!"NULL".equals(tubeId)) {
                    bankedFields.put("TubeBarcode", tubeId);
                }
                if (!"NULL".equals(patientId)) {
                    bankedFields.put("PatientId", patientId);
                }
                if (!"NULL".equals(normalizedPatientId)) {
                    bankedFields.put("NormalizedPatientId", normalizedPatientId);
                }
                if (!"NULL".equals(cmoPatientId)) {
                    bankedFields.put("CMOPatientId", cmoPatientId);
                }
                if (!"NULL".equals(rowPosition)) {
                    bankedFields.put("RowPosition", rowPosition);
                }
                if (!"NULL".equals(colPosition)) {
                    bankedFields.put("ColPosition", colPosition);
                }
                if (!"NULL".equals(plateId)) {
                    bankedFields.put("PlateId", plateId);
                }
                if (!"NULL".equals(cellCount)) {
                    bankedFields.put("CellCount", cellCount);
                }
                if (!"NULL".equals(naToExtract)) {
                    bankedFields.put("NAtoExtract", naToExtract);
                }
                if (!"NULL".equals(sampleOrigin)) {
                    bankedFields.put("SampleOrigin", sampleOrigin);
                }
                if (!"NULL".equals(sequencingReadLength)) {
                    bankedFields.put("RunType", sequencingReadLength);
                }
                if (requestedReads != null && !"".equals(requestedReads)) {
                    bankedFields.put("RequestedReads", requestedReads);
                }
                if (requestedCoverage != null && !"".equals(requestedCoverage)) {
                    bankedFields.put("RequestedCoverage", requestedCoverage);
                }
                if (estimatedPurity != null && !"".equals(estimatedPurity)) {
                    bankedFields.put("EstimatedPurity", estimatedPurity);
                }
                if (cancerType != null && !cancerType.isEmpty()) {
                    bankedFields.put("TumorType", cancerType);
                }
                bankedFields.put("TumorOrNormal", setTumorOrNormal(sampleClass, cancerType, sampleId));

                if (vol > 0.0) {
                    banked.setDataField("Volume", vol, user);
                }
                if (!"NULL".equals(numTubes)) {
                    bankedFields.put("NumTubes", numTubes);
                }
                if (numberOfAmplicons > 0) {
                    bankedFields.put("NumberOfAmplicons", numberOfAmplicons);
                }
                if (concentration > 0.0) {
                    banked.setDataField("Concentration", concentration, user);
                }
                recordId = Long.toString(banked.getRecordId());
                banked.setFields(bankedFields, user);
                AuditLog log = user.getAuditLog();
                log.stopLogging(); //because users of this service might include PHI in their banked sample which will need corrected
                dataRecordManager.storeAndCommit(igoUser + " added information to banked sample " + sampleId, user);
                log.startLogging();
            } catch (Throwable e) {
                e.printStackTrace();
                return Messages.ERROR_IN + " SETTING BANKED SAMPLE: " + e.getMessage();
            }

            return recordId;
        }
    }

    /*
Based on Sample Class & Tumor Type set the derived field TumorOrNormal
Sample Class (REX)	                    Tumor Type (REX)                TumororNormal (LIMS)
Normal or Adjacent Normal	            Normal, Other, blank or null 	Normal
Normal or Adjacent Normal	            Tumor	                        Normal
Unknown Tumor, Primary, Metastasis,
Adjacent Tissue, Local Recurrence	    Normal 	                        Error message at upload
Unknown Tumor, Primary, Metastasis,
Adjacent Tissue, or Local Recurrence	Tumor, Other, blank or null	    Tumor
Other	                                Normal, Other, blank or null	Normal
Other	                                Tumor	                        Tumor
     */
    public static String setTumorOrNormal(String sampleClass, String tumorType, String sampleId) throws IllegalArgumentException {
        switch (sampleClass) {
            case "Normal":
            case "Adjacent Normal":
                return TumorNormalType.NORMAL.getValue();

            case "Unknown Tumor":
            case "Primary":
            case "Metastasis":
            case "Adjacent Tissue":
            case "Local Recurrence":
                if ("Normal".equalsIgnoreCase(tumorType)) {
                    throw new IllegalArgumentException(String.format("Tumor Type (%s) Inconsistent With Sample Class (%s) for SampleID: %s.", tumorType, sampleClass, sampleId));
                } else {
                    return TumorNormalType.TUMOR.getValue();
                }

            case "Other":
                // Normal, Other, blank or null
                if (tumorType == null || tumorType.trim().isEmpty() || "Other".equalsIgnoreCase(tumorType) || "Normal".equalsIgnoreCase(tumorType)) {
                    return TumorNormalType.NORMAL.getValue();
                } else {
                    return TumorNormalType.TUMOR.getValue();
                }

            default:
                return TumorNormalType.NORMAL.getValue();
        }
    }
}