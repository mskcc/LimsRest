package org.mskcc.limsrest.service;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class GetSampleManifestTaskTest {
    @Test
    public void sameRunDifferentLaneTrue() {
        List<ArchivedFastq> passedQCList = new ArrayList<>();
        ArchivedFastq a = new ArchivedFastq();
        a.setFastq("/ifs/archive/GCL/hiseq/FASTQ/JAX_0039_BHCKCHBBXX/Project_06208_C/Sample_P-0009090-T01-WES_IGO_06208_C_80/P-0009090-T01-WES_IGO_06208_C_80_S25_L005_R1_001.fastq.gz");
        ArchivedFastq b = new ArchivedFastq();
        b.setFastq("/ifs/archive/GCL/hiseq/FASTQ/JAX_0039_BHCKCHBBXX/Project_06208_C/Sample_P-0009090-T01-WES_IGO_06208_C_80/P-0009090-T01-WES_IGO_06208_C_80_S25_L006_R2_001.fastq.gz");
        passedQCList.add(a);passedQCList.add(b);

        assertFalse(GetSampleManifestTask.hasRedemux(passedQCList));
    }

    @Test
    public void sameRunRedemux() {
        List<ArchivedFastq> passedQCList = new ArrayList<>();
        ArchivedFastq a = new ArchivedFastq();
        a.setFastq("/ifs/archive/GCL/hiseq/FASTQ/A00227_0011_BH2YHKDMXX_A2/Project_93017_C/Sample_P-0020609-N01-WES_IGO_93017_C_2/P-0020609-N01-WES_IGO_93017_C_2_S60_R2_001.fastq.gz");
        ArchivedFastq b = new ArchivedFastq();
        b.setFastq(   "/ifs/archive/GCL/hiseq/FASTQ/A00227_0011_BH2YHKDMXX/Project_93017_C/Sample_P-0020609-N01-WES_IGO_93017_C_2/P-0020609-N01-WES_IGO_93017_C_2_S60_R2_001.fastq.gz");
        passedQCList.add(a);passedQCList.add(b);

        assertTrue(GetSampleManifestTask.hasRedemux(passedQCList));
    }

    @Test
    public void sameRunRedemux2() {
        List<ArchivedFastq> passedQCList = new ArrayList<>();
        ArchivedFastq a = new ArchivedFastq();
        a.setFastq("/ifs/archive/GCL/hiseq/FASTQ/A00227_0011_BH2YHKDMXX_A2/Project_93017_F/Sample_P-0020689-T01-WES_IGO_93017_F_74/P-0020689-T01-WES_IGO_93017_F_74_S11_R1_001.fastq.gz");
        ArchivedFastq b = new ArchivedFastq();
        b.setFastq(   "/ifs/archive/GCL/hiseq/FASTQ/A00227_0011_BH2YHKDMXX/Project_93017_F/Sample_P-0020689-T01-WES_IGO_93017_F_74/P-0020689-T01-WES_IGO_93017_F_74_S88_R1_001.fastq.gz");
        passedQCList.add(a);passedQCList.add(b);

        assertTrue(GetSampleManifestTask.hasRedemux(passedQCList));
    }

    @Test
    public void sameRunRedemux3() {
        List<ArchivedFastq> passedQCList = new ArrayList<>();
        ArchivedFastq a = new ArchivedFastq();
        a.setFastq("/ifs/archive/GCL/hiseq/FASTQ/JAX_0420_AHGK5CBBXY_A1/Project_10673/Sample_P-0034230-N01-WES_IGO_10673_1/P-0034230-N01-WES_IGO_10673_1_S6_R1_001.fastq.gz");
        ArchivedFastq b = new ArchivedFastq();
        b.setFastq(  "/ifs/archive/GCL/hiseq/FASTQ/JAX_0420_AHGK5CBBXY/Project_10673/Sample_P-0034230-N01-WES_IGO_10673_1/P-0034230-N01-WES_IGO_10673_1_S32_R1_001.fastq.gz");
        passedQCList.add(a);passedQCList.add(b);

        assertTrue(GetSampleManifestTask.hasRedemux(passedQCList));
    }
}