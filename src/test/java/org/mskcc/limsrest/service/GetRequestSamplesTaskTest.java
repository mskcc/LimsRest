package org.mskcc.limsrest.service;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.*;

public class GetRequestSamplesTaskTest {

    @Test
    public void isIMPACTOrHEMEPACT() {
        assertFalse(GetRequestSamplesTask.isIMPACTOrHEMEPACT(""));
        assertTrue(GetRequestSamplesTask.isIMPACTOrHEMEPACT("HC_IMPACT"));
    }
}