package org.mskcc.limsrest.service;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.*;

public class GetRequestSamplesTaskTest {

    @Test
    public void isIMPACTOrHEMEPACT() {
        assertFalse(GetRequestSamplesTask.isIMPACTOrHEMEPACT(""));
        assertTrue(GetRequestSamplesTask.isIMPACTOrHEMEPACT("IMPACT505"));
        assertTrue(GetRequestSamplesTask.isIMPACTOrHEMEPACT("IMPACT468"));
        assertTrue(GetRequestSamplesTask.isIMPACTOrHEMEPACT("IMPACT410"));
    }
}