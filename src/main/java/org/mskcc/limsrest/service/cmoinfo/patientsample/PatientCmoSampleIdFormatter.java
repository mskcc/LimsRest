package org.mskcc.limsrest.service.cmoinfo.patientsample;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.service.cmoinfo.formatter.CmoSampleIdFormatter;

import java.util.LinkedList;
import java.util.List;

public class PatientCmoSampleIdFormatter implements CmoSampleIdFormatter<PatientAwareCmoSampleId> {
    private final static Log LOGGER = LogFactory.getLog(PatientCmoSampleIdFormatter.class);
    private static final String CMO_SAMPLE_ID_DELIMITER = "-";

    @Override
    public String format(PatientAwareCmoSampleId patientCmoSampleId) {
        List<String> cmoSampleIdProps = new LinkedList<>();

        cmoSampleIdProps.add(patientCmoSampleId.getPatientId());
        cmoSampleIdProps.add(patientCmoSampleId.getSampleTypeAbbr() + String.format("%03d", patientCmoSampleId
                .getSampleCount()));
        cmoSampleIdProps.add(patientCmoSampleId.getNucleicAcid());

        String formatted = StringUtils.join(cmoSampleIdProps, CMO_SAMPLE_ID_DELIMITER);

        LOGGER.info(String.format("Formatted CMO Sample Id: %s for NON CellLine sample: %s", formatted,
                patientCmoSampleId));
        return formatted;
    }
}
