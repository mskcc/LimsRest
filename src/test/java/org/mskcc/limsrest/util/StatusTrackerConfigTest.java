package org.mskcc.limsrest.util;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mskcc.limsrest.ConnectionLIMS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mskcc.limsrest.util.StatusTrackerConfig.getLimsStageNameFromStatus;

public class StatusTrackerConfigTest {
    ConnectionLIMS conn;

    @Before
    public void setup() {
        // Connection needed to query the existing tango workflow manager
        this.conn = new ConnectionLIMS("tango.mskcc.org", 1099, "fe74d8e1-c94b-4002-a04c-eb5c492704ba", "test-runner", "password1");
    }

    @After
    public void tearDown() {
        this.conn.close();
    }

    /**
     * Helper method used to test all stage extraction from input statuses
     *
     * @param testSamples
     */
    private void assertSamples(List<SampleStageTester> testSamples) {
        for (SampleStageTester test : testSamples) {
            String actualStage = getLimsStageNameFromStatus(this.conn, test.status);
            Assert.assertEquals(String.format("FAILED: Status %s -> Stage %s (Actual: %s)", test.status, test.expectedStage, actualStage), actualStage, test.expectedStage);
        }
    }

    @Test
    public void getLimsStageNameFromStatusTest_sampleQC() {
        List<SampleStageTester> testSamples = new ArrayList<>(Arrays.asList(
                // STAGE_SAMPLE_QC
                new SampleStageTester("Completed - Quality Control", "05837_1", "02756_B", "Sample QC"),
                new SampleStageTester("Failed - Quality Control", "05546_2", "", "Sample QC"),
                new SampleStageTester("In Process - Quality Control", "05426_2", "", "Sample QC"),
                new SampleStageTester("Ready for - Quality Control", "Neg-1924", "", "Sample QC")
        ));
        assertSamples(testSamples);
    }

    @Test
    public void getLimsStageNameFromStatusTest_returnedToUser() {
        List<SampleStageTester> testSamples = new ArrayList<>(Arrays.asList(
                new SampleStageTester("Completed - Return Samples To User", "NOT_IN_TANGO", "NOT_IN_TANGO", "Returned To User")
        ));
        assertSamples(testSamples);
    }

    @Test
    public void getLimsStageNameFromStatusTest_extraction() {
        List<SampleStageTester> testSamples = new ArrayList<>(Arrays.asList(
                new SampleStageTester("Completed - cfDNA Extraction", "06302_Q_198", "06302_Q", "Nucleic Acid Extraction"),
                new SampleStageTester("Completed - Covid19 Testing RNA Extraction", "10858_1663_1", "10858", "Nucleic Acid Extraction"),
                new SampleStageTester("Completed - DNA Extraction", "06795_5", "04430_AA", "Nucleic Acid Extraction"),
                new SampleStageTester("Completed - DNA Extraction From Blood", "07453_C_26", "05469_Z", "Nucleic Acid Extraction"),
                new SampleStageTester("Completed - DNA/RNA Simultaneous Extraction", "05540_16", "05130_B", "Nucleic Acid Extraction"),
                new SampleStageTester("In Process - DNA Extraction", "06000_DP_1", "06000_DP", "Nucleic Acid Extraction"),
                new SampleStageTester("In Process - DNA/RNA Simultaneous Extraction", "05486_28", "05486", "Nucleic Acid Extraction"),
                new SampleStageTester("Ready for - DNA Extraction", "07973_EW_6", "05257_BV", "Nucleic Acid Extraction"),
                new SampleStageTester("Ready for - DNA/RNA Simultaneous Extraction", "09687_AQ_1", "09687_AQ", "Nucleic Acid Extraction"),
                new SampleStageTester("Completed - RNA Extraction", "06534_C_4", "", "Nucleic Acid Extraction"),
                new SampleStageTester("Failed - Blood Extraction", "05257_B_8", "05257_B", "Nucleic Acid Extraction"),
                new SampleStageTester("In Process - Covid19 Testing RNA Extraction", "10858_10493", "10858", "Nucleic Acid Extraction")
        ));
        assertSamples(testSamples);
    }

