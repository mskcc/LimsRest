package org.mskcc.limsrest.service;

import org.junit.Test;

import static org.junit.Assert.*;

public class GetRequestSamplesTaskTest {

    @Test
    public void isIMPACTOrHEMEPACTBeforeIMPACT505() {
        assertFalse(GetRequestSamplesTask.isIMPACTOrHEMEPACTBeforeIMPACT505("IMPACT505"));
        assertTrue(GetRequestSamplesTask.isIMPACTOrHEMEPACTBeforeIMPACT505("IMPACT468"));
        assertTrue(GetRequestSamplesTask.isIMPACTOrHEMEPACTBeforeIMPACT505("IMPACT410"));
    }
}