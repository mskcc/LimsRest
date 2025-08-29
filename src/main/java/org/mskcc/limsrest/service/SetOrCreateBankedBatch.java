package org.mskcc.limsrest.service;

import com.velox.api.datarecord.AuditLog;
import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.lang3.StringUtils;
import org.mskcc.domain.sample.TumorNormalType;
import org.mskcc.limsrest.controller.SetBankedSamples;
import org.mskcc.limsrest.util.Messages;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A queued task that takes multiple banked sample requests and processes them in a single batch operation
 * This is more efficient than processing samples individually
 *
 * @author Generated
 */
public class SetOrCreateBankedBatch extends LimsTask {
    private List<SetBankedSamples.BankedSampleRequest> sampleRequests;
    private String igoUser;

    public SetOrCreateBankedBatch(List<SetBankedSamples.BankedSampleRequest> sampleRequests, String igoUser) {
        this.sampleRequests = sampleRequests;
        this.igoUser = igoUser;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public Object execute(VeloxConnection conn) {
        List<String> results = new ArrayList<>();
        
        try {
            // Process all samples in a single batch
            for (SetBankedSamples.BankedSampleRequest sampleRequest : sampleRequests) {
                try {
                    String recordId = processSingleSample(sampleRequest);
                    results.add(recordId);
                } catch (Exception e) {
                    results.add(Messages.ERROR_IN + " SETTING BANKED SAMPLE: " + e.getMessage());
                }
            }
            
            // Commit all changes at once
            AuditLog log = user.getAuditLog();
            log.stopLogging(); // because users of this service might include PHI in their banked sample which will need corrected
            dataRecordManager.storeAndCommit(igoUser + " added information to " + sampleRequests.size() + " banked samples",null, user);
            log.startLogging();
            
        } catch (Throwable e) {
            e.printStackTrace();
            return Messages.ERROR_IN + " BATCH SETTING BANKED SAMPLES: " + e.getMessage();
        }

        return results;
    }

    private String processSingleSample(SetBankedSamples.BankedSampleRequest sampleRequest) throws Exception {
        String sampleId = sampleRequest.getUserId();
        String serviceId = sampleRequest.getServiceId();
        
        if (sampleId == null || sampleId.equals("")) {
            throw new LimsException("Must have a sample id to set banked sample");
        }

        // Query for existing banked sample
        List<DataRecord> matchedBanked = dataRecordManager.queryDataRecords("BankedSample", 
            "OtherSampleId = '" + sampleId + "' and ServiceId = '" + serviceId + "'", user);

        DataRecord banked;
        HashMap<String, Object> bankedFields = new HashMap<>();
        boolean setInvestigator = true;
        
        if (matchedBanked.size() < 1) {
            banked = dataRecordManager.addDataRecord("BankedSample", user);
            bankedFields.put("OtherSampleId", sampleId);
            bankedFields.put("RowIndex", Integer.parseInt(sampleRequest.getRowIndex()));
            bankedFields.put("TransactionId", Long.parseLong(sampleRequest.getTransactionId()));
        } else {
            banked = matchedBanked.get(0);
            try {
                if (!"".equals(banked.getStringVal("Investigator", user))) {
                    setInvestigator = false;
                }
            } catch (NullPointerException npe) {
                // Ignore NPE
            }
        }

        // Set species defaults based on recipe
        String species = sampleRequest.getSpecies();
        String recipe = sampleRequest.getRecipe();
        if (recipe != null && recipe.contains("Mouse")) {
            species = "Mouse";
        } else if (recipe != null && recipe.startsWith("HC_")) {
            species = "Human";
        }

        // Process assay array
        String[] assay = sampleRequest.getAssay();
        if (assay != null && assay.length > 0 && !"".equals(assay[0])) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < assay.length - 1; i++) {
                sb.append("'");
                sb.append(assay[i]);
                sb.append("',");
            }
            sb.append("'");
            sb.append(assay[assay.length - 1]);
            sb.append("'");
            String assayString = sb.toString();
            bankedFields.put("Assay", assayString);
        }