    @Test
    public void getLimsStageNameFromStatusTest_libraryCapture() {
        List<SampleStageTester> testSamples = new ArrayList<>(Arrays.asList(
                // STAGE_LIBRARY_CAPTURE,
                new SampleStageTester("Completed - Agilent Capture from KAPA Library", "05622_C_10_1_1_1", "04430_U", "Library Capture"),
                new SampleStageTester("Completed - Capture from KAPA Library", "04540_H_2", "", "Library Capture"),
                new SampleStageTester("Completed - Pooling for Whole Exome Capture", "05605_B_1_1_1_1", "04525_J", "Library Capture"),
                new SampleStageTester("Failed - Agilent Capture from KAPA Library", "05428_O_10_1_1", "05428_O", "Library Capture"),
                new SampleStageTester("In Process - Agilent Capture from KAPA Library", "06049_O_77", "06049_O", "Library Capture"),
                new SampleStageTester("In Process - Capture from KAPA Library", "10786_D_38", "10786_D", "Library Capture")
        ));
        assertSamples(testSamples);
    }

    @Test
    public void getLimsStageNameFromStatusTest_libraryPrep() {
        List<SampleStageTester> testSamples = new ArrayList<>(Arrays.asList(
                new SampleStageTester("Completed - 10X Genomics cDNA Preparation", "05667_CB_1", "05667_CB", "Library Preparation"),
                new SampleStageTester("Completed - 10X Genomics Library Preparation", "05667_CB_1_1", "05667_CB", "Library Preparation"),
                new SampleStageTester("Completed - AmpliSeq Enrichment", "06605_F_1", "05604_S", "Library Preparation"),
                new SampleStageTester("Completed - AmpliSeq Library Prep", "06605_F_1_1", "05604_S", "Library Preparation"),
                new SampleStageTester("Completed - Archer Library Preparation Experiment", "07616_C_1_1", "", "Library Preparation"),
                new SampleStageTester("Completed - ATAC Library Prep", "06865_1", "06125_K", "Library Preparation"),
                new SampleStageTester("Completed - CloneTech SMARTer RNA Amplification", "06639_C_2", "", "Library Preparation"),
                new SampleStageTester("Completed - DLP Library Preparation", "Pool-06000_GP-Tube3", "06000_GP", "Library Preparation"),
                new SampleStageTester("Completed - EPIC Library Preparation", "06287_BG_10_1_1", "", "Library Preparation"),
                new SampleStageTester("Completed - Generic Library Preparation", "09093_1_1", "06000_FD", "Library Preparation"),
                new SampleStageTester("Completed - Generic Normalization Plate Setup", "09083_1", "06000_FD", "Library Preparation"),
                new SampleStageTester("Completed - ImmunoSEQ Library Preparation", "07340_B_1", "", "Library Preparation"),
                new SampleStageTester("Completed - KAPA Library Preparation", "05829_C_1_1_1_1", "", "Library Preparation"),
                new SampleStageTester("Completed - Library Clean Up", "06777_10_1_1", "04622_C", "Library Preparation"),
                new SampleStageTester("Completed - Library Clean Up/Size Selection", "07238_1_1_1_1_1", "", "Library Preparation"),
                new SampleStageTester("Completed - Methyl Miner", "05813_1", "05813", "Library Preparation"),
                new SampleStageTester("Completed - MSK Access Library Preparation", "06302_R_1_2", "", "Library Preparation"),
                new SampleStageTester("Completed - MSK Access Normalization Plate Setup", "06302_R_138", "05257_BX", "Library Preparation"),
                new SampleStageTester("Completed - Nextera Library Prep", "10091_100_1", "10091", "Library Preparation"),
                new SampleStageTester("Completed - NexteraXT Library Prep", "06712_C_2_1", "06000_DQ", "Library Preparation"),
                new SampleStageTester("Completed - Normalization Plate Setup", "06048_B_6_1", "", "Library Preparation"),
                new SampleStageTester("Completed - PCR Cycle Re-Amplification", "05794_C_1_1", "", "Library Preparation"),
                new SampleStageTester("Completed - PCR Free Whole Genome Library Preparation", "08368_25_1", "05732_AH", "Library Preparation"),
                new SampleStageTester("Completed - PolyA RNA Normalization Plate Setup", "06389_B_1", "04907_D", "Library Preparation"),
                new SampleStageTester("Completed - Ribodeplete RNA Normalization Plate Setup", "07008_AF_1", "04592_F", "Library Preparation"),
                new SampleStageTester("Completed - RNA Normalization Plate Set up", "08858_F_1", "", "Library Preparation"),
                new SampleStageTester("Completed - Sample Cleanup/Size Selection", "05667_V_2", "", "Library Preparation"),
                new SampleStageTester("Completed - Sample Shearing", "05213_I_1_1_1", "", "Library Preparation"),
                new SampleStageTester("Completed - Shearing Plate Setup", "05304_E_10_1_2", "04540_E", "Library Preparation"),
                new SampleStageTester("Completed - Smarter RNA Normalization Plate Setup", "08185_B_5", "05737_X", "Library Preparation"),
                new SampleStageTester("Completed - TruSeqRNA Fusion Discovery", "05491_B_11", "04835_D", "Library Preparation"),
                new SampleStageTester("Completed - TruSeqRNA Poly-A cDNA Preparation", "08024_1_1", "", "Library Preparation"),
                new SampleStageTester("Completed - TruSeqRNA Poly-A Library Preparation", "05018_B_10", "03498_D", "Library Preparation"),
                new SampleStageTester("Completed - TruSeqRNA RiboDeplete cDNA Preparation", "07008_Y_1", "", "Library Preparation"),
                new SampleStageTester("Completed - TruSeqRNA RiboDeplete Library Prep", "04969_D_2", "04592_C", "Library Preparation"),
                new SampleStageTester("Failed - KAPA Library Preparation", "06159_88_1_1", "05428_AH", "Library Preparation"),
                new SampleStageTester("In Process - 10X Genomics Library Preparation", "09955_E_1_1", "09955_E", "Library Preparation"),
                new SampleStageTester("In Process - AmpliSeq Enrichment", "04966_L_1", "04966_L", "Library Preparation"),
                new SampleStageTester("In Process - AmpliSeq Library Prep", "07973_BW_1_2_1", "07973_BW", "Library Preparation"),
                new SampleStageTester("In Process - CloneTech SMARTer RNA Amplification", "07615_L_1", "07615_L", "Library Preparation"),
                new SampleStageTester("In Process - Generic Library Preparation", "09364_B_8", "09364_B", "Library Preparation"),
                new SampleStageTester("In Process - KAPA Library Preparation", "04298_C_1_1_1", "", "Library Preparation"),
                new SampleStageTester("In Process - Normalization Plate Setup", "06477_G_1_2", "06477_G", "Library Preparation"),
                new SampleStageTester("In Process - PCR Cycle Re-Amplification", "07570_10_1_1", "07570", "Library Preparation"),
                new SampleStageTester("In Process - TruSeqRNA Library Preparation from cDNA", "05667_CO_10_1_1_1", "", "Library Preparation"),
                new SampleStageTester("Ready for - 10X Genomics cDNA Preparation", "09743_L_1", "09743_L", "Library Preparation"),
                new SampleStageTester("Ready for - Capture from KAPA Library", "07871_X_28", "07871_X", "Library Capture"),
                new SampleStageTester("Ready for - CloneTech SMARTer RNA Amplification", "10223_385", "", "Library Preparation"),
                new SampleStageTester("Ready for - DLP Library Preparation", "06000_GY_1", "06000_GY", "Library Preparation"),
                new SampleStageTester("Ready for - Generic Library Preparation", "09279_466", "09279", "Library Preparation"),
                new SampleStageTester("Ready for - KAPA Library Preparation", "04864_D_10_1", "04864_D", "Library Preparation"),
                new SampleStageTester("Ready for - MSK Access Normalization Plate Setup", "09324_C_31", "09324_C", "Library Preparation"),
                new SampleStageTester("Ready for - Normalization Plate Setup", "06477_B_357", "", "Library Preparation"),
                new SampleStageTester("Ready for - PCR Free Whole Genome Library Preparation", "08822_GL_3_1_2", "08822_GL", "Library Preparation"),
                new SampleStageTester("Ready for - RNA Normalization Plate Set up", "08822_GV_4_1", "08822_GV", "Library Preparation"),
                new SampleStageTester("Ready for - TruSeqRNA Library Preparation from cDNA", "Neg-1302_1", "", "Library Preparation"),
                new SampleStageTester("Ready for - TruSeqRNA RiboDeplete Library Prep", "06961_6", "06717_E", "Library Preparation")
        ));
        assertSamples(testSamples);
    }

