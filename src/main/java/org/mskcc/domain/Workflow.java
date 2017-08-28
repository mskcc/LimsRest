package org.mskcc.domain;

import org.mskcc.limsrest.staticstrings.Constants;

public enum Workflow {
    POOLING_OF_SAMPLE_LIBRARIES_FOR_SEQUENCING(Constants.POOLING_OF_SAMPLE_LIBRARIES_FOR_SEQUENCING, 1),
    CAPTURE_FROM_KAPA_LIBRARY(Constants.CAPTURE_FROM_KAPA_LIBRARY1, 1),
    LIBRARY_POOL_QUALITY_CONTROL(Constants.LIBRARY_POOL_QUALITY_CONTROL1, 1),
    CAPTURE_HYBRIDIZATION("Capture - Hybridization", 1);

    private final String name;
    private final int stepNumber;

    Workflow(String name, int stepNumber) {
        this.name = name;
        this.stepNumber = stepNumber;
    }

    public String getName() {
        return name;
    }

    public int getStepNumber() {
        return stepNumber;
    }
}
