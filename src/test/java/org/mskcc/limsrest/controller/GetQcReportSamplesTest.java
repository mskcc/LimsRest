package org.mskcc.limsrest.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.QcReportSampleList;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;


@RunWith(SpringRunner.class)
public class GetQcReportSamplesTest {

    private GetQcReportSamples getQcReportSamples;

    @Before
    public void setUp() throws Exception {
        getQcReportSamples = new GetQcReportSamples(mock(ConnectionLIMS.class));
    }

    @Test(expected = ResponseStatusException.class)
    public void whenRequestIdIsInIncorrectFormat_shouldThrowAnException() throws Exception {

        List<String> samples = new ArrayList<>();
        samples.add("sample1");
        samples.add("sample2");

        getQcReportSamples.getQcReportSamples("req", samples);
    }

}

