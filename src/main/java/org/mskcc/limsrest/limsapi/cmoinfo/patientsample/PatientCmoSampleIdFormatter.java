package org.mskcc.limsrest.limsapi.cmoinfo.patientsample;

import org.apache.commons.lang3.StringUtils;
import org.mskcc.limsrest.limsapi.cmoinfo.formatter.CmoSampleIdFormatter;

import java.util.LinkedList;
import java.util.List;

public class PatientCmoSampleIdFormatter implements CmoSampleIdFormatter<PatientAwareCmoSampleId> {
    private static final String CMO_SAMPLE_ID_PREFIX = "C";
    private static final String CMO_SAMPLE_ID_DELIMITER = "-";

    @Override
    public String format(PatientAwareCmoSampleId patientAwareCmoSampleId) {
        List<String> cmoSampleIdProps = new LinkedList<>();

        cmoSampleIdProps.add(CMO_SAMPLE_ID_PREFIX);
        cmoSampleIdProps.add(patientAwareCmoSampleId.getPatientId());
        cmoSampleIdProps.add(patientAwareCmoSampleId.getSampleTypeAbbr() + String.format("%03d", patientAwareCmoSampleId
                .getSampleCount()));
        cmoSampleIdProps.add(patientAwareCmoSampleId.getNucleicAcid());

        return StringUtils.join(cmoSampleIdProps, CMO_SAMPLE_ID_DELIMITER);
    }
}
