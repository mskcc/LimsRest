package org.mskcc.limsrest.limsapi.cmoinfo.cspace;

import org.hamcrest.object.IsCompatibleType;
import org.junit.Test;
import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.util.TestUtils;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PatientAwareCorrectedCmoIdConverterTest {
    private PatientAwareCorrectedCmoIdConverter patientAwareCorrectedCmoIdConverter = new
            PatientAwareCorrectedCmoIdConverter();

    @Test
    public void whenCmoIdStringIsEmpty_shouldThrowAnException() throws Exception {
        CorrectedCmoSampleView sample = getSample("sampleId");
        Optional<Exception> exception = TestUtils.assertThrown(() -> {
            patientAwareCorrectedCmoIdConverter.convert(sample);
        });

        assertThat(exception.isPresent(), is(true));
        assertThat(exception.get().getClass(), IsCompatibleType.typeCompatibleWith(IllegalArgumentException.class));
    }

    @Test
    public void whenCmoIdStringIsNotCorrect_shouldThrowAnException() throws Exception {
        assertExceptionThrown("D-12345-N001-d");
        assertExceptionThrown("-12345-N001-d");

        assertExceptionThrown("C-N001-d");
        assertExceptionThrown("C-12345-d");
        assertExceptionThrown("C-12345-N001");

        assertExceptionThrown("C12345-N001-d");
        assertExceptionThrown("C12345N001d");

        assertExceptionThrown("C_12345-N001-d");
        assertExceptionThrown("C-12345_N001-d");
        assertExceptionThrown("C-12345-N001_d");
        assertExceptionThrown("C_12345_N001_d");

        assertExceptionThrown("C-12345-001-d");
        assertExceptionThrown("C-12345-N-d");
        assertExceptionThrown("C-12345-N2-d");
        assertExceptionThrown("C-12345-N34-d");
        assertExceptionThrown("C-12345-1134-d");

        assertExceptionThrown("C-12345-N034-w");
        assertExceptionThrown("C-12345-N034-e");
        assertExceptionThrown("C-12345-N034-d1");
        assertExceptionThrown("C-12345-N034-r1");
        assertExceptionThrown("C-12345-N034-r2");
    }

    private void assertExceptionThrown(String incorrectCmoId) {
        Optional<Exception> exception = TestUtils.assertThrown(() -> {
            CorrectedCmoSampleView sample = getSample(incorrectCmoId);
            patientAwareCorrectedCmoIdConverter.convert(sample);
        });

        assertThat(exception.isPresent(), is(true));
        assertThat(exception.get().getClass(), IsCompatibleType.typeCompatibleWith(IllegalArgumentException.class));
    }

    private CorrectedCmoSampleView getSample(String incorrectCmoId) {
        CorrectedCmoSampleView sample = new CorrectedCmoSampleView("sampleId");
        sample.setCorrectedCmoId(incorrectCmoId);
        return sample;
    }

    @Test
    public void whenCmoIdStringDoesntStartWithPrefix_shouldThrowAnException() throws Exception {
        Optional<Exception> exception = TestUtils.assertThrown(() -> patientAwareCorrectedCmoIdConverter.convert
                (getSample
                ("D-12345-X001-d1")));

        assertThat(exception.isPresent(), is(true));
        assertThat(exception.get().getClass(), IsCompatibleType.typeCompatibleWith(IllegalArgumentException.class));
    }

}