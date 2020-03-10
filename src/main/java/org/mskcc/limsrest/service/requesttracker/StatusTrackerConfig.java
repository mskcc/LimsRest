package org.mskcc.limsrest.service.requesttracker;

import java.util.*;

// Temporary mapping of statuse to their buckets
public class StatusTrackerConfig {
    public static final String STAGE_SUBMITTED = "submitted";
    public static final String STAGE_SAMPLE_QC = "sampleQc";
    public static final String STAGE_LIBRARY_PREP = "libraryPrep";
    public static final String STAGE_SEQUENCING = "sequencing";
    public static final String STAGE_DATA_QC = "dataQc";
    public static final String STAGE_IGO_COMPLETE = "igoComplete";
    public static final String STAGE_UNKNOWN = "unknown";

    /**
     * Add the order of stages here and then the ordering map will be statically initialzed
     */
    private static String[] stageOrder = new String[]{
            STAGE_SUBMITTED,
            STAGE_SAMPLE_QC,
            STAGE_LIBRARY_PREP,
            STAGE_SEQUENCING,
            STAGE_IGO_COMPLETE
    };
    private static Map<String, String> nextStageMap;
    static {
        nextStageMap = new HashMap<>();
        for(int i = 0; i<stageOrder.length-1; i++){
            nextStageMap.put(stageOrder[i], stageOrder[i+1]);
        }
        nextStageMap.put(stageOrder[stageOrder.length-1], null);
    }

    private static final Set<String> LIBRARY_PREP_STATUSES = new HashSet<>(Arrays.asList(
            "Completed - Generic Normalization Plate Setup",
            "Completed - Generic Library Preparation",
            "Completed - Pooling of Sample Libraries by Volume",
            "Received"
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

    public static String getNextStage(String status){
        return nextStageMap.get(status);
    }

    /**
     * Returns the position of the stage. Returns out-of-bounds index if not present
     *
     * @param status
     * @return
     */
    public static int getStageOrder(String status) {
        for(int i = 0; i<stageOrder.length; i++){
            if(status.equals(stageOrder[i])) return i;
        }
        return stageOrder.length;
    }

    /**
     * Comparator used to sort statuses based on their order
     */
    public static class StageComp implements Comparator<String>{
        @Override
        public int compare(String s1, String s2) {
            int p1 = getStageOrder(s1);
            int p2 = getStageOrder(s2);

            return p1 - p2;
            // return (p1 < p2) ? -1 : (p2 > p1) ? 1 : 0;
        }
    }

}

