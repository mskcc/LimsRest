package org.mskcc.limsrest.service;

import com.fasterxml.jackson.annotation.*;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter @NoArgsConstructor
public class RestDescriptor {
    String detectedError;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getDetectError() { return detectedError;
    }
}