    @Test
    public void getLimsStageNameFromStatusTest_sequencing() {
        List<SampleStageTester> testSamples = new ArrayList<>(Arrays.asList(
                new SampleStageTester("Completed - Illumina Sequencing", "Pool-06604_D-Tube1_2_2_1_1", "06604_D", "Sequencing"),
                new SampleStageTester("Completed - Illumina Sequencing Planning/Denaturing", "Pool-06604_D-Tube1_2_2", "06604_D", "Sequencing"),
                new SampleStageTester("Completed - Illumina Sequencing Setup", "Pool-06604_D-Tube1_2_2_1", "06604_D", "Sequencing"),
                new SampleStageTester("Completed - Illumina Sequencing", "Pool-05363-1081", "", "Sequencing"),
                new SampleStageTester("Completed - Illumina Sequencing Analysis", "Pool-05338_B-1080_4", "", "Sequencing"),
                new SampleStageTester("Completed - Illumina Sequencing Planning/Denaturing", "Pool-05338_B-1080_2", "", "Sequencing"),
                new SampleStageTester("Completed - Illumina Sequencing Setup", "06712_B_2_1", "04298_E", "Sequencing"),
                new SampleStageTester("Completed - Normalization of Pooled Libraries", "05423_1", "", "Sequencing"),
                new SampleStageTester("Completed - Pooling of Sample Libraries by Volume", "07810_1_1_1_1", "", "Sequencing"),
                new SampleStageTester("Completed - Pooling of Sample Libraries for Sequencing", "06420_C_13_1", "", "Sequencing"),
                new SampleStageTester("In Process - Illumina Sequencing", "06287_Q_8_1_1_1", "04430_AO,10431,05257_CD,10431_B,07973_ES", "Sequencing"),
                new SampleStageTester("In Process - Normalization of Pooled Libraries", "Pool-05491_C-Tube1_1", "05491_C", "Sequencing"),
                new SampleStageTester("In Process - Pooling of Sample Libraries for Sequencing", "07149_D_1_1_1", "06179_X", "Sequencing"),
                new SampleStageTester("Ready for - Illumina Sequencing", "Pool-10848_B-B1_1", "", "Sequencing"),
                new SampleStageTester("Ready for - Pooling of Sample Libraries by Volume", "07931_2_1_1", "07931", "Sequencing"),
                new SampleStageTester("Ready for - Pooling of Sample Libraries for Sequencing", "09259_H_101_2", "06287_BG", "Sequencing")
        ));
        assertSamples(testSamples);
    }

