package org.mskcc.limsrest.service.cmoinfo;

import org.junit.Test;
import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.limsrest.service.cmoinfo.converter.CorrectedCmoIdConverterFactory;
import org.mskcc.limsrest.service.cmoinfo.converter.CorrectedCmoSampleViewToSampleCmoIdConverter;
import org.mskcc.limsrest.service.cmoinfo.cspace.PatientAwareCorrectedCmoIdConverter;
import org.mskcc.limsrest.service.cmoinfo.retriever.IncrementalSampleCounterRetriever;
import org.mskcc.util.TestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.typeCompatibleWith;

public class IncrementalSampleCounterRetrieverTest {
    private CorrectedCmoSampleView correctedCmoSampleView = getCorrectedCmoSampleView();

    private CorrectedCmoIdConverterFactory converterFactory = new ConverterFactoryMock();

    private IncrementalSampleCounterRetriever incrementalSampleCounterRetriever = new
            IncrementalSampleCounterRetriever(converterFactory);
    @Test
    public void whenSampleCmoIdListIsEmpty_shouldReturnOne() throws Exception {
        List<CorrectedCmoSampleView> sampleCmoIds = Collections.emptyList();

        int sampleCount = incrementalSampleCounterRetriever.retrieve(correctedCmoSampleView, sampleCmoIds, "X");

        assertThat(sampleCount, is(1));
    }

    @Test
    public void whenSampleCmoIdListContainsOneElementWithOtherClass_shouldReturnOne() throws Exception {
        List<CorrectedCmoSampleView> sampleCmoIds = Arrays.asList(getSample("C-123456-L001-d"));
        String sampleClassAbbr = "X";

        int sampleCount = incrementalSampleCounterRetriever.retrieve(correctedCmoSampleView, sampleCmoIds, sampleClassAbbr);

        assertThat(sampleCount, is(1));
    }

    private CorrectedCmoSampleView getSample(String correctedCmoId) {
        CorrectedCmoSampleView sample = new CorrectedCmoSampleView("sampleId");
        sample.setCorrectedCmoId(correctedCmoId);
        return sample;
    }

    @Test
    public void whenSampleCmoIdListContainsMultipleElementsWithOtherClasses_shouldReturnOne() throws Exception {
        List<CorrectedCmoSampleView> sampleCmoIds = Arrays.asList(getSample("C-123456-L001-d"), getSample
                ("C-123456-L002-d"), getSample("C-123456-T001-d"), getSample("C-123456-N001-d"));
        String sampleClassAbbr = "X";

        int sampleCount = incrementalSampleCounterRetriever.retrieve(correctedCmoSampleView, sampleCmoIds, sampleClassAbbr);

        assertThat(sampleCount, is(1));
    }

    @Test
    public void whenSampleCmoIdListContainsSampleWithSameClassAndOthers_shouldReturnIncrementedSameClassCounter()
            throws Exception {
        List<CorrectedCmoSampleView> sampleCmoIds = Arrays.asList(getSample("C-123456-X003-d"), getSample
                ("C-123456-L001-d"), getSample("C-123456-L002-d"), getSample("C-123456-T001-d"), getSample
                ("C-123456-N001-d"));
        String sampleClassAbbr = "X";

        int sampleCount = incrementalSampleCounterRetriever.retrieve(correctedCmoSampleView, sampleCmoIds, sampleClassAbbr);

        assertThat(sampleCount, is(4));
    }

    @Test
    public void whenSampleCmoIdListContainsOneElementWithGivenClass_shouldReturnTwo() throws Exception {
        List<CorrectedCmoSampleView> sampleCmoIds = Arrays.asList(getSample("C-123456-N001-d"));
        String sampleClassAbbr = "N";

        int sampleCount = incrementalSampleCounterRetriever.retrieve(correctedCmoSampleView, sampleCmoIds, sampleClassAbbr);

        assertThat(sampleCount, is(2));
    }

    @Test
    public void whenSampleCmoIdListContainsElementWithSampleCountSix_shouldReturnSeven() throws Exception {
        List<CorrectedCmoSampleView> sampleCmoIds = Arrays.asList(getSample("C-123456-N006-d"));
        String sampleClassAbbr = "N";

        int sampleCount = incrementalSampleCounterRetriever.retrieve(correctedCmoSampleView, sampleCmoIds, sampleClassAbbr);

        assertThat(sampleCount, is(7));
    }

