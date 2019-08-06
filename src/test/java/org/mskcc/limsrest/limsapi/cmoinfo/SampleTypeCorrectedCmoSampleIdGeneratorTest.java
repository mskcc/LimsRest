package org.mskcc.limsrest.limsapi.cmoinfo;

import org.junit.Test;
import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.CmoSampleIdRetrieverFactory;
import org.springframework.context.annotation.PropertySource;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

@PropertySource("classpath:application-test.properties")
public class SampleTypeCorrectedCmoSampleIdGeneratorTest {
    private CmoSampleIdRetrieverFactory factory = mock(CmoSampleIdRetrieverFactory.class);

    @Test
    public void whenCmoSampleIdIsSameAsBefore_shouldNotOverrideIt() throws Exception {
        CorrectedCmoSampleView correctedCmoSampleView = new CorrectedCmoSampleView("sampleId");
        String currentId = "C-123456-X001-d";
        correctedCmoSampleView.setCorrectedCmoId(currentId);

        assertFalse(SampleTypeCorrectedCmoSampleIdGenerator.shouldOverrideCmoId(correctedCmoSampleView, "C-123456-X002-d"));
    }
}