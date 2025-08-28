package org.mskcc.limsrest.util;

import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class for sample sheet specific operations
 */
public class SampleSheetUtils {
    private static final Log log = LogFactory.getLog(SampleSheetUtils.class);

    /**
     * Simple barcode class to represent index sequences
     */
    public static class Barcode {
        private String index1;
        private String index2;
        
        public Barcode(String index1) {
            this.index1 = index1;
            this.index2 = null;
        }
        
        public Barcode(String index1, String index2) {
            this.index1 = index1;
            this.index2 = index2;
        }
        
        public String getIndex1() { return index1; }
        public String getIndex2() { return index2; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Barcode barcode = (Barcode) o;
            return Objects.equals(index1, barcode.index1) && Objects.equals(index2, barcode.index2);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(index1, index2);
        }
        
        @Override
        public String toString() {
            return index2 != null ? index1 + "-" + index2 : index1;
        }
    }

    /**
     * Calculate optimal barcode mismatches based on barcode sequences
     * Simplified version of the original BarcodeMismatch.determineBarcodeMismatches
     */
    public static Integer[] determineBarcodeMismatches(List<Set<Barcode>> barcodesByLane, boolean isDualBarcoded) {
        Integer[] mismatches = new Integer[2];
        
        // Default values
        mismatches[0] = 1; // index1 mismatches
        mismatches[1] = isDualBarcoded ? 1 : 0; // index2 mismatches
        
        try {
            // Calculate minimum Hamming distance for each index
            int minDistance1 = Integer.MAX_VALUE;
            int minDistance2 = Integer.MAX_VALUE;
            
            for (Set<Barcode> laneBarcodes : barcodesByLane) {
                if (laneBarcodes.isEmpty()) continue;
                
                List<Barcode> barcodeList = new ArrayList<>(laneBarcodes);
                
                // Compare all pairs of barcodes in this lane
                for (int i = 0; i < barcodeList.size(); i++) {
                    for (int j = i + 1; j < barcodeList.size(); j++) {
                        Barcode b1 = barcodeList.get(i);
                        Barcode b2 = barcodeList.get(j);
                        
                        // Calculate Hamming distance for index1
                        int distance1 = hammingDistance(b1.getIndex1(), b2.getIndex1());
                        if (distance1 < minDistance1) {
                            minDistance1 = distance1;
                        }
                        
                        // Calculate Hamming distance for index2 if dual barcoded
                        if (isDualBarcoded && b1.getIndex2() != null && b2.getIndex2() != null) {
                            int distance2 = hammingDistance(b1.getIndex2(), b2.getIndex2());
                            if (distance2 < minDistance2) {
                                minDistance2 = distance2;
                            }
                        }
                    }
                }
            }
            
            // Set mismatches based on minimum distances
            if (minDistance1 != Integer.MAX_VALUE) {
                mismatches[0] = Math.max(0, (minDistance1 - 1) / 2);
            }
            
            if (isDualBarcoded && minDistance2 != Integer.MAX_VALUE) {
                mismatches[1] = Math.max(0, (minDistance2 - 1) / 2);
            }
            
            // Ensure mismatches are within reasonable bounds
            mismatches[0] = Math.min(2, Math.max(0, mismatches[0]));
            if (isDualBarcoded) {
                mismatches[1] = Math.min(2, Math.max(0, mismatches[1]));
            }
            
        } catch (Exception e) {
            log.warn("Error calculating barcode mismatches, using defaults: " + e.getMessage());
            mismatches[0] = 1;
            mismatches[1] = isDualBarcoded ? 1 : 0;
        }
        
        return mismatches;
    }