    @Test
    public void getLimsStageNameFromStatusTest_libraryQC() {
        List<SampleStageTester> testSamples = new ArrayList<>(Arrays.asList(
                new SampleStageTester("Completed - Library/Pool Quality Control", "06822_11", "", "Library QC"),
                new SampleStageTester("Failed - Library/Pool Quality Control", "06477_D_532_1_1", "", "Library QC"),
                new SampleStageTester("In Process - Library/Pool Quality Control", "Pool-06645-Tube1_3_1", "", "Library QC"),
                new SampleStageTester("Ready for - Library/Pool Quality Control", "06819_F_13", "", "Library QC")
        ));
        assertSamples(testSamples);
    }

    @Test
    public void getLimsStageNameFromStatusTest_covid19() {
        List<SampleStageTester> testSamples = new ArrayList<>(Arrays.asList(
                new SampleStageTester("Completed - COVID-19 Testing QPCR", "10858_62_1_1_1", "", "COVID-19 Assay"),
                new SampleStageTester("Completed - Covid19 Testing", "10858_2", "", "COVID-19 Assay"),
                new SampleStageTester("In Process - COVID-19 Testing QPCR", "10858_10411_1_1_1", "", "COVID-19 Assay"),
                new SampleStageTester("In Process - Covid19 Testing", "10858_1243_1", "10858", "COVID-19 Assay")
        ));
        assertSamples(testSamples);
    }

