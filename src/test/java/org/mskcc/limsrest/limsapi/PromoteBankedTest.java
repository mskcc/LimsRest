package org.mskcc.limsrest.limsapi;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public class PromoteBankedTest {

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
}