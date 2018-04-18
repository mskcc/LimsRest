package org.mskcc.limsrest.limsapi.cmoinfo.converter;

import org.hamcrest.object.IsCompatibleType;
import org.junit.Test;
import org.mskcc.limsrest.limsapi.cmoinfo.cspace.PatientAwareCorrectedCmoIdConverter;
import org.mskcc.limsrest.limsapi.cmoinfo.oldformat.OldCorrectedCmoIdConverter;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.SampleTypeAbbreviationRetriever;
import org.mskcc.util.TestUtils;

import java.util.Optional;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class FormatAwareCorrectedCmoIdConverterFactoryTest {
    private SampleTypeAbbreviationRetriever abbrRetriever = mock(SampleTypeAbbreviationRetriever.class);
    private FormatAwareCorrectedCmoIdConverterFactory formatAwareCorrectedCmoIdConverterFactory = new
            FormatAwareCorrectedCmoIdConverterFactory(abbrRetriever);

    @Test
    public void whenCorrectedCmoIdIsInCspace_shouldReturnCspaceConverter() throws Exception {
        assertConverterType("C-AS456H-G001-d", PatientAwareCorrectedCmoIdConverter.class);
        assertConverterType("C-12345-X341-d", PatientAwareCorrectedCmoIdConverter.class);
        assertConverterType("C-fjnFNJDSN-N045-r", PatientAwareCorrectedCmoIdConverter.class);
        assertConverterType("C-fds324-R666-d", PatientAwareCorrectedCmoIdConverter.class);
    }

    @Test
    public void whenCorrectedCmoIdIsInOldFormat_shouldReturnOldFormatConverter() throws Exception {
        assertConverterType("432d-gd543-gdgfd-12", OldCorrectedCmoIdConverter.class);
        assertConverterType("gsg-gd543-gdgfd-a12s", OldCorrectedCmoIdConverter.class);
        assertConverterType("gsg-gd543-gdgfd-A12s", OldCorrectedCmoIdConverter.class);
        assertConverterType("321TG-gd543-gdgfd-as9fdfd", OldCorrectedCmoIdConverter.class);
        assertConverterType("gsg-gd543-gdgfd-02", OldCorrectedCmoIdConverter.class);
    }

    @Test
    public void whenCorrectedCmoIdIsInNoSpecifiedFormat_shouldThrowAnException() throws Exception {
        assertExceptionThrown("gsg-gd543-12");
        assertExceptionThrown("a12s");
        assertExceptionThrown("12345");
        assertExceptionThrown("gdgfd-02");
    }

    private void assertConverterType(String correctedCmoId, Class<? extends
            CorrectedCmoSampleViewToSampleCmoIdConverter>
            converterClass) {
        CorrectedCmoSampleViewToSampleCmoIdConverter converter = formatAwareCorrectedCmoIdConverterFactory
                .getConverter(correctedCmoId);

        assertThat(converter.getClass(), IsCompatibleType.typeCompatibleWith(converterClass));
    }

    private void assertExceptionThrown(String correctedCmoId) {
        Optional<Exception> exception = TestUtils.assertThrown(() -> formatAwareCorrectedCmoIdConverterFactory
                .getConverter(correctedCmoId));

        assertTrue(exception.isPresent());
        assertThat(exception.get().getClass(), IsCompatibleType.typeCompatibleWith
                (FormatAwareCorrectedCmoIdConverterFactory.UnsupportedCmoIdFormatException.class));
    }

}