    @Test
    public void getLimsStageNameFromStatusTest_digitalPCR() {
        List<SampleStageTester> testSamples = new ArrayList<>(Arrays.asList(
                new SampleStageTester("Completed - Digital Droplet PCR", "05463_W_6", "04609_R", "Digital PCR"),
                new SampleStageTester("Completed - Digital PCR", "06390_3", "06390", "Digital PCR"),
                new SampleStageTester("In Process - Digital Droplet PCR", "07788_1_1", "07788", "Digital PCR"),
                new SampleStageTester("Ready for - Digital Droplet PCR", "09802_C_14", "08039_CH", "Digital PCR")
        ));
        assertSamples(testSamples);
    }

    @Test
    public void getLimsStageNameFromStatusTest_addingCMOInformation() {
        List<SampleStageTester> testSamples = new ArrayList<>(Arrays.asList(
                new SampleStageTester("Completed - Adding CMO Information", "05200_J_1", "04430_H", "Adding CMO Information"),
                new SampleStageTester("In Process - Adding CMO Information", "06575_25", "06575", "Adding CMO Information")
        ));
        assertSamples(testSamples);
    }

    @Test
    public void getLimsStageNameFromStatusTest_pendingUserDecision() {
        List<SampleStageTester> testSamples = new ArrayList<>(Arrays.asList(
                new SampleStageTester("Completed - Pending User Decision", "04926_L_29", "04926_L", "Pending User Decision"),
                new SampleStageTester("Failed - Pending User Decision", "07431_2", "", "Pending User Decision"),
                new SampleStageTester("In Process - Pending User Decision", "06678_C_19_1_2", "06678_C", "Pending User Decision"),
                new SampleStageTester("Ready for - Pending User Decision", "10653_D_1_1_1_1", "09756_D", "Pending User Decision")
        ));
        assertSamples(testSamples);
    }

    @Test
    public void getLimsStageNameFromStatusTest_batchPlanning() {
        List<SampleStageTester> testSamples = new ArrayList<>(Arrays.asList(
                new SampleStageTester("Completed - Batch Planning", "04926_P_5", "04926_P", "Batch Planning")
        ));
        assertSamples(testSamples);
    }

    @Test
    public void getLimsStageNameFromStatusTest_createSampleAliquots() {
        List<SampleStageTester> testSamples = new ArrayList<>(Arrays.asList(
                new SampleStageTester("Completed - Create Sample Aliquots", "07624_D_75", "07624_D", "Create Sample Aliquots")
        ));
        assertSamples(testSamples);
    }

    @Test
    public void getLimsStageNameFromStatusTest_nanoString() {
        List<SampleStageTester> testSamples = new ArrayList<>(Arrays.asList(
                new SampleStageTester("Completed - NanoString", "06260_B_9_2", "05654_H", "Nano string"),
                new SampleStageTester("Ready for - NanoString", "10761_1", "10761", "Nano string"),
                new SampleStageTester("In Process - NanoString", "06722_J_19", "06722_J", "Nano string")
        ));
        assertSamples(testSamples);
    }

    @Test
    public void getLimsStageNameFromStatusTest_str() {
        List<SampleStageTester> testSamples = new ArrayList<>(Arrays.asList(
                new SampleStageTester("Completed - STR PCR Human", "08822_GL_1_1_1_1", "05971_AI", "STR PCR"),
                new SampleStageTester("Completed - STR/Fragment Analysis Profiling", "10347_B_1", "", "STR Analysis"),
                new SampleStageTester("Ready for - STR/Fragment Analysis Profiling", "Neg-4885", "", "STR Analysis")
        ));
        assertSamples(testSamples);
    }

