package org.mskcc.limsrest.limsapi.cmoinfo.retriever;

import org.junit.Before;
import org.junit.Test;
import org.mskcc.domain.BankedSample;
import org.mskcc.limsrest.limsapi.cmoinfo.CellLineCmoSampleId;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mskcc.util.TestUtils.assertThrown;


public class CellLineCmoSampleIdResolverTest {
    private CellLineCmoSampleIdResolver cellLineCmoSampleIdResolver = new CellLineCmoSampleIdResolver();
    private BankedSample sample;

    @Before
    public void setUp() throws Exception {
        Map<String, Object> fields = new HashMap<>();
        fields.put(BankedSample.RECORD_ID, 3434l);
        sample = new BankedSample(fields);
    }

    @Test
    public void whenUserSampleIdIsNull_shouldThrowAnException() throws Exception {
        sample.setRequestId("dsda");

        Optional<Exception> exception = assertThrown(() -> cellLineCmoSampleIdResolver.resolve(sample, Collections.emptyList()));

        assertThat(exception.isPresent(), is(true));
    }

    @Test
    public void whenUserSampleIdIsEmpty_shouldThrowAnException() throws Exception {
        sample.setUserSampleId("");

        Optional<Exception> exception = assertThrown(() -> cellLineCmoSampleIdResolver.resolve(sample, Collections.emptyList()));

        assertThat(exception.isPresent(), is(true));
    }

    @Test
    public void whenRequestIdIdIsNull_shouldThrowAnException() throws Exception {
        sample.setUserSampleId("dsda");

        Optional<Exception> exception = assertThrown(() -> cellLineCmoSampleIdResolver.resolve(sample, Collections.emptyList()));

        assertThat(exception.isPresent(), is(true));
    }

    @Test
    public void whenRequestIdIsEmpty_shouldThrowAnException() throws Exception {
        sample.setRequestId("");

        Optional<Exception> exception = assertThrown(() -> cellLineCmoSampleIdResolver.resolve(sample, Collections.emptyList()));

        assertThat(exception.isPresent(), is(true));
    }

    @Test
    public void whenUserSampleIdAndRequestIdAreNotEmpty_shouldReturnCellLineCmoId() throws Exception {
        String userId = "gun48o5";
        String reqId = "fewf3322";

        sample.setUserSampleId(userId);
        sample.setRequestId(reqId);

        CellLineCmoSampleId cmoSampleId = cellLineCmoSampleIdResolver.resolve(sample, Collections.emptyList());

        assertThat(cmoSampleId.getUserSampleId(), is(userId));
        assertThat(cmoSampleId.getRequestId(), is(reqId));
    }
}