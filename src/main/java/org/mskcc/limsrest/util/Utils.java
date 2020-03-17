package org.mskcc.limsrest.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.sample.NucleicAcid;
import org.mskcc.limsrest.service.PatientSamplesWithCmoInfoRetriever;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

public class Utils {
    private final static Log LOGGER = LogFactory.getLog(Utils.class);

    public static void runAndCatchNpe(Runnable runnable) {
        try {
            runnable.run();
        } catch (NullPointerException var2) {
        }
    }

    public static String requireNonNullNorEmpty(String string, String message) {
        if (string != null && !"".equals(string)) {
            return string;
        } else {
            throw new RuntimeException(message);
        }
    }

    public static Optional<NucleicAcid> getOptionalNucleicAcid(String nucleicAcid, String sampleId) {
        try {
            return Optional.of(NucleicAcid.fromValue(nucleicAcid));
        } catch (Exception e) {
            LOGGER.warn(String.format("Nucleic acid for sample %s is empty. For some sample types cmo sample " +
                    "id won't be able to be generated", sampleId));

            return Optional.empty();
        }
    }

    public static <T> ResponseEntity<T> getResponseEntity(T input, HttpStatus status) {
        ResponseEntity<T> resp = new ResponseEntity<T>(input, status);
        return resp;
    }
}