    @Test
    public void getLimsStageNameFromStatusTest_snpFingerprinting() {
        List<SampleStageTester> testSamples = new ArrayList<>(Arrays.asList(
                new SampleStageTester("In Process - SNP Fingerprinting", "06938_K_10_1_1", "06938_K", "SNP Fingerprinting"),
                new SampleStageTester("Completed - SNP Fingerprinting", "07037_M_1_1", "04430_X", "SNP Fingerprinting")
        ));
        assertSamples(testSamples);
    }

    @Test
    public void getLimsStageNameFromStatusTest_sampleReceipt() {
        List<SampleStageTester> testSamples = new ArrayList<>(Arrays.asList(
                new SampleStageTester("Completed - Sample Combination for Pooled Submissions", "05332_7", "05114_B", "Sample Receipt")
        ));
        assertSamples(testSamples);
    }

    @Test
    public void getLimsStageNameFromStatusTest_sampleReplacementContamination() {
        List<SampleStageTester> testSamples = new ArrayList<>(Arrays.asList(
                new SampleStageTester("Completed - Sample Replacement/Combination", "06265_H_1", "04971_L", "Sample Replacement/Combination")
        ));
        assertSamples(testSamples);
    }

    @Test
    public void getLimsStageNameFromStatusTest_transferTubeSamples() {
        List<SampleStageTester> testSamples = new ArrayList<>(Arrays.asList(
                new SampleStageTester("Completed - Transfer Tube Samples to Plates", "05445_D_1", "04298_D", "Transfer Tube Samples to Plates"),
                new SampleStageTester("Failed - Transfer Tube Samples to Plates", "04540_D_1_1", "04540_D", "Transfer Tube Samples to Plates")
        ));
        assertSamples(testSamples);
    }

    @Test
    public void getLimsStageNameFromStatusTest_pathology() {
        List<SampleStageTester> testSamples = new ArrayList<>(Arrays.asList(
                new SampleStageTester("Failed - Pathology", "04644_K_22", "04430_X", "Pathology"),
                new SampleStageTester("Ready for - Pathology", "09687_AP_1", "05257_BU", "Pathology")
        ));
        assertSamples(testSamples);
    }

    private class SampleStageTester {
        String status;
        String sampleId;
        String requestId;
        String expectedStage;

        SampleStageTester(String status, String sampleId, String requestId, String expectedStage) {
            this.status = status;
            this.sampleId = sampleId;
            this.requestId = requestId;
            this.expectedStage = expectedStage;
        }
    }


