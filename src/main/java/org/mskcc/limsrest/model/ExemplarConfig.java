package org.mskcc.limsrest.model;

import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonInclude;

@AllArgsConstructor
@ToString
@Setter
public class ExemplarConfig {
    private String chipPosition;
    private String chipID;
    private String preservation;
    private Boolean cytAssist;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getChipPosition() {
        return chipPosition;
    }
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getChipID() {
        return chipID;
    }
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Boolean getCytAssist() {
        return cytAssist;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getPreservation() {
        return preservation;
    }
}
