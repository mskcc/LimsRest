package org.mskcc.limsrest.service.requesttracker;

import java.util.*;

// Temporary mapping of statuse to their buckets
public class StatusTrackerConfig {
    // VALID STAGES
    public static final String STAGE_SUBMITTED = "submitted";
    public static final String STAGE_SAMPLE_QC = "sampleQc";
    public static final String STAGE_LIBRARY_PREP = "libraryPrep";
    public static final String STAGE_SEQUENCING = "sequencing";
    public static final String STAGE_DATA_QC = "dataQc";
    public static final String STAGE_IGO_COMPLETE = "igoComplete";
    public static final String STAGE_EXTRACTION = "extraction";

    // INVALID STAGES
    public static final String STAGE_UNKNOWN = "unknown";
    public static final String STAGE_AWAITING_PROCESSING = "awaitingProcessing";    // Stage prior to any workflow

    // Special Statuses
    public static final String STATUS_AWAITING_PROCESSING = "Awaiting Processing";  // Stage cannot be determined
    private static final Set<String> FAILED_STATUSES = new HashSet<>(Arrays.asList(
            "Failed - Completed",
            "Failed - Pending User Decision",
            "Failed - Library/Pool Quality Control"
    ));

    private static final Set<String> STATUS_EXTRACTION = new HashSet<>(Arrays.asList(
            "Completed - DNA Extraction",
            "Returned to User"              // ?
    ));

    private static final Set<String> SAMPLE_QC_STATUSES = new HashSet<>(Arrays.asList(
            "Completed - Library/Pool Quality Control"
    ));
    private static final Set<String> LIBRARY_PREP_STATUSES = new HashSet<>(Arrays.asList(
            "Completed - Generic Normalization Plate Setup",
            "Completed - Generic Library Preparation",
            "Completed - Pooling of Sample Libraries by Volume",
            "In Process - KAPA Library Preparation",
            "In Process - Capture - Hybridization",
            "Completed - MSK Access Normalization Plate Setup",
            "Ready for - MSK Access Capture - Hybridization",
            "Ready for - Pooling of Sample Libraries by Volume",
            "Completed - Library Clean Up/Size Selection",
            "Completed - Normalization Plate Setup",
            "Completed - KAPA Library Preparation",
            "Ready for - Normalization Plate Setup",
            "Completed - Archer Library Preparation Experiment",
            "Completed - Capture from KAPA Library",
            "In Process - Pooling of Sample Libraries for Sequencing",
            "Completed - Capture - Hybridization",
            "Completed - MSK Access Capture - Hybridization",
            "Ready for - Library/Pool Quality Control",
            "Ready for - Digital Droplet PCR",      // ???
            "Completed - TruSeqRNA Poly-A cDNA Preparation",
            "Completed - MSK Access Library Preparation",
            "Completed - STR/Fragment Analysis Profiling",
            "Completed - STR PCR Human",
            "Completed - Digital Droplet PCR",
            "STR",     // ???
            "Received"  // ???
    ));
    private static final Set<String> SEQUENCING_STATUSES = new HashSet<>(Arrays.asList(
            "Ready for - Pooling of Sample Libraries for Sequencing",
            "Completed - Pooling of Sample Libraries for Sequencing",
            "In Process - Illumina Sequencing",
            "Completed - Illumina Sequencing",
            "Completed - Illumina Sequencing Setup",
            "Completed - Illumina Sequencing Planning/Denaturing",
            "Completed - Illumina Sequencing Analysis"

    ));
    /**
     * Add the order of valid stages here and then the ordering map will be statically initialzed
     */
    private static String[] stageOrder = new String[]{
            STAGE_SUBMITTED,
            STAGE_AWAITING_PROCESSING,
            STAGE_EXTRACTION,
            STAGE_LIBRARY_PREP,
            STAGE_SAMPLE_QC,
            STAGE_SEQUENCING,
            STAGE_DATA_QC,  // If something needs to be re-sequenced, we want to keep the sample in data-qc
            STAGE_IGO_COMPLETE
    };
    private static Map<String, String> nextStageMap;

    static {
        nextStageMap = new HashMap<>();
        for (int i = 0; i < stageOrder.length - 1; i++) {
            nextStageMap.put(stageOrder[i], stageOrder[i + 1]);
        }
        nextStageMap.put(stageOrder[stageOrder.length - 1], null);
    }

    /**
     * Returns whether the input stage is valid
     *
     * @param stage
     * @return
     */
    public static boolean isValidStage(String stage) {
        return nextStageMap.containsKey((stage));
    }

    /**
     * Returns whether the input status is a failed one
     *
     * @param status
     * @return
     */
    public static Boolean isFailedStatus(String status) {
        return FAILED_STATUSES.contains(status);
    }

    public static String getStageForStatus(String status) throws IllegalArgumentException {
        if (LIBRARY_PREP_STATUSES.contains(status)) return STAGE_LIBRARY_PREP;
        else if (SEQUENCING_STATUSES.contains(status)) return STAGE_SEQUENCING;
        else if (SAMPLE_QC_STATUSES.contains(status)) return STAGE_SAMPLE_QC;
        else if (STATUS_AWAITING_PROCESSING.equals(status)) return STAGE_AWAITING_PROCESSING;
        else if (STATUS_EXTRACTION.contains(status)) return STAGE_EXTRACTION;
        // Failed statuses need to be assigned stages based on preceeding/succeeding samples
        else if (FAILED_STATUSES.contains(status)) return STAGE_UNKNOWN;

        throw new IllegalArgumentException();
    }

    public static String getNextStage(String status) {
        return nextStageMap.get(status);
    }

    /**
     * Returns the position of the stage. Returns out-of-bounds index if not present
     *
     * @param status
     * @return
     */
    public static int getStageOrder(String status) {
        // if(status == null) return stageOrder.length;
        for (int i = 0; i < stageOrder.length; i++) {
            if (status.equals(stageOrder[i])) return i;
        }
        return stageOrder.length;
    }

    /**
     * Comparator used to sort statuses based on their order
     */
    public static class StageComp implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            int p1 = getStageOrder(s1);
            int p2 = getStageOrder(s2);

            return p1 - p2;
        }
    }

}

