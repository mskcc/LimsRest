package org.mskcc.domain;

import org.mskcc.limsrest.staticstrings.Constants;

public enum AssignedProcess {
    WHOLE_EXOME_CAPTURE(
            Constants.WHOLE_EXOME_CAPTURE,
            Workflow.CAPTURE_FROM_KAPA_LIBRARY),

    PRE_SEQUENCING_POOLING_OF_LIBRARIES(
            Constants.PRE_SEQUENCING_POOLING_OF_LIBRARIES1,
            Workflow.POOLING_OF_SAMPLE_LIBRARIES_FOR_SEQUENCING),

    IMPACT_HEMEPACT_OR_CUSTOM_CAPTURE(
            Constants.IMPACT_HEME_PACT_OR_CUSTOM_CAPTURE,
            Workflow.CAPTURE_HYBRIDIZATION);

    private final String name;
    private final Workflow workflow;

    AssignedProcess(String name, Workflow workflow) {
        this.name = name;
        this.workflow = workflow;
    }

    public String getName() {
        return name;
    }

    public int getStepNumber() {
        return workflow.getStepNumber();
    }

    public String getWorkflowName() {
        return workflow.getName();
    }

    public String getStatus() {
        return String.format("Ready for - %s", getWorkflowName());
    }
}