    /**
     * BELOW ARE ALL THE BAD EXEMPLAR STATUSES EXTRACTED FROM TANGO.
     * Included here for temporary reference. Will be deleted
     * TODO - Delete
     */
    /*
    // MANUALLY-ENTERED (Non Standard)
    // terminating
    new SampleStageTester("Do Not Use Again - Speak to LIMS Team", "07973_BK_2", "07973_BK", ""),
    new SampleStageTester("Storage Only", "05347_C_38", "04500_F", ""),
    new SampleStageTester("Turned off by Group Leader", "05440_12", "", ""),
    new SampleStageTester("Turned off by Group Leader - Placeholder", "09687_AC_2", "09687_AC", ""),
    new SampleStageTester("Turned off by Group Leader, Request Canceled", "06230_H_1", "06230_H", ""),
    new SampleStageTester("Discarded", "05238_C_5", "", ""),
    new SampleStageTester("Failed - Reprocess", "05957_1_1_1_1", "", ""),
    new SampleStageTester("In Long-term Storage", "06717_K_6_1_1", "06101_B", ""),
    new SampleStageTester("Sample Potentionally Lost", "06477_P_293", "06477_P", ""),
    new SampleStageTester("Sample Information Transmitted to LIMS", "Pool-1202_1_1_1_1", "", ""),

    // non-terminating
    new SampleStageTester("Ready for - KAPA Library - Manual Preparation", "05460_3", "05460", "Library Preparation"),
    new SampleStageTester("Ready for - TruSeqRNA Manual Library Preparation", "04996_E_4", "04996_E", "Library Preparation"),
    new SampleStageTester("Completed - DNA Extraction - Automatic", "06866_23", "06866", "Nucleic Acid Extraction"),
    new SampleStageTester("Completed - DNA Extraction - Manual", "04773_G_5", "04773_G", "Nucleic Acid Extraction"),
    new SampleStageTester("Completed 5X - PCR Whole Genome Library QC", "08368_25_1_1_1_1_1", "08368", "Library QC"),
    new SampleStageTester("STR", "07428_AF_1_2", "07428_AF", "STR"),
    new SampleStageTester("Completed - KAPA Automation - Library Set Up", "05367_B_1_1", "", "Library Preparation"),
    new SampleStageTester("Completed - KAPA Library - Manual Preparation", "05478_2", "04657_C", "Library Preparation"),
    new SampleStageTester("Completed - KAPA Library Prep", "04998_F_1_1", "04919_G", "Library Preparation"),
    new SampleStageTester("Completed - Library Quality Control", "05719_1_1", "05211_B", "Library QC"),
    new SampleStageTester("Completed - TruSeqRNA PolyA Library Prep", "06830_4", "06830", "Library Preparation"),
    new SampleStageTester("Completed - TruSeqRNA smRNA Library Prep", "06679_4", "05226_E", "Library Preparation"),
    new SampleStageTester("Completed WGS- 5X PCR Library Preparation", "08389_10_1_1_1_1_1", "08389", "Library Preparation"),
    new SampleStageTester("Completed - KAPA mRNA Stranded Sequencing", "05796_8", "05539", "Sequencing"),
    new SampleStageTester("Completed - Post-Extraction Hold", "05763_2", "04430_J", "Nucleic Acid Extraction"),
    new SampleStageTester("In Process - TruSeqRNA Manual Library Preparation", "05563_2", "05450_B", "Library Preparation"),
    new SampleStageTester("Failed - KAPA Library Prep", "06604_D_3_1_1_1", "06604_D", "Library Preparation"),
    new SampleStageTester("Failed - Library Quality Control", "05834_2", "05538_K", "Library QC"),
    new SampleStageTester("Failed - Pooling of Sample Libraries", "Pool-06575-Tube4_1", "06575", "Library Preparation"),
    new SampleStageTester("Failed - Quality Control for Downstream Application", "06146_B_19", "06146_B", "Library QC"),
    new SampleStageTester("In Process - Quality Control for Downstream Application", "05433_B_4", "05433_B", "Library QC"),
    new SampleStageTester("Failed - TruSeqRNA Poly-A Library Prep", "06851_C_2_2_1_1", "06851_C", "Library Preparation"),
    new SampleStageTester("In Process - DNA Extraction - Manual", "05065_C_15", "05065_C", "Nucleic Acid Extraction"),
    new SampleStageTester("Completed - Hold for Decision or New Request", "06902_4_1", "05257_AK", "Pending User Decision"),
    new SampleStageTester("Failed - KAPA Automation - Library Set Up", "05509_B_13_1_1_1", "", "Library Preparation"),

    // NOT SURE
    new SampleStageTester("Completed - Capture - Hybridization", "05245_B_16_1_1_1", "", ""),
    new SampleStageTester("Completed - CloneTech SMARTer Amplification", "05501_2", "04996_F", ""),
    new SampleStageTester("Completed - KAPA Automation - Normalization", "05367_B_1", "04430_F", ""),
    new SampleStageTester("Completed - MSK Access Capture - Hybridization", "06000_FD_10_1_1", "05257_BX", ""),
    new SampleStageTester("Completed - Pooling & Normalization", "05422_1", "", ""),
    new SampleStageTester("Completed - Pooling of Sample Libraries", "05440_11", "02756_B", ""),
    new SampleStageTester("Completed - Post-Workflow Hold", "05951_B_7", "05951_B", ""),
    new SampleStageTester("Completed - RNA Isolation - Manual", "05588_6", "05588", ""),
    new SampleStageTester("Completed - RNA Normalization", "06575_W_6", "05740_G", ""),
    new SampleStageTester("Completed - TruSeqPolyA - RNA to cDNA", "09245_D_10_1_1", "", ""),
    new SampleStageTester("Completed WGS 5c-PCR Lib", "08270_B_10_2_1_1_1_1", "08270_B", ""),
    new SampleStageTester("Failed - Capture - Hybridization", "Pool-05367_F-Tube4_1", "05367_F", ""),
    new SampleStageTester("Failed - CloneTech SMARTer Amplification", "06301_F_3_1_1", "06301_F", ""),
    new SampleStageTester("Failed - Completed", "Pool-1235", "", ""),
    new SampleStageTester("Failed - RNA Isolation - Manual", "05545_J_33", "05545_J", ""),
    new SampleStageTester("In Process - Capture - Hybridization", "05513_1", "", ""),
    new SampleStageTester("In Process - Covid19 Testing QPCR", "10858_62_1_1_1_4", "", ""),
    new SampleStageTester("In Process - MSK Access Capture - Hybridization", "06302_R_117_1_1", "06302_R", ""),
    new SampleStageTester("In Process - Pooling & Normalization", "05393_B_3", "", ""),
    new SampleStageTester("In Process - Pooling of Sample Libraries", "05456_5_1_1_1_1", "05456", ""),
    new SampleStageTester("In Process - Post-Extraction Hold", "04430_J_1", "04430_J", ""),
    new SampleStageTester("In Process - Post-Workflow Hold", "06019_40", "06019", ""),
    new SampleStageTester("In Process - Sample Replacement", "05397_23", "05397", ""),
    new SampleStageTester("Microarray", "05581_1", "05581", ""),
    new SampleStageTester("Ready for - Capture - Hybridization", "05851_H_6_2_1_1", "05851_H", ""),
    new SampleStageTester("Ready for - KAPA Automation - Normalization", "04931_C_10", "04931_C", ""),
    new SampleStageTester("Ready for - MSK Access Capture - Hybridization", "06302_AJ_153_1_1", "", ""),
    new SampleStageTester("Ready for - Sample Replacement", "06575_15", "06575", "")
    */