    @Test
    public void whenSampleCmoIdListContainsElementWithTwoDigitSampleCount_shouldReturnThisCountPlusOne() throws
            Exception {
        List<CorrectedCmoSampleView> sampleCmoIds = Arrays.asList(getSample("C-123456-N010-d"));
        String sampleClassAbbr = "N";

        int sampleCount = incrementalSampleCounterRetriever.retrieve(correctedCmoSampleView, sampleCmoIds, sampleClassAbbr);

        assertThat(sampleCount, is(11));
    }

    @Test
    public void whenSampleCmoIdListContainsElementWithThreeDigitSampleCount_shouldReturnThisCountPlusOne() throws
            Exception {
        List<CorrectedCmoSampleView> sampleCmoIds = Arrays.asList(getSample("C-123456-N654-d"));
        String sampleClassAbbr = "N";

        int sampleCount = incrementalSampleCounterRetriever.retrieve(correctedCmoSampleView, sampleCmoIds, sampleClassAbbr);

        assertThat(sampleCount, is(655));
    }

    @Test
    public void whenSampleCmoIdListContainsElementWithMaxCountValue_shouldThrowException() throws Exception {
        List<CorrectedCmoSampleView> sampleCmoIds = Arrays.asList(getSample("C-123456-N999-d"));
        String sampleClassAbbr = "N";

        Optional<Exception> exception = TestUtils.assertThrown(() -> incrementalSampleCounterRetriever.retrieve
                (correctedCmoSampleView, sampleCmoIds, sampleClassAbbr));

        assertThat(exception.isPresent(), is(true));
        assertThat(exception.get().getClass(), typeCompatibleWith(IncrementalSampleCounterRetriever
                .SampleCounterOverflowException.class));
    }

    @Test
    public void whenSampleCmoIdListContainsOneElementWithEmptyCmoId_shouldReturnOne() throws Exception {
        List<CorrectedCmoSampleView> sampleCmoIds = Arrays.asList(getSample(""));
        String sampleClassAbbr = "N";

        int sampleCount = incrementalSampleCounterRetriever.retrieve(correctedCmoSampleView, sampleCmoIds, sampleClassAbbr);

        assertThat(sampleCount, is(1));
    }

    @Test
    public void whenSampleCmoIdListContainsOneElementWithNullCmoId_shouldReturnOne() throws Exception {
        List<CorrectedCmoSampleView> sampleCmoIds = Arrays.asList(getSample(null));
        String sampleClassAbbr = "N";

        int sampleCount = incrementalSampleCounterRetriever.retrieve(correctedCmoSampleView, sampleCmoIds, sampleClassAbbr);

        assertThat(sampleCount, is(1));
    }

    @Test
    public void whenCounterIsAlreadySet_shouldUseThatOne() throws Exception {
        //given
        List<CorrectedCmoSampleView> sampleCmoIds = Arrays.asList(getSample("C-123456-N123-d"));
        String sampleClassAbbr = "N";

        //when
        int counter = 3;
        int sampleCount = incrementalSampleCounterRetriever.retrieve(getSampleViewWithCounter(counter), sampleCmoIds, sampleClassAbbr);

        //then
        assertThat(sampleCount, is(counter));
    }

    private CorrectedCmoSampleView getSampleViewWithCounter(int counter) {
        CorrectedCmoSampleView correctedCmoSampleView = new CorrectedCmoSampleView("id3");
        correctedCmoSampleView.setCounter(counter);

        return correctedCmoSampleView;
    }

    private CorrectedCmoSampleView getCorrectedCmoSampleView() {
        CorrectedCmoSampleView correctedCmoSampleView = new CorrectedCmoSampleView("id");


        return correctedCmoSampleView;
    }

    private class ConverterFactoryMock implements CorrectedCmoIdConverterFactory {

        @Override
        public CorrectedCmoSampleViewToSampleCmoIdConverter getConverter(String correctedCmoSampleId) {
            return new PatientAwareCorrectedCmoIdConverter();
        }
    }
}
