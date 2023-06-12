package org.mskcc.limsrest.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class GetSampleManifestTaskTest {

    @Test
    public void filterMultipleDemuxes() {
        List<ArchivedFastq> passedQCList = new ArrayList<>();
        ArchivedFastq a = new ArchivedFastq();
        a.setFastqLastModified(new Date()); // a is most recent
        a.setFastq("/ifs/archive/GCL/hiseq/FASTQ/JAX_0420_AHGK5CBBXY_A1/Project_10673/Sample_P-0034230-N01-WES_IGO_10673_1/P-0034230-N01-WES_IGO_10673_1_S6_R1_001.fastq.gz");
        a.setRunBaseDirectory("JAX_0420_AHGK5CBBXY_A1");

        ArchivedFastq b = new ArchivedFastq();
        b.setFastqLastModified(new Date(1000));
        b.setFastq(  "/ifs/archive/GCL/hiseq/FASTQ/JAX_0420_AHGK5CBBXY/Project_10673/Sample_P-0034230-N01-WES_IGO_10673_1/P-0034230-N01-WES_IGO_10673_1_S32_R1_001.fastq.gz");
        b.setRunBaseDirectory("JAX_0420_AHGK5CBBXY");


        List<ArchivedFastq> fastqs = GetSampleManifestTask.filterMultipleDemuxes(Arrays.asList(b,a));
        // assert only a (most recent) fastq is returned
        assertEquals(a, fastqs.get(0));
        assertEquals(1, fastqs.size());
    }

    @Test
    public void sameRunDifferentLaneTrue() {
        List<ArchivedFastq> passedQCList = new ArrayList<>();
        ArchivedFastq a = new ArchivedFastq();
        a.setFastq("/ifs/archive/GCL/hiseq/FASTQ/JAX_0039_BHCKCHBBXX/Project_06208_C/Sample_P-0009090-T01-WES_IGO_06208_C_80/P-0009090-T01-WES_IGO_06208_C_80_S25_L005_R1_001.fastq.gz");
        a.setRunBaseDirectory("JAX_0039_BHCKCHBBXX");
        a.setFastqLastModified(new Date(5000));

        ArchivedFastq b = new ArchivedFastq();
        b.setFastq("/ifs/archive/GCL/hiseq/FASTQ/JAX_0039_BHCKCHBBXX/Project_06208_C/Sample_P-0009090-T01-WES_IGO_06208_C_80/P-0009090-T01-WES_IGO_06208_C_80_S25_L006_R2_001.fastq.gz");
        b.setRunBaseDirectory("JAX_0039_BHCKCHBBXX");
        b.setFastqLastModified(new Date(6000));

        List<ArchivedFastq> result = GetSampleManifestTask.filterMultipleDemuxes(Arrays.asList(b,a));
        assertEquals(2, result.size());
    }

    @Test
    public void sameLaneR1AndR2() {
        List<ArchivedFastq> passedQCList = new ArrayList<>();
        ArchivedFastq a = new ArchivedFastq();
        a.setFastq("/igo/delivery/FASTQ/MICHELLE_0383_AHC3LWDSX2_v2/Project_11570_B/Sample_001_IGO_11570_B_1/001_IGO_11570_B_1_S1_L003_R1_001.fastq.gz");
        a.setRunBaseDirectory("MICHELLE_0383_AHC3LWDSX2_v2");
        a.setFastqLastModified(new Date(5000));

        ArchivedFastq b = new ArchivedFastq();
        b.setFastq("/igo/delivery/FASTQ/MICHELLE_0383_AHC3LWDSX2_v2/Project_11570_B/Sample_001_IGO_11570_B_1/001_IGO_11570_B_1_S1_L003_R2_001.fastq.gz");
        b.setRunBaseDirectory("MICHELLE_0383_AHC3LWDSX2_v2");
        b.setFastqLastModified(new Date(5000));

        List<ArchivedFastq> result = GetSampleManifestTask.filterMultipleDemuxes(Arrays.asList(a,b));
        assertEquals(2, result.size());
    }

    @Test
    public void sameRunRedemux_A2() {
        List<ArchivedFastq> passedQCList = new ArrayList<>();
        ArchivedFastq a = new ArchivedFastq();
        a.setFastq("/ifs/archive/GCL/hiseq/FASTQ/A00227_0011_BH2YHKDMXX_A2/Project_93017_F/Sample_P-0020689-T01-WES_IGO_93017_F_74/P-0020689-T01-WES_IGO_93017_F_74_S11_R1_001.fastq.gz");
        a.setRunBaseDirectory("A00227_0011_BH2YHKDMXX_A2");
        a.setFastqLastModified(new Date(20000)); // more recent date than below

        ArchivedFastq b = new ArchivedFastq();
        b.setFastq(   "/ifs/archive/GCL/hiseq/FASTQ/A00227_0011_BH2YHKDMXX/Project_93017_F/Sample_P-0020689-T01-WES_IGO_93017_F_74/P-0020689-T01-WES_IGO_93017_F_74_S88_R1_001.fastq.gz");
        b.setRunBaseDirectory("A00227_0011_BH2YHKDMXX");
        b.setFastqLastModified(new Date(10000));

        List<ArchivedFastq> result = GetSampleManifestTask.filterMultipleDemuxes(Arrays.asList(b,a));
        assertEquals(a, result.get(0));
    }

    @Test
    public void sameRunRedemuxManySamples() {
        List<ArchivedFastq> passedQCList = new ArrayList<>();
        ArchivedFastq a = new ArchivedFastq();
        a.setFastq("/ifs/archive/GCL/hiseq/FASTQ/JAX_0420_AHGK5CBBXY_A1/Project_10673/Sample_P-0034230-N01-WES_IGO_10673_1/P-0034230-N01-WES_IGO_10673_1_S6_R1_001.fastq.gz");
        ArchivedFastq b = new ArchivedFastq();
        b.setFastq(  "/ifs/archive/GCL/hiseq/FASTQ/JAX_0420_AHGK5CBBXY/Project_10673/Sample_P-0034230-N01-WES_IGO_10673_1/P-0034230-N01-WES_IGO_10673_1_S323_R1_001.fastq.gz");
        passedQCList.add(a);passedQCList.add(b);

        //assertTrue(GetSampleManifestTask.hasRedemux(passedQCList));
    }
}