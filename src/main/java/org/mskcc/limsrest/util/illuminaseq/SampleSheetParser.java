package org.mskcc.limsrest.util.illuminaseq;

import java.util.*;
import java.io.*;

public class SampleSheetParser {

    public static List<SampleData> parseSampleSheet(String input) throws IOException {
        List<SampleData> samples = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new StringReader(input));
        String line;
        boolean inDataSection = false;
        String[] headers = null;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("[Data]")) {
                inDataSection = true;
                // Skip next line, which is empty
                reader.readLine();
                // Header line
                headers = reader.readLine().split(",", -1);
                continue;
            }

            if (inDataSection && !line.isEmpty()) {
                String[] fields = line.split(",", -1);
                if (fields.length < headers.length) {
                    System.err.println("Skipping line: " + line);
                    continue; // Skip malformed lines
                }

                SampleData sample = new SampleData();
                sample.lane = fields[0];
                sample.sampleId = fields[1];
                sample.samplePlate = fields[2];
                sample.sampleWell = fields[3];
                sample.i7IndexId = fields[4];
                sample.index = fields[5];
                sample.index2 = fields[6];
                sample.sampleProject = fields[7];
                sample.baitSet = fields[8];

                samples.add(sample);
            }
        }

        return samples;
    }

    public static void main(String[] args) throws IOException {
        String sampleSheet = "[Header],,,,,,,,\n" +
                "IEMFileVersion,4,,,,,,,\n" +
                "Date,4/11/2025,,,,,,,\n" +
                "Workflow,GenerateFASTQ,,,,,,,\n" +
                "Application,FAUCI2,,,,,,,\n" +
                "Assay,,,,,,,,\n" +
                "Description,,,,,,,,\n" +
                "Chemistry,Default,,,,,,,\n" +
                ",,,,,,,,\n" +
                "[Reads],,,,,,,,\n" +
                "151,,,,,,,,\n" +
                "151,,,,,,,,\n" +
                ",,,,,,,,\n" +
                "[Settings],,,,,,,,\n" +
                "BarcodeMismatchesIndex1,1,,,,,,,\n" +
                "BarcodeMismatchesIndex2,1,,,,,,,\n" +
                "[Data],,,,,,,,\n" +
                "Lane,Sample_ID,Sample_Plate,Sample_Well,I7_Index_ID,index,index2,Sample_Project,Bait_Set,Description\n" +
                "1,1_RNA_nascent_wo_input_RPE1FUCCI_rep1_IGO_17170_1,Human,User_RNA,NEBNext1,ATCACGAT,CGAGATCT,Project_17170,,powells@mskcc.org\n" +
                "1,2_RNA_nascent_wo_G1_RPE1FUCCI_rep1_IGO_17170_2,Human,User_RNA,NEBNext2,CGATGTAT,CGAGATCT,Project_17170,,powells@mskcc.org\n" +
                "1,3_RNA_nascent_wo_G2_M_RPE1FUCCI_rep1_IGO_17170_3,Human,User_RNA,NEBNext3,TTAGGCAT,CGAGATCT,Project_17170,,powells@mskcc.org\n" +
                "1,4_RNA_nascent_wo_S_RPE1FUCCI_rep1_IGO_17170_4,Human,User_RNA,NEBNext4,TGACCAAT,CGAGATCT,Project_17170,,powells@mskcc.org\n" +
                "1,5_RNA_nascent_wo_earlyS_RPE1FUCCI_rep1_IGO_17170_5,Human,User_RNA,NEBNext5,ACAGTGAT,CGAGATCT,Project_17170,,powells@mskcc.org\n" +
                "1,6_RNA_nascent_REV_input_RPE1FUCCI_rep1_IGO_17170_6,Human,User_RNA,NEBNext6,GCCAATAT,CGAGATCT,Project_17170,,powells@mskcc.org\n" +
                "1,7_RNA_nascent_REV_G1_RPE1FUCCI_rep1_IGO_17170_7,Human,User_RNA,NEBNext7,CAGATCAT,CGAGATCT,Project_17170,,powells@mskcc.org\n" +
                "1,8_RNA_nascent_REV_G2_M_RPE1FUCCI_rep1_IGO_17170_8,Human,User_RNA,NEBNext8,ACTTGAAT,CGAGATCT,Project_17170,,powells@mskcc.org\n" +
                "1,9_RNA_nascent_REV_S_RPE1FUCCIrep1_rep1_IGO_17170_9,Human,User_RNA,NEBNext9,GATCAGAT,CGAGATCT,Project_17170,,powells@mskcc.org\n" +
                "1,10_RNA_nascent_REV_earlyS_RPE1FUCCI_rep1_IGO_17170_10,Human,User_RNA,NEBNext10,TAGCTTAT,CGAGATCT,Project_17170,,powells@mskcc.org\n" +
                "1,113483_CG24-792_IGO_14096_Q_10,Human,WGS_Deep,UDI0064,CCATTCGA,GTTGTCCG,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-794_IGO_14096_Q_12,Human,WGS_Deep,UDI0065,ACACTAAG,ATCCATAT,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-795_IGO_14096_Q_13,Human,WGS_Deep,UDI0066,GTGTCGGA,GCTTGCGC,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-798_IGO_14096_Q_16,Human,WGS_Deep,UDI0067,TTCCTGTT,AGTATCTT,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-800_IGO_14096_Q_18,Human,WGS_Deep,UDI0068,CCTTCACC,GACGCTCC,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-801_IGO_14096_Q_19,Human,WGS_Deep,UDI0069,GCCACAGG,CATGCCAT,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-783_IGO_14096_Q_1,Human,WGS_Deep,UDI0057,TGCGGCGT,CCTCGGTA,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-802_IGO_14096_Q_20,Human,WGS_Deep,UDI0070,ATTGTGAA,TGCATTGC,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-803_IGO_14096_Q_21,Human,WGS_Deep,UDI0071,ACTCGTGT,ATTGGAAC,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-804_IGO_14096_Q_22,Human,WGS_Deep,UDI0072,GTCTACAC,GCCAAGGT,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-805_IGO_14096_Q_23,Human,WGS_Deep,UDI0073,CAATTAAC,CGAGATAT,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-807_IGO_14096_Q_25,Human,WGS_Deep,UDI0074,TGGCCGGT,TAGAGCGC,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-808_IGO_14096_Q_26,Human,WGS_Deep,UDI0075,AGTACTCC,AACCTGTT,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-809_IGO_14096_Q_27,Human,WGS_Deep,UDI0076,GACGTCTT,GGTTCACC,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-810_IGO_14096_Q_28,Human,WGS_Deep,UDI0077,TGCGAGAC,CATTGTTG,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-811_IGO_14096_Q_29,Human,WGS_Deep,UDI0078,CATAGAGT,TGCCACCA,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-812_IGO_14096_Q_30,Human,WGS_Deep,UDI0079,ACAGGCGC,CTCTGCCT,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-813_IGO_14096_Q_31,Human,WGS_Deep,UDI0080,GTGAATAT,TCTCATTC,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-814_IGO_14096_Q_32,Human,WGS_Deep,UDI0081,AACTGTAG,ACGCCGCA,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-815_IGO_14096_Q_33,Human,WGS_Deep,UDI0082,GGTCACGA,GTATTATG,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-816_IGO_14096_Q_34,Human,WGS_Deep,UDI0083,CTGCTTCC,GATAGATC,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-817_IGO_14096_Q_35,Human,WGS_Deep,UDI0084,TCATCCTT,AGCGAGCT,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-818_IGO_14096_Q_36,Human,WGS_Deep,UDI0085,AGGTTATA,CAGTTCCG,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-819_IGO_14096_Q_37,Human,WGS_Deep,UDI0086,GAACCGCG,TGACCTTA,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-820_IGO_14096_Q_38,Human,WGS_Deep,UDI0087,CTCACCAA,CTAGGCAA,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-821_IGO_14096_Q_39,Human,WGS_Deep,UDI0088,TCTGTTGG,TCGAATGG,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-786_IGO_14096_Q_4,Human,WGS_Deep,UDI0058,CATAATAC,TTCTAACG,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-787_IGO_14096_Q_5,Human,WGS_Deep,UDI0059,GATCTATC,ATGAGGCT,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-788_IGO_14096_Q_6,Human,WGS_Deep,UDI0060,AGCTCGCT,GCAGAATC,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-789_IGO_14096_Q_7,Human,WGS_Deep,UDI0061,CGGAACTG,CACTACGA,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-790_IGO_14096_Q_8,Human,WGS_Deep,UDI0062,TAAGGTCA,TGTCGTAG,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,113483_CG24-791_IGO_14096_Q_9,Human,WGS_Deep,UDI0063,TTGCCTAG,ACCACTTA,Project_14096_Q,,maclachk@mskcc.org\n" +
                "1,ND43_IGO_16911_D_2,Human,WGS_Deep,UDI0024,ATGAGGCC,GTTAATTG,Project_16911_D,,dewolfs@mskcc.org\n" +
                "7,S1763148_2_11_FFT_130313A_15_33_IGO_17147_1,Human,SC_DLP,DLPi7_33-i5_15,GGCACAAT,ACCAAGTT,Project_17147,,alrawid@mskcc.org\n" +
                "7,S1763148_2_11_FFT_130313A_16_33_IGO_17147_1,Human,SC_DLP,DLPi7_33-i5_16,GGCACAAT,ACGCCGGT,Project_17147,,alrawid@mskcc.org\n" +
                "7,S1763148_2_11_FFT_130313A_17_33_IGO_17147_1,Human,SC_DLP,DLPi7_33-i5_17,GGCACAAT,ACCGGCCT,Project_17147,,alrawid@mskcc.org\n" +
                "7,S1763148_2_11_FFT_130313A_18_33_IGO_17147_1,Human,SC_DLP,DLPi7_33-i5_18,GGCACAAT,ACTAGTTG,Project_17147,,alrawid@mskcc.org\n" +
                "7,S1763148_2_11_FFT_130313A_19_33_IGO_17147_1,Human,SC_DLP,DLPi7_33-i5_19,GGCACAAT,ACGCGTGG,Project_17147,,alrawid@mskcc.org\n" +
                "7,S1763148_2_11_FFT_130313A_20_33_IGO_17147_1,Human,SC_DLP,DLPi7_33-i5_20,GGCACAAT,ACGTATAG,Project_17147,,alrawid@mskcc.org\n" +
                "7,S1959278_3_1_FIM_130313A_43_14_IGO_17148_1,Human,SC_DLP,DLPi7_14-i5_43,AAAGCAAT,ACCCACTC,Project_17148,,alrawid@mskcc.org\n" +
                "7,S1959278_3_1_FIM_130313A_44_14_IGO_17148_1,Human,SC_DLP,DLPi7_14-i5_44,AAAGCAAT,ACCAGCAG,Project_17148,,alrawid@mskcc.org\n" +
                "7,S1959278_3_1_FIM_130313A_45_14_IGO_17148_1,Human,SC_DLP,DLPi7_14-i5_45,AAAGCAAT,ACCGCGGC,Project_17148,,alrawid@mskcc.org\n" +
                "7,S1959278_3_1_FIM_130313A_46_14_IGO_17148_1,Human,SC_DLP,DLPi7_14-i5_46,AAAGCAAT,ACGAATGA,Project_17148,,alrawid@mskcc.org\n" +
                "7,S1959278_3_1_FIM_130313A_47_14_IGO_17148_1,Human,SC_DLP,DLPi7_14-i5_47,AAAGCAAT,ACGCGCCA,Project_17148,,alrawid@mskcc.org\n" +
                "7,S1959278_3_1_FIM_130313A_48_14_IGO_17148_1,Human,SC_DLP,DLPi7_14-i5_48,AAAGCAAT,ACCTCTAC,Project_17148,,alrawid@mskcc.org\n" +
                "7,S1959278_3_1_FIM_130313A_49_14_IGO_17148_1,Human,SC_DLP,DLPi7_14-i5_49,AAAGCAAT,ACCACTGT,Project_17148,,alrawid@mskcc.org\n" +
                "7,S1959278_3_1_FIM_130313A_50_14_IGO_17148_1,Human,SC_DLP,DLPi7_14-i5_50,AAAGCAAT,ACATGTTT,Project_17148,,alrawid@mskcc.org\n" +
                "8,S1959278_3_1_FIM_130313A_61_64_IGO_17148_1,Human,SC_DLP,DLPi7_64-i5_61,ACCGGCAT,ACATTGGC,Project_17148,,alrawid@mskcc.org"; // paste your sample sheet here
        List<SampleData> data = parseSampleSheet(sampleSheet);
        System.out.println("Data:" + data.get(0));

    }
}

