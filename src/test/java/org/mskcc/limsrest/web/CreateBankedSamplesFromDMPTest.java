package org.mskcc.limsrest.web;

import org.junit.Before;
import org.junit.Test;
import org.mskcc.limsrest.connection.ConnectionPoolLIMS;
import org.mskcc.limsrest.limsapi.dmp.DefaultTodayDateRetriever;
import org.mskcc.limsrest.limsapi.dmp.GenerateBankedSamplesFromDMP;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.FutureTask;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateBankedSamplesFromDMPTest {
    private final GenerateBankedSamplesFromDMP generateBankedSamplesFromDMP = mock
            (GenerateBankedSamplesFromDMP.class);
    private final ConnectionPoolLIMS conn = mock(ConnectionPoolLIMS.class);
    private final DefaultTodayDateRetriever dateRetriever = mock(DefaultTodayDateRetriever.class);
    private CreateBankedSamplesFromDMP createBankedSamplesFromDMP;

    @Before
    public void setUp() throws Exception {
        createBankedSamplesFromDMP = new CreateBankedSamplesFromDMP(conn);
    }

    @Test
    public void whenDateIsNullAndNoErrors_shouldReturnOk() throws Exception {
        when(conn.submitTask(any())).thenReturn(mock(FutureTask.class));

        ResponseEntity<String> response = createBankedSamplesFromDMP.getSampleCmoId(null);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void whenDateIsNullAndCreateBankedThrowsException_shouldReturnNotFound() throws Exception {
        when(generateBankedSamplesFromDMP.call()).thenThrow(Exception.class);
        when(dateRetriever.retrieve(any())).thenCallRealMethod();

        ResponseEntity<String> response = createBankedSamplesFromDMP.getSampleCmoId(null);

        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
    }

    @Test
    public void whenDateIsNullAndDateRetrieverThrowsException_shouldReturnNotFound() throws Exception {
        when(dateRetriever.retrieve(any())).thenThrow(Exception.class);

        ResponseEntity<String> response = createBankedSamplesFromDMP.getSampleCmoId(null);

        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
    }
}
