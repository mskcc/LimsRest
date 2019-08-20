package org.mskcc.limsrest.service.cmoinfo.cellline;

import org.mskcc.limsrest.service.cmoinfo.CmoSampleId;
import org.mskcc.util.CommonUtils;

/**
 * CellLineCmoSampleId stores data needed to create CMO Sample ID for Cell line samples used subsequently by Project
 * Managers.
 */
public class CellLineCmoSampleId implements CmoSampleId {
    private final String sampleId;
    private final String requestId;

    public CellLineCmoSampleId(String sampleId, String requestId) {
        CommonUtils.requireNonNullNorEmpty(sampleId, String.format("Sample id cannot be null nor empty for cell line " +
                "cmo sample id"));
        CommonUtils.requireNonNullNorEmpty(requestId, String.format("Request id cannot be null nor empty for cell " +
                "line " +
                "cmo sample id"));

        this.sampleId = sampleId;
        this.requestId = requestId;
    }

    public String getSampleId() {
        return sampleId;
    }

    public String getRequestId() {
        return requestId;
    }

    @Override
    public String toString() {
        return "CellLineCmoSampleId{" +
                "sampleId='" + sampleId + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}
