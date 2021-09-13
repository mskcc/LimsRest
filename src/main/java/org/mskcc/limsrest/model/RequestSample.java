package org.mskcc.limsrest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Setter
public class RequestSample implements Serializable {
    private String investigatorSampleId;
    private String igoSampleId;
    private boolean igoComplete;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getInvestigatorSampleId() {
        return investigatorSampleId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getIgoSampleId() {
        return igoSampleId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public boolean isIgoComplete() {
        return igoComplete;
    }
}