package org.mskcc.limsrest.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class IGOTools {

    private static Set<String> EXCEPTIONALLY_VALID_IGO_IDS = new HashSet<String>(
            Arrays.asList("POOLED_05605_AC_1")
    );

    /**
     * Extract the request id from an IGO id like: 06049_AA_1_2_1
     * @param igoId
     * @return the request ID
     */
    public static String requestFromIgoId(String igoId) {
        if (igoId == null)
            return null;
        return igoId.replaceAll("_[0-9]+", "");
    }

    public static boolean isValidIGOSampleId(String igoId) {
        return igoId.matches("\\d\\d\\d\\d\\d(_[A-Z]*)?(_[0-9]*)+") ||
                EXCEPTIONALLY_VALID_IGO_IDS.contains(igoId);
    }

    /**
     * Returns the IGO ID with aliquots removed.
     *
     * @param igoId
     * @return
     */
    public static String baseIgoSampleId(String igoId) {
        if (igoId == null)
            return null;
        String request = requestFromIgoId(igoId);
        int indexEnd = igoId.indexOf('_', request.length() + 1);
        if (indexEnd == -1)
            return igoId;
        else
            return igoId.substring(0, indexEnd);
    }
}
