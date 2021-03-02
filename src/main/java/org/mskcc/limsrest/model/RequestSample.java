package org.mskcc.limsrest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class RequestSample implements Serializable {
    private String investigatorSampleId;
    private String igoSampleId;
    private boolean igoComplete;

    public RequestSample() {}

    /**
     * RequestSample contructor
     * @param investigatorSampleId
     * @param igoSampleId
     * @param igoComplete
     */
    public RequestSample(String investigatorSampleId, String igoSampleId, boolean igoComplete) {
        this.investigatorSampleId = investigatorSampleId;
        this.igoSampleId = igoSampleId;
        this.igoComplete = igoComplete;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getInvestigatorSampleId() {
        return investigatorSampleId;
    }

    public void setInvestigatorSampleId(String investigatorSampleId) {
        this.investigatorSampleId = investigatorSampleId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getIgoSampleId() {
        return igoSampleId;
    }

    public void setIgoSampleId(String igoSampleId) {
        this.igoSampleId = igoSampleId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public boolean isIgoComplete() {
        return igoComplete;
    }

    public void setIgoComplete(boolean igoComplete) {
        this.igoComplete = igoComplete;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
