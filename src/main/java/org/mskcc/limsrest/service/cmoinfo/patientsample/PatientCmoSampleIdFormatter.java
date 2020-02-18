package org.mskcc.limsrest.service.cmoinfo.patientsample;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.service.cmoinfo.formatter.CmoSampleIdFormatter;

public class PatientCmoSampleIdFormatter implements CmoSampleIdFormatter<PatientAwareCmoSampleId> {
    private final static Log LOGGER = LogFactory.getLog(PatientCmoSampleIdFormatter.class);

    @Override
    public String format(PatientAwareCmoSampleId patientCmoSampleId) {
        String cmoSampleName = patientCmoSampleId.getPatientId() + "-" +
                patientCmoSampleId.getSampleTypeAbbr() + String.format("%03d", patientCmoSampleId.getSampleCount()) + "-"
                + patientCmoSampleId.getNucleicAcid();

        LOGGER.info(String.format("Formatted CMO Sample Id: %s for NON CellLine sample: %s", cmoSampleName,
                patientCmoSampleId));
        return cmoSampleName;
    }
}