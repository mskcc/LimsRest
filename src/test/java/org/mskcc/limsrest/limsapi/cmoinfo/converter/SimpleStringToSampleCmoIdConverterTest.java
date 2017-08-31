package org.mskcc.limsrest.limsapi.cmoinfo.converter;

import org.hamcrest.object.IsCompatibleType;
import org.junit.Test;
import org.mskcc.util.TestUtils;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SimpleStringToSampleCmoIdConverterTest {
    private SimpleStringToSampleCmoIdConverter simpleStringToSampleCmoIdConverter = new SimpleStringToSampleCmoIdConverter();

    @Test
    public void whenCmoIdStringIsEmpty_shouldThrowAnException() throws Exception {
        Optional<Exception> exception = TestUtils.assertThrown(() -> simpleStringToSampleCmoIdConverter.convert(""));

        assertThat(exception.isPresent(), is(true));
        assertThat(exception.get().getClass(), IsCompatibleType.typeCompatibleWith(IllegalArgumentException.class));
    }

    @Test
    public void whenCmoIdStringIsNotCorrect_shouldThrowAnException() throws Exception {
        assertExceptionThrown("D-12345-N001-d1");
        assertExceptionThrown("-12345-N001-d1");

        assertExceptionThrown("C-N001-d1");
        assertExceptionThrown("C-12345-d1");
        assertExceptionThrown("C-12345-N001");

        assertExceptionThrown("C12345-N001-d1");
        assertExceptionThrown("C12345N001d1");

        assertExceptionThrown("C_12345-N001-d1");
        assertExceptionThrown("C-12345_N001-d1");
        assertExceptionThrown("C-12345-N001_d1");

        assertExceptionThrown("C-12345-001-d1");
        assertExceptionThrown("C-12345-N-d1");
        assertExceptionThrown("C-12345-N2-d1");
        assertExceptionThrown("C-12345-N34-d1");
        assertExceptionThrown("C-12345-1134-d1");

        assertExceptionThrown("C-12345-N034-w1");
        assertExceptionThrown("C-12345-N034-e1");
    }

    private void assertExceptionThrown(String incorrectCmoId) {
        Optional<Exception> exception = TestUtils.assertThrown(() -> {
            simpleStringToSampleCmoIdConverter.convert(incorrectCmoId);
        });

        assertThat(exception.isPresent(), is(true));
        assertThat(exception.get().getClass(), IsCompatibleType.typeCompatibleWith(IllegalArgumentException.class));
    }

    @Test
    public void whenCmoIdStringDoesntStartWithPrefix_shouldThrowAnException() throws Exception {
        Optional<Exception> exception = TestUtils.assertThrown(() -> simpleStringToSampleCmoIdConverter.convert("D-12345-X001-d1"));

        assertThat(exception.isPresent(), is(true));
        assertThat(exception.get().getClass(), IsCompatibleType.typeCompatibleWith(IllegalArgumentException.class));
    }

}