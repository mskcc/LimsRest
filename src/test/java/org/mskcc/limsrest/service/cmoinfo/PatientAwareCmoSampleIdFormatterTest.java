package org.mskcc.limsrest.service.cmoinfo;

import org.junit.Test;
import org.mskcc.limsrest.service.cmoinfo.patientsample.PatientAwareCmoSampleId;
import org.mskcc.limsrest.service.cmoinfo.patientsample.PatientCmoSampleIdFormatter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PatientAwareCmoSampleIdFormatterTest {
    private PatientCmoSampleIdFormatter patientCmoSampleIdFormatter = new PatientCmoSampleIdFormatter();

    @Test
    public void whenAllCmoIdPropertiesArePresent_shouldReturnFormattedSampleCmoId() throws Exception {
        assertFormatterCmoSampleId("C-1234", "T", 1, "d", "C-1234-T001-d");
        assertFormatterCmoSampleId("C-123456", "N", 2, "d", "C-123456-N002-d");
        assertFormatterCmoSampleId("C-6565", "T", 34, "d", "C-6565-T034-d");
        assertFormatterCmoSampleId("C-76", "T", 682, "d", "C-76-T682-d");

        assertFormatterCmoSampleId("C-543", "T", 5, "r", "C-543-T005-r");
        assertFormatterCmoSampleId("C-54354", "T", 5, "r", "C-54354-T005-r");
        assertFormatterCmoSampleId("C-6543", "T", 5, "r", "C-6543-T005-r");

        assertFormatterCmoSampleId("C-5443", "T", 5, "r", "C-5443-T005-r");
        assertFormatterCmoSampleId("C-54534", "T", 5, "r", "C-54534-T005-r");
    }


    private void assertFormatterCmoSampleId(String patientId, String sampleClass, int sampleCount, String nucleicAcid, String expected) {
        //given
        PatientAwareCmoSampleId patientAwareCmoSampleId = new PatientAwareCmoSampleId(patientId, sampleClass,
                sampleCount, nucleicAcid);

        //when
        String formattedCmoSampleId = patientCmoSampleIdFormatter.format(patientAwareCmoSampleId);

        //then
        assertThat(formattedCmoSampleId, is(expected));
    }
}