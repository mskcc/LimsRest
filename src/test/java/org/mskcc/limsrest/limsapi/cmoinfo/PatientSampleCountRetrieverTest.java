package org.mskcc.limsrest.limsapi.cmoinfo;

import org.junit.Test;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.SimpleStringToSampleCmoIdConverter;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.StringToSampleCmoIdConverter;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.PatientSampleCountRetriever;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PatientSampleCountRetrieverTest {
    private StringToSampleCmoIdConverter sampleCmoIdStringToCmoIdConverter = new SimpleStringToSampleCmoIdConverter();
    private PatientSampleCountRetriever patientSampleCountRetriever = new PatientSampleCountRetriever(sampleCmoIdStringToCmoIdConverter);

    @Test
    public void whenSampleCmoIdListIsEmpty_shouldReturnOne() throws Exception {
        List<String> sampleCmoIds = Collections.emptyList();

        long sampleCount = patientSampleCountRetriever.retrieve(sampleCmoIds, "X");

        assertThat(sampleCount, is(1l));
    }

    @Test
    public void whenSampleCmoIdListContainsOneElementWithOtherClass_shouldReturnOne() throws Exception {
        List<String> sampleCmoIds = Arrays.asList("C-123456-L001-d");
        String sampleClassAbbr = "X";

        long sampleCount = patientSampleCountRetriever.retrieve(sampleCmoIds, sampleClassAbbr);

        assertThat(sampleCount, is(1l));
    }

    @Test
    public void whenSampleCmoIdListContainsMutipleElementsWithOtherClasses_shouldReturnOne() throws Exception {
        List<String> sampleCmoIds = Arrays.asList("C-123456-L001-d", "C-123456-L002-d", "C-123456-T001-d", "C-123456-N001-d");
        String sampleClassAbbr = "X";

        long sampleCount = patientSampleCountRetriever.retrieve(sampleCmoIds, sampleClassAbbr);

        assertThat(sampleCount, is(1l));
    }

    @Test
    public void whenSampleCmoIdListContainsOneElementWithGivenClass_shouldReturnTwo() throws Exception {
        List<String> sampleCmoIds = Arrays.asList("C-123456-N001-d");
        String sampleClassAbbr = "N";

        long sampleCount = patientSampleCountRetriever.retrieve(sampleCmoIds, sampleClassAbbr);

        assertThat(sampleCount, is(2l));
    }

    @Test
    public void whenSampleCmoIdListContainsElementWithSampleCountSix_shouldReturnSeven() throws Exception {
        List<String> sampleCmoIds = Arrays.asList("C-123456-N006-d");
        String sampleClassAbbr = "N";

        long sampleCount = patientSampleCountRetriever.retrieve(sampleCmoIds, sampleClassAbbr);

        assertThat(sampleCount, is(7l));
    }
}
