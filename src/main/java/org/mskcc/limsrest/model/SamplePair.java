package org.mskcc.limsrest.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * For a given patient ID and recipe, the list of tumor and normal samples in LIMS.
 */
@Getter
@ToString
@Setter
public class SamplePair {
    private String patientId;
    private String recipe;
    private List<String> normalIgoIdSampleName;
    private List<String> tumorIgoIdSampleName;

    public SamplePair(String patientId, String recipe, List<String> normalIgoIdSampleName, List<String> tumorIgoIdSampleName) {
        this.patientId = patientId;
        this.recipe = recipe;
        this.normalIgoIdSampleName = normalIgoIdSampleName;
        this.tumorIgoIdSampleName = tumorIgoIdSampleName;
    }

    public void addSample(String tumorOrNormal, String name) {
        if ("tumor".equalsIgnoreCase(tumorOrNormal))
            tumorIgoIdSampleName.add(name);
        if ("normal".equalsIgnoreCase(tumorOrNormal))
            normalIgoIdSampleName.add(name);
    }
}
