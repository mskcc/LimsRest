package org.mskcc.limsrest.util;

import org.junit.Test;

import static org.junit.Assert.*;
public class UtilsTest {

    @Test
    public void getOncotreeTumorType(){
        assertEquals("Thyroid Cancer", Utils.getOncotreeTumorType("THAP"));
        assertEquals("Breast Cancer, NOS", Utils.getOncotreeTumorType("Breast"));
    }

    @Test
    public void isCompleteStatus(){
        assertTrue(Utils.isSequencingCompleteStatus("Completed - Illumina Sequencing"));
        assertTrue(Utils.isCompleteStatus("Completed - Illumina Sequencing"));
        assertTrue(Utils.isCompleteStatus("Failed - RNA Extraction"));
        assertTrue(Utils.isCompleteStatus("Failed - Kapa Library Preparation"));
        assertTrue(Utils.isCompleteStatus("Completed - Normalization Plate Setup"));
        assertFalse(Utils.isCompleteStatus("In Process - RNA Extraction"));
    }

    @Test
    public void getSampleTypeOrder(){
        assertEquals(1, Utils.getSampleTypeOrder("blood"));
        assertEquals(2, Utils.getSampleTypeOrder("cdna"));
        assertEquals(3, Utils.getSampleTypeOrder("cdna library"));
        assertEquals(4, Utils.getSampleTypeOrder("capture library"));
        assertEquals(5, Utils.getSampleTypeOrder("pooled library"));
        assertEquals(0, Utils.getSampleTypeOrder("Test"));
    }

}
