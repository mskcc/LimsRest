package org.mskcc.limsrest.limsapi;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class SetOrCreateBankedTest {

    private SetOrCreateBanked setOrCreateBanked;

    @Before
    public void setupSetOrCreateBanked() {
        setOrCreateBanked = new SetOrCreateBanked();
    }

    @Test
    public void setTumorOrNormalToNormal() {
        String[] sampleClasses = {
                "Normal", "Adjacent Normal",
        };

        String[] tumorTypes = {
                "Normal", "Other", "", null, "Tumor"
        };

        for (String sampleClass: sampleClasses) {
            for (String tumorType: tumorTypes) {
                String tumorOrNormal = setOrCreateBanked.setTumorOrNormal(sampleClass, tumorType);
                assertEquals("Normal", tumorOrNormal);
            }
        }

    }

    @Test
    public void setTumorOrNormalToTumor() {
        String[] sampleClasses = {
                "Unknown Tumor", "Primary", "Metastasis", "Adjacent Tissue", "Local Recurrence",
        };

        String[] tumorTypes = {
                "Other", "", null, "Tumor"
        };

        for (String sampleClass: sampleClasses) {
            for (String tumorType: tumorTypes) {
                String tumorOrNormal = setOrCreateBanked.setTumorOrNormal(sampleClass, tumorType);
                assertEquals("Tumor", tumorOrNormal);
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTumorOrNormalError() {
        setOrCreateBanked.setTumorOrNormal("Primary", "Normal");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTumorOrNormalErrorUnknownSampleClass() {
        setOrCreateBanked.setTumorOrNormal("unknown-sample-class", "Normal");
    }

    @Test
    public void whenSampleClassIsOtherTumorTypeIsTumor_thenTumor() {
        String tumorOrNormal = setOrCreateBanked.setTumorOrNormal("Other", "Tumor");
        assertEquals("Tumor", tumorOrNormal);
    }

    @Test
    public void whenSampleClassIsOtherTumorTypeIsNormal_thenNormal() {
        String tumorOrNormal = setOrCreateBanked.setTumorOrNormal("Other", "Normal");
        assertEquals("Normal", tumorOrNormal);
    }

    @Test
    public void whenSampleClassIsOtherTumorTypeIsOther_thenNormal() {
        String tumorOrNormal = setOrCreateBanked.setTumorOrNormal("Other", "Other");
        assertEquals("Normal", tumorOrNormal);
    }

    @Test
    public void whenSampleClassIsOtherTumorTypeIsBlank_thenNormal() {
        String tumorOrNormal = setOrCreateBanked.setTumorOrNormal("Other", "");
        assertEquals("Normal", tumorOrNormal);
    }
}