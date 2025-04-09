package org.mskcc.limsrest.service.sequencingqc;

import lombok.*;

@Getter @Setter @AllArgsConstructor @ToString
public class SampleSequencingQcONT {
    public static final String TABLE_NAME = "SequencingAnalysisONT";

    private @NonNull String igoId; // IGOID
    private @NonNull String flowcell; // Flowcell
    private long reads; // ReadsNumber
    private double bases; // Bases display name GigaBases
    private long N50;
    private double medianReadLength; // MedianReadLength
    private double estimatedCoverage; // EstimatedCoverage
    private double bamCoverage; // BAMCoverage
    private String sequencerName; // SequencerName
    private String sampleName; // OtherSampleId
    private String sequencerPosition; // SequencerPosition
    // adding three new columns to ONT stats Mar. 2025
    private String flowCellType;
    private String chemistry;
    private String minKNOWSoftwareVersion;

    private String qcStatus;
    private Long recordId;
    private String recipe;
}
