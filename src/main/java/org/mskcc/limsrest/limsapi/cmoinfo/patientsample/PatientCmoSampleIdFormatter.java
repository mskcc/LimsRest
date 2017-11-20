package org.mskcc.limsrest.limsapi.cmoinfo.patientsample;

import org.apache.commons.lang3.StringUtils;
import org.mskcc.limsrest.limsapi.cmoinfo.formatter.CmoSampleIdFormatter;

import java.util.LinkedList;
import java.util.List;

public class PatientCmoSampleIdFormatter implements CmoSampleIdFormatter<PatientCmoSampleId> {
    private static final String CMO_SAMPLE_ID_PREFIX = "C";
    private static final String CMO_SAMPLE_ID_DELIMITER = "-";

    @Override
    public String format(PatientCmoSampleId patientCmoSampleId) {
        List<String> cmoSampleIdProps = new LinkedList<>();

        cmoSampleIdProps.add(CMO_SAMPLE_ID_PREFIX);
        cmoSampleIdProps.add(patientCmoSampleId.getPatientId());
        cmoSampleIdProps.add(patientCmoSampleId.getSampleTypeAbbr() + String.format("%03d", patientCmoSampleId
                .getSampleCount()));
        cmoSampleIdProps.add(patientCmoSampleId.getNucleicAcid());

        return StringUtils.join(cmoSampleIdProps, CMO_SAMPLE_ID_DELIMITER);
    }
}
