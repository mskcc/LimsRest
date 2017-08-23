package org.mskcc.domain;

import java.util.HashMap;
import java.util.Map;

public enum ProcessName {
    PRE_SEQUENCING_POOLING_OF_LIBRARIES("Pre-Sequencing Pooling of Libraries"),
    WHOLE_EXOME_CAPTURE("Whole Exome Capture");

    private static final Map<String, ProcessName> valueToProcessName = new HashMap<>();

    static {
        for (ProcessName processName : values()) {
            valueToProcessName.put(processName.value, processName);
        }
    }

    private String value;

    ProcessName(String value) {
        this.value = value;
    }

    public static ProcessName getByValue(String value) {
        if(!valueToProcessName.containsKey(value))
            throw new RuntimeException(String.format("Qc status: %s doesn't exist", value));

        return valueToProcessName.get(value);
    }

    public String getValue() {
        return value;
    }
}
