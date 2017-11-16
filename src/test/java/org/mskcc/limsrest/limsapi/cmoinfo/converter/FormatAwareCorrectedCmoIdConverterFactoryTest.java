package org.mskcc.limsrest.limsapi.cmoinfo.converter;

import org.hamcrest.object.IsCompatibleType;
import org.junit.Test;
import org.mskcc.limsrest.limsapi.cmoinfo.cspace.CspaceCorrectedCmoIdConverter;
import org.mskcc.limsrest.limsapi.cmoinfo.noformat.NoFormatCorrectedCmoIdConverter;
import org.mskcc.limsrest.limsapi.cmoinfo.oldformat.OldCorrectedCmoIdConverter;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.SampleAbbreviationRetriever;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class FormatAwareCorrectedCmoIdConverterFactoryTest {
    private SampleAbbreviationRetriever abbrRetriever = mock(SampleAbbreviationRetriever.class);
    private FormatAwareCorrectedCmoIdConverterFactory formatAwareCorrectedCmoIdConverterFactory = new
            FormatAwareCorrectedCmoIdConverterFactory(abbrRetriever);

    @Test
    public void whenCorrectedCmoIdIsInCspace_shouldReturnCspaceConverter() throws Exception {
        assertConverterType("C-AS456H-G001-d", CspaceCorrectedCmoIdConverter.class);
        assertConverterType("C-12345-X341-d", CspaceCorrectedCmoIdConverter.class);
        assertConverterType("C-fjnFNJDSN-N045-r", CspaceCorrectedCmoIdConverter.class);
        assertConverterType("C-fds324-R666-d", CspaceCorrectedCmoIdConverter.class);
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
    public void whenCorrectedCmoIdIsInNoSpecifiedFormat_shouldReturnNoFormatConverter() throws Exception {
        assertConverterType("gsg-gd543-12", NoFormatCorrectedCmoIdConverter.class);
        assertConverterType("a12s", NoFormatCorrectedCmoIdConverter.class);
        assertConverterType("12345", NoFormatCorrectedCmoIdConverter.class);
        assertConverterType("gdgfd-02", NoFormatCorrectedCmoIdConverter.class);
    }

    private void assertConverterType(String correctedCmoId, Class<? extends StringToSampleCmoIdConverter>
            converterClass) {
        StringToSampleCmoIdConverter converter = formatAwareCorrectedCmoIdConverterFactory.getConverter(correctedCmoId);

        assertThat(converter.getClass(), IsCompatibleType.typeCompatibleWith(converterClass));
    }

}