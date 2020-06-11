package org.mskcc.limsrest.util;

/**
 * Lims Stage for a ProjectSample composed of a Sample DataRecord's "ExemplarSampleType" & "ExemplarSampleStatus"
 */
public class LimsStage {
    private String stageName;
    private Boolean isComplete;

    public LimsStage(String stageName, Boolean isComplete) {
        this.stageName = stageName;
        this.isComplete = isComplete;
    }

    private String getStatusString() {
        if (this.isComplete) {
            return "Completed";
        }
        return "Pre-Processing";
    }

    public String getStageName() {
        return this.stageName;
    }

    /**
     * Stringifies Stage w/ stageName and status
     *
     * @return
     */
    public String toString() {
        String status = getStatusString();
        return String.format("%s - %s", this.stageName, status);
    }
}
