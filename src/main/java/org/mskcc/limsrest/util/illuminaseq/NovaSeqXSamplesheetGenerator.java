package org.mskcc.limsrest.util.illuminaseq;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Illumina V2 sample sheet with DRAGEN and demux settings.
 */
public class NovaSeqXSamplesheetGenerator {

    // Inner class to represent a BCLConvert sample
    public static class BclConvertSample {
        private int lane;
        private String sampleId;
        private String samplePlate;
        private String sampleWell;
        private String indexName;
        private String index1;
        private String index2;
        private String project;
        private String baitSet;
        private int mismatchesIndex1;
        private int mismatchesIndex2;

        public BclConvertSample(int lane, String sampleId, String samplePlate, String sampleWell, String indexName,
                                String index1, String index2, String project, String baitSet,
                                int mismatchesIndex1, int mismatchesIndex2) {
            this.lane = lane;
            this.sampleId = sampleId;
            this.samplePlate = samplePlate;
            this.sampleWell = sampleWell;
            this.indexName = indexName;
            this.index1 = index1;
            this.index2 = index2;
            this.project = project;
            this.baitSet = baitSet;
            this.mismatchesIndex1 = mismatchesIndex1;
            this.mismatchesIndex2 = mismatchesIndex2;
        }

        public String toCsv() {
            return lane + "," + sampleId + "," + samplePlate + "," + sampleWell + "," + index1 + "," + index2 + "," +
                    project + "," + baitSet + "," + mismatchesIndex1 + "," + mismatchesIndex2;
        }
    }

    // Inner class to represent a DragenGermline sample
    public static class DragenGermlineSample {
        private String sampleId;
        private String referenceGenomeDir;
        private String variantCallingMode;

        public DragenGermlineSample(String sampleId, String referenceGenomeDir, String variantCallingMode) {
            this.sampleId = sampleId;
            this.referenceGenomeDir = referenceGenomeDir;
            this.variantCallingMode = variantCallingMode;
        }

        public String toCsv() {
            return sampleId + "," + referenceGenomeDir + "," + variantCallingMode + ",,,";
        }
    }

    // Class properties
    private String runName;
    private int read1Cycles;
    private int read2Cycles;
    private int index1Cycles;
    private int index2Cycles;
    private String bclConvertVersion = "1.3.11";
    private String fastqCompressionFormat = "gzip";
    private String dragenSoftwareVersion = "4.3.13";
    private String dragenAppVersion = "1.3.11";
    private boolean keepFastq = false;
    private String mapAlignOutFormat = "cram";

    private List<BclConvertSample> bclConvertSamples = new ArrayList<>();
    private List<DragenGermlineSample> dragenGermlineSamples = new ArrayList<>();

    // Constructor
    public NovaSeqXSamplesheetGenerator(String runName, int read1Cycles, int read2Cycles, int index1Cycles, int index2Cycles) {
        this.runName = runName;
        this.read1Cycles = read1Cycles;
        this.read2Cycles = read2Cycles;
        this.index1Cycles = index1Cycles;
        this.index2Cycles = index2Cycles;
    }

    // Method to add BCLConvert sample
    public void addBclConvertSample(int lane, String sampleId, String samplePlate, String sampleWell, String indexName,
                                    String index1, String index2, String project, String baitSet, int mismatchesIndex1, int mismatchesIndex2) {
        bclConvertSamples.add(new BclConvertSample(lane, sampleId, samplePlate, sampleWell, indexName, index1, index2,
                project, baitSet, mismatchesIndex1, mismatchesIndex2));
    }

    // Method to add DragenGermline sample
    public void addDragenGermlineSample(String sampleId, String referenceGenomeDir, String variantCallingMode) {
        dragenGermlineSamples.add(new DragenGermlineSample(sampleId, referenceGenomeDir, variantCallingMode));
    }

    // Setters for configurations
    public void setReadCycles(int read1Cycles, int read2Cycles, int index1Cycles, int index2Cycles) {
        this.read1Cycles = read1Cycles;
        this.read2Cycles = read2Cycles;
        this.index1Cycles = index1Cycles;
        this.index2Cycles = index2Cycles;
    }

