package org.mskcc.limsrest.limsapi.cmoinfo;

import org.hamcrest.object.IsCompatibleType;
import org.junit.Test;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientCmoSampleId;
import org.mskcc.util.TestUtils;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CmoSampleIdTest {
    @Test
    public void whenSampleCountExceedsRange_shouldThrowAnException() throws Exception {
        assertExceptionThrownOnCountRangeExceeded(1000);
        assertExceptionThrownOnCountRangeExceeded(543342);

        assertExceptionNotThrownOnCountRangeExceeded(999);
        assertExceptionNotThrownOnCountRangeExceeded(1);
    }

    private void assertExceptionNotThrownOnCountRangeExceeded(int sampleCount) {
        Optional<Exception> exception = TestUtils.assertThrown(() -> new PatientCmoSampleId("P1", "L", sampleCount, "r"));

        assertThat(exception.isPresent(), is(false));
    }

    private void assertExceptionThrownOnCountRangeExceeded(int sampleCount) {
        Optional<Exception> exception = TestUtils.assertThrown(() -> new PatientCmoSampleId("P1", "L", sampleCount, "r"));

        assertThat(exception.isPresent(), is(true));
        assertThat(exception.get().getClass(), IsCompatibleType.typeCompatibleWith(IllegalArgumentException.class));
    }
}