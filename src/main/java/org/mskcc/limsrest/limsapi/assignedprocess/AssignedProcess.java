package org.mskcc.limsrest.limsapi.assignedprocess;

import org.mskcc.domain.Workflow;

public enum AssignedProcess {
    WHOLE_EXOME_CAPTURE("Whole Exome Capture", Workflow.CAPTURE_FROM_KAPA_LIBRARY),
    PRE_SEQUENCING_POOLING_OF_LIBRARIES("Pre-Sequencing Pooling of Libraries", Workflow.POOLING_OF_SAMPLE_LIBRARIES_FOR_SEQUENCING),
    IMPACT_HEMEPACT_OR_CUSTOM_CAPTURE("IMPACT/HemePACT or Custom Capture", Workflow.CAPTURE_HYBRIDIZATION);

    private final String name;
    private final Workflow workflow;

    AssignedProcess(String name, Workflow workflow) {
        this.name = name;
        this.workflow = workflow;
    }

    public String getName() {
        return this.name;
    }

    public int getStepNumber() {
        return this.workflow.getStepNumber();
    }

    public String getWorkflowName() {
        return this.workflow.getName();
    }

    public String getStatus() {
        return String.format("Ready for - %s", this.getWorkflowName());
    }
}