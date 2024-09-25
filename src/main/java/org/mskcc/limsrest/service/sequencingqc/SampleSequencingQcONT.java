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
    private int medianReadLength; // MedianReadLength
    private double estimatedCoverage; // EstimatedCoverage
    private double bamCoverage; // BAMCoverage
    private String sequencerName; // SequencerName
    private String sequencerPosition; // SequencerPosition
}
