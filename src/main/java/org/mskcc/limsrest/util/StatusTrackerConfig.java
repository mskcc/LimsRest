package org.mskcc.limsrest.util;

// Temporary mapping of statuse to their buckets
public class StatusTrackerConfig {
    // VALID STAGES
    public static final String STAGE_SUBMITTED = "Submitted";
    public static final String STAGE_SAMPLE_QC = "QualityControl";
    public static final String STAGE_EXTRACTION = "Extraction";
    public static final String STAGE_LIBRARY_PREP = "LibraryPreparaton";
    public static final String STAGE_LIBRARY_CAPTURE = "LibraryCapture";
    public static final String STAGE_SEQUENCING = "Sequencing";
    public static final String STAGE_DATA_QC = "DataQC";
    public static final String STAGE_IGO_COMPLETE = "IGOComplete";
    // AMBIGUOUS STAGES
    public static final String STAGE_AWAITING_PROCESSING = "awaitingProcessing";    // Stage prior to any workflow

    /**
     * Add the order of valid stages here and then the ordering map will be statically initialzed
     */
    private static String[] stageOrder = new String[]{
            // TODO - need to confirm w/ Anna/Ajay
            STAGE_SUBMITTED,
            STAGE_AWAITING_PROCESSING,
            STAGE_EXTRACTION,
            STAGE_LIBRARY_PREP,
            STAGE_LIBRARY_CAPTURE,
            STAGE_SAMPLE_QC,
            STAGE_SEQUENCING,
            STAGE_DATA_QC,  // If something needs to be re-sequenced, we want to keep the sample in data-qc
            STAGE_IGO_COMPLETE
    };
}

