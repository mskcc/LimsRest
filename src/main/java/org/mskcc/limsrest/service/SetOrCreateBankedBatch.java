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
import java.util.stream.Collectors;

/**
 * A queued task that takes multiple banked sample requests and processes them in a single batch operation
 * This is more efficient than processing samples individually
 *
 * @author Rajiev Timal
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
        List<String> results = new ArrayList<>(sampleRequests.size());
        
        try {
            // Batch query for existing banked samples to reduce database calls
            Map<String, DataRecord> existingBankedSamples = batchQueryExistingBankedSamples();
            
            // Process all samples in parallel where possible
            List<DataRecord> recordsToUpdate = new ArrayList<>(sampleRequests.size());
            List<DataRecord> recordsToCreate = new ArrayList<>();
            
            for (SetBankedSamples.BankedSampleRequest sampleRequest : sampleRequests) {
                try {
                    DataRecord bankedRecord = processSingleSample(sampleRequest, existingBankedSamples);
                    if (bankedRecord != null) {
                        results.add(Long.toString(bankedRecord.getRecordId()));
                        if (existingBankedSamples.containsKey(getSampleKey(sampleRequest))) {
                            recordsToUpdate.add(bankedRecord);
                        } else {
                            recordsToCreate.add(bankedRecord);
                        }
                    }
                } catch (Exception e) {
                    results.add(Messages.ERROR_IN + " SETTING BANKED SAMPLE: " + e.getMessage());
                }
            }
            
            // Batch store new records
            if (!recordsToCreate.isEmpty()) {
                dataRecordManager.storeAndCommit(igoUser + " created " + recordsToCreate.size() + " new banked samples", null, user);
            }
            
            // Batch update existing records
            if (!recordsToUpdate.isEmpty()) {
                dataRecordManager.storeAndCommit(igoUser + " updated " + recordsToUpdate.size() + " existing banked samples", null, user);
            }
            
            // Commit all changes at once
            AuditLog log = user.getAuditLog();
            log.stopLogging(); // because users of this service might include PHI in their banked sample which will need corrected
            dataRecordManager.storeAndCommit(igoUser + " processed " + sampleRequests.size() + " banked samples", null, user);
            log.startLogging();
            
        } catch (Throwable e) {
            e.printStackTrace();
            return Messages.ERROR_IN + " BATCH SETTING BANKED SAMPLES: " + e.getMessage();
        }

        return results;
    }

    /**
     * Batch query for existing banked samples to reduce database calls
     */
    private Map<String, DataRecord> batchQueryExistingBankedSamples() throws Exception {
        if (sampleRequests.isEmpty()) {
            return new HashMap<>();
        }
        
        // Build a single query for all samples
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("OtherSampleId IN (");
        
        List<String> sampleIds = sampleRequests.stream()
            .map(SetBankedSamples.BankedSampleRequest::getUserId)
            .filter(id -> id != null && !id.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        
        if (sampleIds.isEmpty()) {
            return new HashMap<>();
        }
        
        for (int i = 0; i < sampleIds.size(); i++) {
            if (i > 0) queryBuilder.append(",");
            queryBuilder.append("'").append(sampleIds.get(i)).append("'");
        }
        queryBuilder.append(")");
        
        // Add service ID filter if all requests have the same service ID
        String commonServiceId = getCommonServiceId();
        if (commonServiceId != null) {
            queryBuilder.append(" AND ServiceId = '").append(commonServiceId).append("'");
        }
        
        List<DataRecord> existingRecords = dataRecordManager.queryDataRecords("BankedSample", queryBuilder.toString(), user);
        
        // Create a map for quick lookup
        Map<String, DataRecord> existingMap = new HashMap<>();
        for (DataRecord record : existingRecords) {
            String key = getSampleKey(record);
            existingMap.put(key, record);
        }
        
        return existingMap;
    }
    
    private String getCommonServiceId() {
        if (sampleRequests.isEmpty()) return null;
        
        String firstServiceId = sampleRequests.get(0).getServiceId();
        if (firstServiceId == null) return null;
        
        return sampleRequests.stream()
            .allMatch(req -> firstServiceId.equals(req.getServiceId())) ? firstServiceId : null;
    }
    
    private String getSampleKey(SetBankedSamples.BankedSampleRequest request) {
        return request.getUserId() + "|" + request.getServiceId();
    }
    
    private String getSampleKey(DataRecord record) throws Exception {
        return record.getStringVal("OtherSampleId", user) + "|" + record.getStringVal("ServiceId", user);
    }

    private DataRecord processSingleSample(SetBankedSamples.BankedSampleRequest sampleRequest, 
                                         Map<String, DataRecord> existingBankedSamples) throws Exception {
        String sampleId = sampleRequest.getUserId();
        String serviceId = sampleRequest.getServiceId();
        
        if (sampleId == null || sampleId.isEmpty()) {
            throw new LimsException("Must have a sample id to set banked sample");
        }

        // Check for existing banked sample from our batch query
        String sampleKey = getSampleKey(sampleRequest);
        DataRecord banked = existingBankedSamples.get(sampleKey);
        
        HashMap<String, Object> bankedFields = new HashMap<>(50); // Pre-allocate with expected size
        boolean setInvestigator = true;
        
        if (banked == null) {
            banked = dataRecordManager.addDataRecord("BankedSample", user);
            bankedFields.put("OtherSampleId", sampleId);
            bankedFields.put("RowIndex", Integer.parseInt(sampleRequest.getRowIndex()));
            bankedFields.put("TransactionId", Long.parseLong(sampleRequest.getTransactionId()));
        } else {
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

        // // Process assay array more efficiently
        // String[] assay = sampleRequest.getAssay();
        // if (assay != null && assay.length > 0 && !"".equals(assay[0])) {
        //     // Use StringBuilder instead of StringBuffer for better performance
        //     StringBuilder sb = new StringBuilder(assay.length * 20); // Pre-allocate reasonable size
        //     for (int i = 0; i < assay.length; i++) {
        //         if (i > 0) sb.append(",");
        //         sb.append("'").append(assay[i]).append("'");
        //     }
        //     bankedFields.put("Assay", sb.toString());
        // }

        setFieldIfNotNull(bankedFields, "SequencingReadLength", sampleRequest.getSequencingReadLength(), "NULL");
        setFieldIfNotNull(bankedFields, "Assay", sampleRequest.getAssay(), "NULL");
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

        // Set all fields at once for better performance
        banked.setFields(bankedFields, user);

        return banked;
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
