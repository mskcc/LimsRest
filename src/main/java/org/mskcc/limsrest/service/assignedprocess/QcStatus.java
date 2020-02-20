package org.mskcc.limsrest.service.assignedprocess;

public enum QcStatus {
    FAILED("Failed"),
    NEW_LIBRARY_NEEDED("New-Library-Needed"),
    PASSED("Passed"),
    IGO_COMPLETE("IGO-Complete"),
    RECAPTURE_SAMPLE("Recapture-Sample"),
    REPOOL_SAMPLE("Repool-Sample"),
    REQUIRED_ADDITIONAL_READS("Required-Additional-Reads"),
    RESEQUENCE_POOL("Resequence-Pool"),
    UNDER_REVIEW("Under-Review");

    private String text;

    QcStatus(String text) {
        this.text = text;
    }

    public static QcStatus fromString(String text) {
        for (QcStatus b : QcStatus.values()) {
            if (b.text.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }

    public String getText() {
        return this.text;
    }
}