package org.mskcc.limsrest.limsapi;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

// TODO: integration test for SetOrCreateBanked
public class SetOrCreateBankedTest {

    private SetOrCreateBanked setOrCreateBanked;
    private String sampleId = "111";

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
                String tumorOrNormal = setOrCreateBanked.setTumorOrNormal(sampleClass, tumorType, sampleId);
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
                "Other", "", null, "TMT", "UCS"
        };

        for (String sampleClass: sampleClasses) {
            for (String tumorType: tumorTypes) {
                String tumorOrNormal = setOrCreateBanked.setTumorOrNormal(sampleClass, tumorType, sampleId);
                assertEquals("Tumor", tumorOrNormal);
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTumorOrNormalError() {
        setOrCreateBanked.setTumorOrNormal("Primary", "Normal", sampleId);
    }

    @Test
    public void setTumorOrNormalErrorUnknownSampleClass() {
        assertEquals("Normal", setOrCreateBanked.setTumorOrNormal("", "Normal", sampleId));
    }

    @Test
    public void whenSampleClassIsOtherTumorTypeIsTumor_thenNormal() {
        String tumorOrNormal = setOrCreateBanked.setTumorOrNormal("Other", "Tumor", sampleId);
        assertEquals("Tumor", tumorOrNormal);
    }

    @Test
    public void whenSampleClassIsOtherTumorTypeIsNormal_thenNormal() {
        String tumorOrNormal = setOrCreateBanked.setTumorOrNormal("Other", "Normal", sampleId);
        assertEquals("Normal", tumorOrNormal);
    }

    @Test
    public void whenSampleClassIsOtherTumorTypeIsOther_thenNormal() {
        String tumorOrNormal = setOrCreateBanked.setTumorOrNormal("Other", "Other", sampleId);
        assertEquals("Normal", tumorOrNormal);
    }

    @Test
    public void whenSampleClassIsOtherTumorTypeIsBlank_thenNormal() {
        String tumorOrNormal = setOrCreateBanked.setTumorOrNormal("Other", "", sampleId);
        assertEquals("Normal", tumorOrNormal);
    }
}