        // Set all the fields
        setFieldIfNotNull(bankedFields, "OtherSampleId", sampleId, "NULL");
        setFieldIfNotNull(bankedFields, "UserSampleID", sampleId, "NULL");
        setFieldIfNotNull(bankedFields, "Investigator", sampleRequest.getInvestigator(), "NULL", setInvestigator);
        setFieldIfNotNull(bankedFields, "ClinicalInfo", sampleRequest.getClinicalInfo(), "NULL");
        setFieldIfNotNull(bankedFields, "CollectionYear", sampleRequest.getCollectionYear(), "NULL");
        setFieldIfNotNull(bankedFields, "ConcentrationUnits", sampleRequest.getConcentrationUnits(), "NULL");
        setFieldIfNotNull(bankedFields, "Gender", sampleRequest.getGender(), "NULL");
        setFieldIfNotNull(bankedFields, "GeneticAlterations", sampleRequest.getGeneticAlterations(), "NULL");
        setFieldIfNotNull(bankedFields, "Preservation", sampleRequest.getPreservation(), "NULL");
        setFieldIfNotNull(bankedFields, "SpecimenType", sampleRequest.getSpecimenType(), "NULL");
        setFieldIfNotNull(bankedFields, "SampleType", sampleRequest.getSampleType(), "NULL");
        setFieldIfNotNull(bankedFields, "SampleClass", sampleRequest.getSampleClass(), "NULL");
        setFieldIfNotNull(bankedFields, "Species", species, "NULL");
        setFieldIfNotNull(bankedFields, "SpikeInGenes", sampleRequest.getSpikeInGenes(), "NULL");
        setFieldIfNotNull(bankedFields, "TissueSite", sampleRequest.getTissueType(), "NULL");
        setFieldIfNotNull(bankedFields, "MicronicTubeBarcode", sampleRequest.getMicronicTubeBarcode(), "NULL");
        setFieldIfNotNull(bankedFields, "BarcodeId", sampleRequest.getBarcodeId(), "NULL");
        setFieldIfNotNull(bankedFields, "Recipe", sampleRequest.getRecipe(), "NULL");
        setFieldIfNotNull(bankedFields, "CapturePanel", sampleRequest.getCapturePanel(), "NULL");
        setFieldIfNotNull(bankedFields, "RunType", sampleRequest.getRunType(), "NULL");
        setFieldIfNotNull(bankedFields, "ServiceId", sampleRequest.getServiceId(), "NULL");
        setFieldIfNotNull(bankedFields, "TubeBarcode", sampleRequest.getTubeId(), "NULL");
        setFieldIfNotNull(bankedFields, "PatientId", sampleRequest.getPatientId(), "NULL");
        setFieldIfNotNull(bankedFields, "NormalizedPatientId", sampleRequest.getNormalizedPatientId(), "NULL");
        setFieldIfNotNull(bankedFields, "CMOPatientId", sampleRequest.getCmoPatientId(), "NULL");
        setFieldIfNotNull(bankedFields, "RowPosition", sampleRequest.getRowPos(), "NULL");
        setFieldIfNotNull(bankedFields, "ColPosition", sampleRequest.getColPos(), "NULL");
        setFieldIfNotNull(bankedFields, "PlateId", sampleRequest.getPlateId(), "NULL");
        setFieldIfNotNull(bankedFields, "CellCount", sampleRequest.getCellCount(), "NULL");
        setFieldIfNotNull(bankedFields, "NAtoExtract", sampleRequest.getNaToExtract(), "NULL");
        setFieldIfNotNull(bankedFields, "SampleOrigin", sampleRequest.getSampleOrigin(), "NULL");
        setFieldIfNotNull(bankedFields, "NumTubes", sampleRequest.getNumTubes(), "NULL");

        // Handle special fields
        if (sampleRequest.getRequestedReads() != null && !"".equals(sampleRequest.getRequestedReads())) {
            bankedFields.put("RequestedReads", sampleRequest.getRequestedReads());
        }
        if (sampleRequest.getRequestedCoverage() != null && !"".equals(sampleRequest.getRequestedCoverage())) {
            bankedFields.put("RequestedCoverage", sampleRequest.getRequestedCoverage());
        }
        if (sampleRequest.getEstimatedPurity() != null && !"".equals(sampleRequest.getEstimatedPurity())) {
            bankedFields.put("EstimatedPurity", sampleRequest.getEstimatedPurity());
        }
        if (sampleRequest.getCancerType() != null && !sampleRequest.getCancerType().isEmpty()) {
            bankedFields.put("TumorType", sampleRequest.getCancerType());
        }

        // Set TumorOrNormal derived field
        bankedFields.put("TumorOrNormal", setTumorOrNormal(sampleRequest.getSampleClass(), sampleRequest.getCancerType(), sampleId));

        // Set numeric fields directly on the record
        float vol = Float.parseFloat(sampleRequest.getVol());
        if (vol > 0.0) {
            banked.setDataField("Volume", vol, user);
        }

        int numberOfAmplicons = Integer.parseInt(sampleRequest.getNumberOfAmplicons());
        if (numberOfAmplicons > 0) {
            bankedFields.put("NumberOfAmplicons", numberOfAmplicons);
        }

        double concentration = Double.parseDouble(sampleRequest.getConcentration());
        if (concentration > 0.0) {
            banked.setDataField("Concentration", concentration, user);
        }

        // Set all fields at once
        banked.setFields(bankedFields, user);

        return Long.toString(banked.getRecordId());
    }

    private void setFieldIfNotNull(HashMap<String, Object> bankedFields, String fieldName, String value, String nullValue) {
        setFieldIfNotNull(bankedFields, fieldName, value, nullValue, true);
    }

    private void setFieldIfNotNull(HashMap<String, Object> bankedFields, String fieldName, String value, String nullValue, boolean shouldSet) {
        if (shouldSet && value != null && !nullValue.equals(value)) {
            bankedFields.put(fieldName, value);
        }
    }

    /**
     * Based on Sample Class & Tumor Type set the derived field TumorOrNormal
     * Sample Class (REX)	                    Tumor Type (REX)                TumororNormal (LIMS)
     * Normal or Adjacent Normal	            Normal, Other, blank or null 	Normal
     * Normal or Adjacent Normal	            Tumor	                        Normal
     * Unknown Tumor, Primary, Metastasis,
     * Adjacent Tissue, Local Recurrence	    Normal 	                        Error message at upload
     * Unknown Tumor, Primary, Metastasis,
     * Adjacent Tissue, or Local Recurrence	Tumor, Other, blank or null	    Tumor
     * Other	                                Normal, Other, blank or null	Normal
     * Other	                                Tumor	                        Tumor
     */
    private String setTumorOrNormal(String sampleClass, String tumorType, String sampleId) throws IllegalArgumentException {
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
            // sample class 'Other' picklist was removed from the LIMS on Feb. 5, 2024
            case "Other":
                // Normal, Other, blank or null
                if (tumorType == null || tumorType.trim().isEmpty() || "Other".equalsIgnoreCase(tumorType) || "Normal".equalsIgnoreCase(tumorType)) {
                    return TumorNormalType.NORMAL.getValue();
                } else {
                    return TumorNormalType.TUMOR.getValue();
                }

            default:
                return "";
        }
    }
}
