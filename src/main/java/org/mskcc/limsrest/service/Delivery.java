package org.mskcc.limsrest.service;

import com.fasterxml.jackson.annotation.JsonInclude;

public class Delivery extends RestDescriptor {
    private String sampleId;
    private String requestId;

    public Delivery(String requestId, String sampleId) {
        this.requestId = requestId;
        this.sampleId = sampleId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRequestId() {
        return requestId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSampleId() {
        return sampleId;
    }
}
