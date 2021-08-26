package org.mskcc.limsrest.service.cmoinfo.cellline;

import lombok.Getter;
import lombok.ToString;
import org.mskcc.limsrest.service.cmoinfo.CmoSampleId;
import org.mskcc.util.CommonUtils;

/**
 * CellLineCmoSampleId stores data needed to create CMO Sample ID for Cell line samples used subsequently by Project
 * Managers.
 */
@Getter @ToString
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
//    @Override
//    public String toString() {
//        return "CellLineCmoSampleId{" +
//                "sampleId='" + sampleId + '\'' +
//                ", requestId='" + requestId + '\'' +
//                '}';
//    }
}
