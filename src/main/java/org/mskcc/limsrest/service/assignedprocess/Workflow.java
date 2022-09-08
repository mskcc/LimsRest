package org.mskcc.limsrest.service.assignedprocess;

public enum Workflow {
    POOLING_OF_SAMPLE_LIBRARIES_FOR_SEQUENCING("Pooling of Sample Libraries for Sequencing", 1),
    CAPTURE_FROM_KAPA_LIBRARY("Capture from KAPA Library", 1),
    LIBRARY_POOL_QUALITY_CONTROL("Library/Pool Quality Control", 1),
    CAPTURE_HYBRIDIZATION("Capture - Hybridization", 1),
    ASSIGN_FROM_INVESTIGATOR_DECISION("Assign from Investigator Decisions", 1);

    private final String name;
    private final int stepNumber;

    private Workflow(String name, int stepNumber) {
        this.name = name;
        this.stepNumber = stepNumber;
    }

    public String getName() {
        return this.name;
    }

    public int getStepNumber() {
        return this.stepNumber;
    }
}
