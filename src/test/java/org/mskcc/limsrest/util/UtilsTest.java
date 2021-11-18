package org.mskcc.limsrest.util;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mskcc.limsrest.util.Utils.selectLarger;

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

    @Test
    public void testSelectLarger() {
        double result = selectLarger("30-40 million");
        assertEquals(40.0, result, 0.001);
    }

    @Test
    public void testGetBaseSampleId() {
        assertEquals("012345_B", Utils.getBaseSampleId("012345_B"));
        assertEquals("012345_B_1", Utils.getBaseSampleId("012345_B_1_1_2"));
    }
}
