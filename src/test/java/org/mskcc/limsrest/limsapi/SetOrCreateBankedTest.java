package org.mskcc.limsrest.limsapi;

import org.junit.Test;

import static org.junit.Assert.*;

public class SetOrCreateBankedTest {

    @Test
    public void setTumorOrNormalToNormal() {
        assertEquals("Normal", SetOrCreateBanked.setTumorOrNormal("Normal", "Normal"));
        assertEquals("Normal", SetOrCreateBanked.setTumorOrNormal("Normal", ""));
        assertEquals("Normal", SetOrCreateBanked.setTumorOrNormal("Adjacent Normal", null));
    }

    @Test
    public void setTumorOrNormalToTumor() {
        assertEquals("Tumor", SetOrCreateBanked.setTumorOrNormal("Adjacent Tissue", "Tumor"));
        assertEquals("Tumor", SetOrCreateBanked.setTumorOrNormal("Unkown Tumor", ""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTumorOrNormalError() {
        SetOrCreateBanked.setTumorOrNormal("Primary", "Normal");
    }
}