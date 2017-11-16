package org.mskcc.limsrest.limsapi.cmoinfo.retriever;

import org.junit.Before;
import org.junit.Test;
import org.mskcc.domain.CorrectedCmoSampleView;
import org.mskcc.limsrest.limsapi.cmoinfo.cellline.CellLineCmoSampleId;
import org.mskcc.limsrest.limsapi.cmoinfo.formatter.CmoSampleIdFormatter;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FormattedCmoSampleIdRetrieverTest {
    private FormattedCmoSampleIdRetriever formattedCmoSampleIdRetriever;
    private CmoSampleIdResolver resolver;
    private CmoSampleIdFormatter formatter;

    @Before
    public void setUp() throws Exception {
        resolver = mock(CmoSampleIdResolver.class);
        formatter = mock(CmoSampleIdFormatter.class);

        formattedCmoSampleIdRetriever = new FormattedCmoSampleIdRetriever(resolver, formatter);
    }

    @Test
    public void whenRetrieverIsInvoked_shouldReturnCmoSampleId() throws Exception {
        //given
        CorrectedCmoSampleView sample = new CorrectedCmoSampleView("sampleId");
        List<CorrectedCmoSampleView> patientSamples = new ArrayList<>();

        CellLineCmoSampleId cmoSampleId = new CellLineCmoSampleId("dds", "ffefe");
        when(resolver.resolve(any(), any(), any())).thenReturn(cmoSampleId);

        String cmoSampleIdString = "bdjewbdewi";
        when(formatter.format(cmoSampleId)).thenReturn(cmoSampleIdString);

        //when
        String cmoId = formattedCmoSampleIdRetriever.retrieve(sample, patientSamples, "12345");

        //then
        assertThat(cmoId, is(cmoSampleIdString));
    }
}