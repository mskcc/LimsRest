package org.mskcc.limsrest.limsapi.cmoinfo.cellline;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CellLineCmoSampleIdFormatterTest {
    private CellLineCmoSampleIdFormatter cellLineCmoSampleIdFormatter = new CellLineCmoSampleIdFormatter();

    @Test
    public void whenSampleIdAndRequestIdAreNotNullNorEmpty_shouldReturnFormattedCmoId() throws Exception {
        assertCmoSampleId("2144_T_1", "43432_U", "2144_T_1-43432_U");
        assertCmoSampleId("a", "b", "a-b");
        assertCmoSampleId("abd_5_T", "123_U", "abd_5_T-123_U");
    }

    private void assertCmoSampleId(String sampleId, String requestId, String expected) {
        CellLineCmoSampleId cmoSampleId = new CellLineCmoSampleId(sampleId, requestId);

        String formattedId = cellLineCmoSampleIdFormatter.format(cmoSampleId);

        assertThat(formattedId, is(expected));
    }
}