    /**
     * Calculate Hamming distance between two strings
     */
    private static int hammingDistance(String s1, String s2) {
        if (s1 == null || s2 == null) return Integer.MAX_VALUE;
        if (s1.length() != s2.length()) return Integer.MAX_VALUE;
        
        int distance = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                distance++;
            }
        }
        return distance;
    }

    /**
     * Check if a barcode sequence represents 10x Genomics format
     */
    public static boolean is10xGenomics(String indexTag) {
        if (indexTag == null) return false;
        String[] parts = indexTag.split("-");
        return parts.length == 3;
    }

    /**
     * Check if a barcode sequence is dual barcoded (contains hyphen but not 10x)
     */
    public static boolean isDualBarcode(String indexTag) {
        if (indexTag == null) return false;
        String[] parts = indexTag.split("-");
        return parts.length == 2;
    }

    /**
     * Extract base IGO sample ID (remove aliquot suffixes)
     */
    public static String baseIgoSampleId(String igoId) {
        if (igoId == null) return null;
        if (igoId.startsWith("Pool-")) {
            log.debug("No IGO ID for Pool " + igoId);
            return null;
        }
        
        String request = requestFromIgoId(igoId);
        int indexEnd = igoId.indexOf('_', request.length() + 1);
        if (indexEnd == -1) {
            return igoId;
        } else {
            return igoId.substring(0, indexEnd);
        }
    }

    /**
     * Extract request ID from IGO sample ID
     */
    public static String requestFromIgoId(String igoId) {
        if (igoId == null) return null;
        return igoId.replaceAll("_[0-9]+", "");
    }

    /**
     * Validate IGO sample ID format
     */
    public static boolean isValidIGOSampleId(String igoId) {
        if (igoId == null) return false;
        return igoId.matches("\\d\\d\\d\\d\\d(_[A-Z]*)?(_[0-9]*)+");
    }

    /**
     * Get padding sequences for different barcode adapters and machine types
     */
    public static Map<String, String> getPaddingMap(String machineType) {
        Map<String, String> paddingMap = new HashMap<>();
        
        // Illumina TruSeq adapter sequences - simplified set
        String[] truSeqIds = {"TS1", "TS2", "TS3", "TS4", "TS5", "TS6", "TS7", "TS8", "TS9", "TS10", "TS11", "TS12"};
        for (String id : truSeqIds) {
            paddingMap.put(id, "AT");
        }
        
        // Special cases
        paddingMap.put("TS13", "CA");
        paddingMap.put("TS14", "GT");
        paddingMap.put("TS15", "GA");
        paddingMap.put("TS16", "CG");
        paddingMap.put("TS18", "AC");
        paddingMap.put("TS19", "CG");
        paddingMap.put("TS20", "TT");
        paddingMap.put("TS21", "GA");
        paddingMap.put("TS22", "TA");
        paddingMap.put("TS23", "AT");
        paddingMap.put("TS25", "AT");
        paddingMap.put("TS27", "TT");
        
        // RPI series
        for (int i = 1; i < 49; i++) {
            paddingMap.put("RPI" + i, "AT");
            paddingMap.put("IDT-TS" + i, "AT");
        }
        
        // NEBNext series
        for (int i = 1; i < 13; i++) {
            paddingMap.put("NEBNext" + i, "AT");
        }
        paddingMap.put("NEBNext13", "CA");
        paddingMap.put("NEBNext14", "GT");
        paddingMap.put("NEBNext15", "GA");
        paddingMap.put("NEBNext16", "AT");
        paddingMap.put("NEBNext18", "AC");
        paddingMap.put("NEBNext19", "CG");
        paddingMap.put("NEBNext20", "TT");
        paddingMap.put("NEBNext21", "GA");
        paddingMap.put("NEBNext22", "TA");
        paddingMap.put("NEBNext23", "AT");
        paddingMap.put("NEBNext25", "AT");
        paddingMap.put("NEBNext27", "TT");
        
        // 10X CITE-seq
        for (int i = 1; i <= 9; i++) {
            paddingMap.put("CITE_" + i, "AT");
        }
        
        // Apply reverse complement for certain machine types
        if (!"NextSeq".equals(machineType) && !"HiSeq4000".equals(machineType) && !"NovaSeq".equals(machineType)) {
            Map<String, String> reversedMap = new HashMap<>();
            for (Map.Entry<String, String> entry : paddingMap.entrySet()) {
                reversedMap.put(entry.getKey(), reverseComplement(entry.getValue()));
            }
            return reversedMap;
        }
        
        return paddingMap;
    }

    /**
     * Generate reverse complement of a DNA sequence
     */
    public static String reverseComplement(String sequence) {
        if (sequence == null) return null;
        
        StringBuilder revComp = new StringBuilder();
        for (int i = sequence.length() - 1; i >= 0; i--) {
            switch (sequence.charAt(i)) {
                case 'A': revComp.append('T'); break;
                case 'C': revComp.append('G'); break;
                case 'G': revComp.append('C'); break;
                case 'T': revComp.append('A'); break;
                default: revComp.append(sequence.charAt(i)); break;
            }
        }
        return revComp.toString();
    }

    /**
     * Format field for CSV output (handle commas, quotes, newlines)
     */
    public static String formatCsvField(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
} 