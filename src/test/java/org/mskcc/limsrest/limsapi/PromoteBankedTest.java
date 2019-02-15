package org.mskcc.limsrest.limsapi;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mskcc.domain.RequestSpecies;
import org.mskcc.domain.sample.BankedSample;
import org.mskcc.domain.sample.CmoSampleInfo;
import org.mskcc.limsrest.limsapi.cmoinfo.CorrectedCmoSampleIdGenerator;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.CorrectedCmoIdConverter;
import org.mskcc.limsrest.limsapi.promote.BankedSampleToSampleConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class PromoteBankedTest {

    private PromoteBanked promoteBanked;
    private CorrectedCmoSampleIdGenerator correctedCmoSampleIdGenerator;

    @Before
    public void setup() {
        correctedCmoSampleIdGenerator = Mockito.mock(CorrectedCmoSampleIdGenerator.class);
        promoteBanked = new PromoteBanked(Mockito.mock(CorrectedCmoIdConverter.class), correctedCmoSampleIdGenerator, Mockito.mock(BankedSampleToSampleConverter.class));
    }

    @Test
    public void setSeqRequirementsIMPACT() {
        HashMap<String, Object> map = new HashMap<>();
        PromoteBanked.setSeqReq("IMPACT468", "Tumor", map);
        assertEquals("PE100", (String) map.get("SequencingRunType"));
        assertEquals(14.0, map.get("RequestedReads"));
        assertEquals(500, map.get("CoverageTarget"));

        PromoteBanked.setSeqReq("M-IMPACT_v1", "Tumor", map);
        assertEquals("PE100", (String) map.get("SequencingRunType"));
        assertEquals(14.0, map.get("RequestedReads"));
        assertEquals(500, map.get("CoverageTarget"));

        PromoteBanked.setSeqReq("IMPACT468", "Normal", map);
        assertEquals("PE100", (String) map.get("SequencingRunType"));
        assertEquals(7.0, map.get("RequestedReads"));
        assertEquals(250, map.get("CoverageTarget"));
    }

    @Test
    public void setSeqRequirementsHemePACT() {
        HashMap<String, Object> map = new HashMap<>();
        PromoteBanked.setSeqReq("HemePACT", "Tumor", map);
        assertEquals("PE100", (String) map.get("SequencingRunType"));
        assertEquals(20.0, map.get("RequestedReads"));
        assertEquals(500, map.get("CoverageTarget"));

        PromoteBanked.setSeqReq("HemePACT", "Normal", map);
        assertEquals("PE100", (String) map.get("SequencingRunType"));
        assertEquals(10.0, map.get("RequestedReads"));
        assertEquals(250, map.get("CoverageTarget"));
    }

    @Test
    public void setSeqRequirementsWES_whenRequestReadsIsValid() {
        Map<String, Object> seqRequirementMap = new HashMap<>();
        promoteBanked.setSeqReqForWES("100X", seqRequirementMap);
        Assertions.assertThat(seqRequirementMap).hasSize(3);
        Assertions.assertThat(seqRequirementMap).containsKeys("SequencingRunType", "CoverageTarget", "RequestedReads");
        Assertions.assertThat(seqRequirementMap).containsEntry("SequencingRunType", "PE100");
        Assertions.assertThat(seqRequirementMap).containsEntry("CoverageTarget", 100);
        Assertions.assertThat(seqRequirementMap).containsEntry("RequestedReads", 60.0);
    }

    @Test
    public void setSeqRequirementWES_whenCoverageTargetIsNotInMap() {
        Map<String, Object> seqRequirementMap = new HashMap<>();
        promoteBanked.setSeqReqForWES("1000X", seqRequirementMap);
        Assertions.assertThat(seqRequirementMap).hasSize(3);
        Assertions.assertThat(seqRequirementMap).containsKeys("SequencingRunType", "CoverageTarget", "RequestedReads");
        Assertions.assertThat(seqRequirementMap).containsEntry("SequencingRunType", "PE100");
        Assertions.assertThat(seqRequirementMap).containsEntry("CoverageTarget", 1000);
        Assertions.assertThat(seqRequirementMap).containsEntry("RequestedReads", null);
    }

    @Test
    public void setSeqRequirementWES_whenRequestedReadsIsEmpty () {
        Map<String, Object> seqRequirementMap = new HashMap<>();
        promoteBanked.setSeqReqForWES("", seqRequirementMap);
        Assertions.assertThat(seqRequirementMap).hasSize(1);
        Assertions.assertThat(seqRequirementMap).containsKeys("SequencingRunType");
        Assertions.assertThat(seqRequirementMap).containsEntry("SequencingRunType", "PE100");
    }

    @Test
    public void unaliquotname_whenSampleNameIsNotAliquot() {
        String sampleName = "1234_S_1";
        Assertions.assertThat(promoteBanked.unaliquotName(sampleName)).isEqualTo(sampleName);
    }

    @Test
    public void unaliquotname_whenSampleNameIsAliquot() {
        String sampleName = "1234_S_1_1_1_1";
        Assertions.assertThat(promoteBanked.unaliquotName(sampleName)).isEqualTo("1234_S_1");
    }

    @Test
    public void getCorrectedCmoSampleId_whenSpeciesIsNotHuman() {
        Map<String, Object> fields = ImmutableMap.<String, Object>builder()
                .put("CMOPatientId", "pid343")
                .put("SampleType", "DNA")
                .put("Species", "Mouse")
                .build();
        BankedSample bankedSample = new BankedSample("123", fields);
        String id = promoteBanked.getCorrectedCmoSampleId(bankedSample, "123_S");
        Assertions.assertThat(id).isEmpty();
    }

    @Test
    public void getCorrectedCmoSampleId_whenSpeciesIsHuman() {
        Map<String, Object> fields = ImmutableMap.<String, Object>builder()
                .put("CMOPatientId", "pid343")
                .put("SampleType", "DNA")
                .put("Species", "Human")
                .put("UserSampleID", "U2343")
                .put("OtherSampleId", "O34234")
                .build();
        String mockCorrectedId = "C-AAAAA1-V001-d";
        Mockito.when(correctedCmoSampleIdGenerator.generate(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(mockCorrectedId);
        BankedSample bankedSample = new BankedSample("123", fields);
        String id = promoteBanked.getCorrectedCmoSampleId(bankedSample, "123_S");
        Assertions.assertThat(id).isEqualTo(mockCorrectedId);
    }

    @Test
    public void getCmoFields() {
        Map<String, Object> bankedFields = ImmutableMap.<String, Object>builder()
                .put("CMOPatientId", "pid343")
                .put("SampleType", "DNA")
                .put("Species", "Human")
                .put("UserSampleID", "U2343")
                .put("OtherSampleId", "O34234")
                .put(BankedSample.CLINICAL_INFO, "23432")
                .put(BankedSample.SAMPLE_CLASS, "Tumor")
                .put(BankedSample.COLLECTION_YEAR, 1999)
                .put(BankedSample.PATIENT_ID, "2354")
                .put(BankedSample.ESTIMATED_PURITY, "0.9")
                .put(BankedSample.GENDER, "F")
                .put(BankedSample.GENETIC_ALTERATIONS, "DDF")
                .put(BankedSample.NORMALIZED_PATIENT_ID, "23534")
                .put(BankedSample.PRESERVATION, "OK")
                .put(BankedSample.TUMOR_TYPE, "TUO")
                .put(BankedSample.TUMOR_OR_NORMAL, "TU")
                .put(BankedSample.TISSUE_SITE, "TH")
                .put(BankedSample.SPECIMEN_TYPE, "")
                .put(BankedSample.SAMPLE_ORIGIN, "")
                .build();

        Map<String, Object> cmoFields = promoteBanked.getCmoFields(bankedFields, "C-AAAAA1-V001-d", "123_S", "igo-2", "UIFDF");
        Assertions.assertThat(cmoFields).containsKeys(
                CmoSampleInfo.ALT_ID, CmoSampleInfo.CLINICAL_INFO, CmoSampleInfo.CMO_PATIENT_ID,
                CmoSampleInfo.CMOSAMPLE_CLASS, CmoSampleInfo.COLLECTION_YEAR, CmoSampleInfo.CORRECTED_INVEST_PATIENT_ID, CmoSampleInfo.DMPLIBRARY_INPUT, CmoSampleInfo.DMPLIBRARY_OUTPUT,
                CmoSampleInfo.ESTIMATED_PURITY, CmoSampleInfo.GENDER, CmoSampleInfo.GENETIC_ALTERATIONS,
                CmoSampleInfo.NORMALIZED_PATIENT_ID, CmoSampleInfo.OTHER_SAMPLE_ID, CmoSampleInfo.PATIENT_ID,
                CmoSampleInfo.PRESERVATION, CmoSampleInfo.REQUEST_ID, CmoSampleInfo.SAMPLE_ID,
                CmoSampleInfo.SAMPLE_ORIGIN, CmoSampleInfo.SPECIES, CmoSampleInfo.SPECIMEN_TYPE,
                CmoSampleInfo.TISSUE_LOCATION, CmoSampleInfo.TUMOR_OR_NORMAL,
                CmoSampleInfo.TUMOR_TYPE, CmoSampleInfo.USER_SAMPLE_ID
        );
    }
}