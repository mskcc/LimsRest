package org.mskcc.limsrest.limsapi.cmoinfo;

import org.junit.Test;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientCmoSampleId;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientCmoSampleIdFormatter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PatientCmoSampleIdFormatterTest {
    private PatientCmoSampleIdFormatter patientCmoSampleIdFormatter = new PatientCmoSampleIdFormatter();

    @Test
    public void whenAllCmoIdPropertiesArePresent_shouldReturnFormattedSampleCmoId() throws Exception {
        assertFormatterCmoSampleId("P1", "T", 1, "d", "C-P1-T001-d");
        assertFormatterCmoSampleId("P543", "N", 2, "d", "C-P543-N002-d");
        assertFormatterCmoSampleId("P43TD", "T", 34, "d", "C-P43TD-T034-d");
        assertFormatterCmoSampleId("P43TD", "T", 682, "d", "C-P43TD-T682-d");

        assertFormatterCmoSampleId("P43TD", "T", 5, "r", "C-P43TD-T005-r");
        assertFormatterCmoSampleId("P43TD", "T", 5, "r", "C-P43TD-T005-r");
        assertFormatterCmoSampleId("P43TD", "T", 5, "r", "C-P43TD-T005-r");

        assertFormatterCmoSampleId("P43TD", "T", 5, "r", "C-P43TD-T005-r");
        assertFormatterCmoSampleId("P43TD", "T", 5, "r", "C-P43TD-T005-r");
    }


    private void assertFormatterCmoSampleId(String patientId, String sampleClass, int sampleCount, String nucleicAcid, String expected) {
        //given
        PatientCmoSampleId patientCmoSampleId = new PatientCmoSampleId(patientId, sampleClass, sampleCount, nucleicAcid);

        //when
        String formattedCmoSampleId = patientCmoSampleIdFormatter.format(patientCmoSampleId);

        //then
        assertThat(formattedCmoSampleId, is(expected));
    }
}