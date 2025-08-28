package org.mskcc.limsrest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class SampleSheetService {
    
    private static final Logger log = LoggerFactory.getLogger(SampleSheetService.class);
    
    /**
     * Generate sample sheet for the given experiment ID
     * This is a simplified version for local development
     */
    public Map<String, Object> generateSampleSheet(String experimentId) {
        log.info("Generating sample sheet for experimentId: {}", experimentId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // For local development, return a mock sample sheet
            if (experimentId == null || experimentId.trim().isEmpty()) {
                throw new IllegalArgumentException("Experiment ID cannot be null or empty");
            }
            
            // Mock CSV content for testing
            String csvContent = generateMockSampleSheet(experimentId);
            
            result.put("csvContent", csvContent);
            result.put("filename", "sample_sheet_" + experimentId + ".csv");
            result.put("experimentId", experimentId);
            result.put("success", true);
            
            log.info("Successfully generated mock sample sheet for experiment: {}", experimentId);
            
        } catch (Exception e) {
            log.error("Error generating sample sheet for experimentId: {}", experimentId, e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("csvContent", "Error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Generate a mock sample sheet for testing purposes
     */
    private String generateMockSampleSheet(String experimentId) {
        StringBuilder csv = new StringBuilder();
        
        // Sample sheet header
        csv.append("[Header]\n");
        csv.append("IEMFileVersion,4\n");
        csv.append("Investigator Name,Test User\n");
        csv.append("Experiment Name,Experiment_").append(experimentId).append("\n");
        csv.append("Date,").append(java.time.LocalDate.now()).append("\n");
        csv.append("Workflow,GenerateFASTQ\n");
        csv.append("Application,FASTQ Only\n");
        csv.append("Assay,TruSeq HT\n");
        csv.append("Description,Mock sample sheet for testing\n");
        csv.append("Chemistry,Amplicon\n");
        csv.append("\n");
        
        // Reads section
        csv.append("[Reads]\n");
        csv.append("151\n");
        csv.append("151\n");
        csv.append("\n");
        
        // Settings section
        csv.append("[Settings]\n");
        csv.append("ReverseComplement,0\n");
        csv.append("Adapter,AGATCGGAAGAGCACACGTCTGAACTCCAGTCA\n");
        csv.append("AdapterRead2,AGATCGGAAGAGCGTCGTGTAGGGAAAGAGTGT\n");
        csv.append("\n");
        
        // Data section
        csv.append("[Data]\n");
        csv.append("Sample_ID,Sample_Name,Sample_Plate,Sample_Well,I7_Index_ID,index,I5_Index_ID,index2,Sample_Project,Description\n");
        
        // Generate some mock samples
        for (int i = 1; i <= 5; i++) {
            csv.append("Sample_").append(experimentId).append("_").append(i)
               .append(",Sample_").append(experimentId).append("_").append(i)
               .append(",Plate1,A").append(String.format("%02d", i))
               .append(",N701,TAAGGCGA")
               .append(",S501,TAGATCGC")
               .append(",Project_").append(experimentId)
               .append(",Mock sample for testing\n");
        }
        
        return csv.toString();
    }
} 