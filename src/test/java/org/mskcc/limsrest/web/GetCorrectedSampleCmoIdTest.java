package org.mskcc.limsrest.web;

import org.junit.Before;
import org.junit.Test;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.GenerateSampleCmoIdTask;
import org.mskcc.limsrest.staticstrings.Constants;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

public class GetCorrectedSampleCmoIdTest {
    private GetCorrectedSampleCmoId getCorrectedSampleCmoId;

    @Before
    public void setUp() throws Exception {
        getCorrectedSampleCmoId = new GetCorrectedSampleCmoId(mock(ConnectionQueue.class), mock
                (GenerateSampleCmoIdTask.class));
    }

    @Test
    public void whenSampleIgoIdIsInIncorrectFormat_shouldThrowAnException() throws Exception {
        ResponseEntity<String> responseEntity = getCorrectedSampleCmoId.getSampleCmoIdByIgoId("I will crash your " +
                "system. BUAHAHAHAHA");

        List<String> strings = responseEntity.getHeaders().get(Constants.ERRORS);
        assertThat(strings.size(), is(1));
        assertThat(strings.get(0).contains("IncorrectSampleIgoIdFormatException"), is(true));
    }
}