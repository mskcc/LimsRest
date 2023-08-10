package org.mskcc.limsrest.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IGOToolsTest {

    @Test
    public void requestIdFromIgoId() {
        assertEquals("06049_A", IGOTools.requestFromIgoId("06049_A"));
        assertEquals("06049_AA", IGOTools.requestFromIgoId("06049_AA_3_2_1"));
        assertEquals("06049", IGOTools.requestFromIgoId("06049_1_1"));
        assertEquals("06049_O", IGOTools.requestFromIgoId("06049_O_28"));
        assertEquals("05022_I", IGOTools.requestFromIgoId("05022_I_1"));
    }

    @Test
    public void baseIgoSampleId() {
        assertEquals("06049_AA_3", IGOTools.baseIgoSampleId("06049_AA_3_2_1"));
        assertEquals("06049_AA_33", IGOTools.baseIgoSampleId("06049_AA_33_2_1"));
        assertEquals("06049_33", IGOTools.baseIgoSampleId("06049_33_2_1"));
        assertEquals("05022_I_1", IGOTools.baseIgoSampleId("05022_I_1"));
    }

    @Test
    public void isValidIgoSampleId() {
        assertTrue(IGOTools.isValidIGOSampleId("12345_AA_1"));
        assertTrue(IGOTools.isValidIGOSampleId("05500_AA_11_1_1_1"));
        assertTrue(IGOTools.isValidIGOSampleId("05500_11_1_1_1"));
        assertFalse(IGOTools.isValidIGOSampleId("1234_11_1_1_1"));
    }
}