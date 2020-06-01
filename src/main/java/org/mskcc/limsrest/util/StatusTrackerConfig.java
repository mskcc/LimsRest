package org.mskcc.limsrest.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

// Temporary mapping of statuse to their buckets
public class StatusTrackerConfig {
    // VALID STAGES
    public static final String STAGE_SUBMITTED = "Submitted";
    public static final String STAGE_SAMPLE_QC = "Quality Control";
    public static final String STAGE_EXTRACTION = "Extraction";
    public static final String STAGE_LIBRARY_PREP = "Library Preparaton";
    public static final String STAGE_LIBRARY_CAPTURE = "Library Capture";
    public static final String STAGE_SEQUENCING = "Sequencing";
    public static final String STAGE_DATA_QC = "Data QC";
    public static final String STAGE_IGO_COMPLETE = "IGO Complete";
    // AMBIGUOUS STAGES
    public static final String STAGE_UNKNOWN = "unknown";
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
            STAGE_SAMPLE_QC,        // SAMPLE_QC happens at different levels, e.g.  after extraction, after Library prep, after capture, after pooling etc
            STAGE_SEQUENCING,
            STAGE_DATA_QC,          // If something needs to be re-sequenced, we want to keep the sample in data-qc
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

