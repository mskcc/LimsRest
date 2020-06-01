package org.mskcc.limsrest.service.requesttracker;

import java.util.*;
import static org.mskcc.limsrest.util.StatusTrackerConfig.*;

// Temporary mapping of statuse to their buckets
public class StatusTrackerConfig {
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

    // Will take from the last available stage
    // "Returned to User'"
    // Turned off by Group Leader
    // Processing Completed
    // "Discarded"
    // "Received"  // ???

    private static final Set<String> SAMPLE_QC_STATUSES = new HashSet<>(Arrays.asList(
            "Ready for - Library/Pool Quality Control",
            "Completed - Library/Pool Quality Control"
    ));
    private static final Set<String> LIBRARY_PREP_STATUSES = new HashSet<>(Arrays.asList(
            "Completed - Generic Library Preparation",
            "Completed - Library Clean Up/Size Selection",

            "In Process - KAPA Library Preparation",
            "Completed - KAPA Library Preparation",
            "Completed - Capture from KAPA Library",

            "Completed - Generic Normalization Plate Setup",
            "Completed - MSK Access Normalization Plate Setup",

            "Completed - Pooling of Sample Libraries by Volume",
            "Ready for - Pooling of Sample Libraries by Volume",
            "In Process - Pooling of Sample Libraries for Sequencing",

            "In Process - Capture - Hybridization",
            "Ready for - MSK Access Capture - Hybridization",
            "Completed - Capture - Hybridization",
            "Completed - MSK Access Capture - Hybridization",
            "Completed - MSK Access Library Preparation",

            "Completed - Normalization Plate Setup",
            "Ready for - Normalization Plate Setup",

            "Completed - Archer Library Preparation Experiment",

            "Completed - TruSeqRNA Poly-A cDNA Preparation",

            "Completed - STR/Fragment Analysis Profiling",
            "Completed - STR PCR Human",
            "STR",     // ???

            "Ready for - Digital Droplet PCR",      // ???
            "Completed - Digital Droplet PCR",
            "Completed - PCR Cycle Re-Amplification",

            "Completed - 10X Genomics cDNA Preparation",
            "Completed - 10X Genomics Library Preparation"
    ));
    private static final Set<String> SEQUENCING_STATUSES = new HashSet<>(Arrays.asList(
            "Ready for - Pooling of Sample Libraries for Sequencing",
            "Ready for - Illumina Sequencing",
            "In Process - Illumina Sequencing",
            "Completed - Pooling of Sample Libraries for Sequencing",
            "Completed - Illumina Sequencing",
            "Completed - Illumina Sequencing Setup",
            "Completed - Illumina Sequencing Planning/Denaturing",
            "Completed - Illumina Sequencing Analysis"

    ));

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
}

