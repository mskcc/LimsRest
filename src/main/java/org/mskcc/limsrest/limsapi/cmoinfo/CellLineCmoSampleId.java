package org.mskcc.limsrest.limsapi.cmoinfo;

public class CellLineCmoSampleId implements CmoSampleId {
    private final String userSampleId;
    private final String requestId;

    public CellLineCmoSampleId(String userSampleId, String requestId) {
        this.userSampleId = userSampleId;
        this.requestId = requestId;
    }

    public String getUserSampleId() {
        return userSampleId;
    }

    public String getRequestId() {
        return requestId;
    }
}
