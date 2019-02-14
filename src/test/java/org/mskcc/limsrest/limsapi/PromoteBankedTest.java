package org.mskcc.limsrest.limsapi;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mskcc.limsrest.limsapi.cmoinfo.CorrectedCmoSampleIdGenerator;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.CorrectedCmoIdConverter;
import org.mskcc.limsrest.limsapi.promote.BankedSampleToSampleConverter;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class PromoteBankedTest {

    private PromoteBanked promoteBanked;

    @Before
    public void setup() {
        promoteBanked = new PromoteBanked(Mockito.mock(CorrectedCmoIdConverter.class), Mockito.mock(CorrectedCmoSampleIdGenerator.class), Mockito.mock(BankedSampleToSampleConverter.class));
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

}