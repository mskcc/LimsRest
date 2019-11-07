package org.mskcc.limsrest.util;

public class IGOTools {

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
        return igoId.matches("\\d\\d\\d\\d\\d(_[A-Z]*)?(_[0-9]*)+");
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
