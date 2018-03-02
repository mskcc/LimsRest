package org.mskcc.limsrest.limsapi.cmoinfo.retriever;

import org.junit.Before;
import org.junit.Test;
import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.domain.sample.BankedSample;
import org.mskcc.limsrest.limsapi.cmoinfo.cellline.CellLineCmoSampleId;
import org.mskcc.limsrest.limsapi.cmoinfo.cellline.CellLineCmoSampleIdResolver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class CellLineCmoSampleIdResolverTest {
    private CellLineCmoSampleIdResolver cellLineCmoSampleIdResolver = new CellLineCmoSampleIdResolver();
    private CorrectedCmoSampleView sample;

    @Before
    public void setUp() throws Exception {
        Map<String, Object> fields = new HashMap<>();
        fields.put(BankedSample.RECORD_ID, 3434l);
    }

    @Test
    public void whenUserSampleIdIsNotEmpty_shouldReturnCellLineCmoIdWithRequestIdProvided() throws Exception {
        assertCellLineSampleId("gun48o5", "65432_P", "65432P");
        assertCellLineSampleId("1234_P_1", "65432_P", "65432P");
        assertCellLineSampleId("1234_P_1", "65432", "65432");
        assertCellLineSampleId("1234_P_1", "1234_P_A", "1234PA");
    }

    private void assertCellLineSampleId(String sampleId, String requestId, String expectedRequestId) {
        String bankedSampleRequestId = "fewf3322";

        sample = new CorrectedCmoSampleView(sampleId);
        sample.setRequestId(bankedSampleRequestId);
        sample.setSampleId(sampleId);

        CellLineCmoSampleId cmoSampleId = cellLineCmoSampleIdResolver.resolve(sample, Collections.emptyList(),
                requestId);

        assertThat(cmoSampleId.getSampleId(), is(sampleId));
        assertThat(cmoSampleId.getRequestId(), is(expectedRequestId));
    }
}