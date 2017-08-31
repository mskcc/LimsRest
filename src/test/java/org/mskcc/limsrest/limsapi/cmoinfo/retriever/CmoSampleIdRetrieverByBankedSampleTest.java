package org.mskcc.limsrest.limsapi.cmoinfo.retriever;

import org.junit.Before;
import org.junit.Test;
import org.mskcc.limsrest.limsapi.cmoinfo.CellLineCmoSampleId;
import org.mskcc.limsrest.limsapi.cmoinfo.formatter.CmoSampleIdFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CmoSampleIdRetrieverByBankedSampleTest {
    private CmoSampleIdRetrieverByBankedSample cmoSampleIdRetrieverByBankedSample;
    private CmoSampleIdResolver resolver;
    private CmoSampleIdFormatter formatter;

    @Before
    public void setUp() throws Exception {
        resolver = mock(CmoSampleIdResolver.class);
        formatter = mock(CmoSampleIdFormatter.class);

        cmoSampleIdRetrieverByBankedSample = new CmoSampleIdRetrieverByBankedSample(resolver, formatter);
    }

    @Test
    public void whenRetrieverIsInvoked_shouldReturnCmoSampleId() throws Exception {
        //given
        Map<String, Object> fields = new HashMap<>();
        List<String> sampleIds = new ArrayList<>();

        CellLineCmoSampleId cmoSampleId = new CellLineCmoSampleId("dds", "ffefe");
        when(resolver.resolve(any(), any())).thenReturn(cmoSampleId);

        String cmoSampleIdString = "bdjewbdewi";
        when(formatter.format(cmoSampleId)).thenReturn(cmoSampleIdString);

        //when
        String cmoId = cmoSampleIdRetrieverByBankedSample.retrieve(fields, sampleIds);

        //then
        assertThat(cmoId, is(cmoSampleIdString));
    }

}