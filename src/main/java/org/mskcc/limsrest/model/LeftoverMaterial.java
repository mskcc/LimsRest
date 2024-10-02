package org.mskcc.limsrest.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@ToString
@Getter
public class LeftoverMaterial {
    private String igoId;
    private String sampleId;

    private Double remainingMassDNA;
    private Double remainingMassRNA;

    private Double remainingLibraryMass;
    private String remainingLibraryUnits;
}