    /*
    // DEPRECATED WORKFLOW
    new SampleStageTester("Completed - Agilent SureSelectXT Exome Hybridized Library Enrichment", "05613_1_1_1_1", "04682_D", ""),
    new SampleStageTester("Completed - Agilent SureSelectXT Exome Library Amplification", "04468_H_1_1", "04468_H", ""),
    new SampleStageTester("Completed - Agilent SureSelectXT Exome Library Hybridization", "05409_B_1_1_1", "04682_D", ""),
    new SampleStageTester("Completed - Agilent SureSelectXT Exome Library Preparation", "04468_H_27", "04468_H", ""),
    new SampleStageTester("In Process - Agilent SureSelectXT Exome Hybridized Library Enrichment", "05435_3_1_1_1", "05433_C", ""),
    new SampleStageTester("In Process - Agilent SureSelectXT Exome Library Amplification", "04468_H_1_1_1", "04468_H", ""),
    */

    /*
    // TOO SPECIFIC
    new SampleStageTester("Returned to User IGO-013921", "06301_B_18", "06301_B", ""),
    new SampleStageTester("Returned to User IGO-013922", "06301_D_26", "06301_D", ""),
    new SampleStageTester("Returned to User IGO-013923", "06301_E_18", "06301_E", ""),
    new SampleStageTester("Returned to User IGO-013925", "06301_I_1_1", "06301_I", ""),
    new SampleStageTester("Shipped to Illumina_IGO-016220", "09253_O_1_1", "09253_O", ""),
    new SampleStageTester("Shipped_to_Illumina_IGO-016220", "09253_P_10_1", "09253_P", ""),
    new SampleStageTester("TXF to 05022_C", "04758_C_11", "04758_C", ""),
     */

    /*
    // TYPOS
    new SampleStageTester("Completed - Illumina Sequncing", "05395_1_1_1_1_1", "05395", ""),
    new SampleStageTester("Completed- Illumina Sequencing Setup", "Pool-06260_H-06717_G-06907_F-Tube1_1_1", "06907_F,06260_H,06717_G", "Sequencing"),
    */

}
