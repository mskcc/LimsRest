package org.mskcc.limsrest.service.requesttracker;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// Temporary mapping of statuse to their buckets
public class StatusTrackerConfig {
    public static final String STAGE_SUBMITTED = "submitted";
    public static final String STAGE_SAMPLE_QC = "sampleQc";
    public static final String STAGE_LIBRARY_PREP = "libraryPrep";
    public static final String STAGE_SEQUENCING = "sequencing";
    public static final String STAGE_DATA_QC = "dataQc";
    public static final String STAGE_IGO_COMPLETE = "igoComplete";
    public static final String STAGE_UNKNOWN = "unknown";

    private static final Set<String> LIBRARY_PREP_STATUSES = new HashSet<>(Arrays.asList(
            "Completed - Generic Normalization Plate Setup",
            "Completed - Generic Library Preparation",
            "Completed - Pooling of Sample Libraries by Volume"
    ));

    private static final Set<String> SEQUENCING_STATUSES = new HashSet<>(Arrays.asList(
            "Ready for - Pooling of Sample Libraries for Sequencing",
            "In Process - Illumina Sequencing",
            "Completed - Illumina Sequencing"
    ));

    /**
     * Returns whether a status is a complete one
     *
     *  TODO - Posible that this won't work if the workflow doesn't end w/ sequencing
     *
     * @param status
     * @return
     */
    public static final boolean isCompletedStatus(String status){
        return status.equals("Completed - Illumina Sequencing");
    }

    public static String getStageForStatus(String status) {
        if(LIBRARY_PREP_STATUSES.contains(status)) return STAGE_LIBRARY_PREP;
        else if(SEQUENCING_STATUSES.contains(status)) return STAGE_SEQUENCING;
        else return STAGE_UNKNOWN;
    }
}
