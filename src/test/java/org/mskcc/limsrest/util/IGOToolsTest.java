package org.mskcc.limsrest.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class IGOToolsTest {

    @Test
    public void requestIdFromIgoId() {
        assertEquals("06049_A", IGOTools.requestFromIgoId("06049_A"));
        assertEquals("06049_AA", IGOTools.requestFromIgoId("06049_AA_3_2_1"));
        assertEquals("06049", IGOTools.requestFromIgoId("06049"));
        assertEquals("06049_O", IGOTools.requestFromIgoId("06049_O_28"));
    }
}