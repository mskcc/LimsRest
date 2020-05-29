package org.mskcc.limsrest.util;

/**
 * Lims Stage for a ProjectSample composed of a Sample DataRecord's "ExemplarSampleType" & "ExemplarSampleStatus"
 */
public class LimsStage {
    enum Status {
        Pending,
        Failed,
        Complete
    }

    private String stage;
    private Status status;

    public LimsStage(String stage, Status status){
        this.stage = stage;
        this.status = status;
    }

    public boolean isFailed() {
        return (this.status == Status.Failed);
    }

    private String getStatusString() {
        // TODO - constants
        if(this.status == Status.Failed){
            return "Failed";
        } else if (this.status == Status.Complete){
            return "Completed";
        }
        return "Pending";
    }

    /**
     * Stringifies Stage (E.g. "com
     *
     * @return
     */
    public String getStageStatus(){
        String status = getStatusString();
        return String.format("%s - %s", status, this.stage);
    }
}
