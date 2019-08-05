package org.mskcc.limsrest.limsapi.dmp;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class OncotreeTumorTypeRetrieverTest {

    @Test
    public void retrieve() {
        OncotreeTumorTypeRetriever r = new OncotreeTumorTypeRetriever();
        Set<TumorType> tumorTypes = r.retrieve();
        assertTrue(tumorTypes.contains(new TumorType("GB", "Brain_CNS", "Glioblastoma")));
    }
}