    public void setBclConvertSettings(String version, String compressionFormat) {
        this.bclConvertVersion = version;
        this.fastqCompressionFormat = compressionFormat;
    }

    public void setDragenSettings(String softwareVersion, String appVersion, boolean keepFastq, String mapAlignOutFormat) {
        this.dragenSoftwareVersion = softwareVersion;
        this.dragenAppVersion = appVersion;
        this.keepFastq = keepFastq;
        this.mapAlignOutFormat = mapAlignOutFormat;
    }

    // Generate samplesheet
    public void generateSamplesheet(String outputFilePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilePath))) {
            // Write Header section
            writer.println("[Header],,,,,,");
            writer.println("FileFormatVersion,2,,,,,");
            writer.println("RunName," + runName + ",,,,,");
            writer.println("InstrumentPlatform,NovaSeqXSeries,,,,,");
            writer.println("IndexOrientation,Forward,,,,,");
            writer.println(",,,,,,");

            // Write Reads section
            writer.println("[Reads],,,,,,");
            writer.println("Read1Cycles," + read1Cycles + ",,,,,");
            writer.println("Read2Cycles," + read2Cycles + ",,,,,");
            writer.println("Index1Cycles," + index1Cycles + ",,,,,");
            writer.println("Index2Cycles," + index2Cycles + ",,,,,");
            writer.println(",,,,,,");

            // Write BCLConvert_Settings section
            writer.println("[BCLConvert_Settings],,,,,,");
            writer.println("SoftwareVersion," + bclConvertVersion + ",,,,,");
            writer.println("FastqCompressionFormat," + fastqCompressionFormat + ",,,,,");
            writer.println(",,,,,,");

            // Write BCLConvert_Data section
            writer.println("[BCLConvert_Data],,,,,,");
            writer.println("Lane,Sample_ID,Sample_Plate,Sample_Well,Index,Index2,Sample_Project,Bait_Set,BarcodeMismatchesIndex1,BarcodeMismatchesIndex2");
            for (BclConvertSample sample : bclConvertSamples) {
                writer.println(sample.toCsv());
            }
            writer.println(",,,,,,");

            // Write DragenGermline_Settings section
            writer.println("[DragenGermline_Settings],,,,,,");
            writer.println("SoftwareVersion," + dragenSoftwareVersion + ",,,,,");
            writer.println("AppVersion," + dragenAppVersion + ",,,,,");
            writer.println("KeepFastq," + (keepFastq ? "TRUE" : "FALSE") + ",,,,,");
            writer.println("MapAlignOutFormat," + mapAlignOutFormat + ",,,,,");
            writer.println(",,,,,,");

            // Write DragenGermline_Data section
            writer.println("[DragenGermline_Data],,,,,,");
            writer.println("Sample_ID,ReferenceGenomeDir,VariantCallingMode,,,,");
            for (DragenGermlineSample sample : dragenGermlineSamples) {
                writer.println(sample.toCsv());
            }
        }
    }

    public static void main(String[] args) {
        try {
            // Create generator with run name
            NovaSeqXSamplesheetGenerator generator = new NovaSeqXSamplesheetGenerator("20250411_FAUCI2_A", 151, 151, 8, 8);

            // Add DragenGermline samples
            String referenceGenome = "hg38-alt_masked.cnv.hla.methylated_combined.rna-10-r4.0-2";
            generator.addDragenGermlineSample("XPRO_3133_T2_DNA_IGO_08822_ZD_4", referenceGenome, "None");
            generator.addDragenGermlineSample("XPRO_3200_T_DNA_IGO_08822_ZX_1", referenceGenome, "None");

            // Generate the samplesheet
            generator.generateSamplesheet("NovaSeqX_samplesheet.csv");
            System.out.println("Samplesheet generated successfully!");

        } catch (IOException e) {
            System.err.println("Error generating samplesheet: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
