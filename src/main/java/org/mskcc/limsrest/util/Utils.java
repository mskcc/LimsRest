package org.mskcc.limsrest.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.sample.NucleicAcid;
import org.mskcc.limsrest.limsapi.PatientSamplesWithCmoInfoRetriever;

import java.util.Optional;

public class Utils {
    private final static Log LOGGER = LogFactory.getLog(PatientSamplesWithCmoInfoRetriever.class);

    public static Optional<NucleicAcid> getOptionalNucleicAcid(String nucleicAcid, String sampleId) {
        try {
            return Optional.of(NucleicAcid.fromValue(nucleicAcid));
        } catch (Exception e) {
            LOGGER.warn(String.format("Nucleic acid for sample %s is empty. For some sample types cmo sample " +
                    "id won't be able to be generated", sampleId));

            return Optional.empty();
        }
    }
}
