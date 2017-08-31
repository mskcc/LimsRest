package org.mskcc.limsrest.limsapi.cmoinfo.retriever;

import org.mskcc.domain.BankedSample;
import org.mskcc.limsrest.limsapi.cmoinfo.CellLineCmoSampleId;
import org.mskcc.util.CommonUtils;

import java.util.List;

public class CellLineCmoSampleIdResolver implements CmoSampleIdResolver<CellLineCmoSampleId> {
    @Override
    public CellLineCmoSampleId resolve(BankedSample bankedSample, List<String> sampleIds) {
        CommonUtils.requireNonNullNorEmpty(bankedSample.getUserSampleId(), String.format("User Sample Id is not set for banked sample with Record id: %s", bankedSample.getRecordId()));
        CommonUtils.requireNonNullNorEmpty(bankedSample.getRequestId(), String.format("Request Id is not set for banked sample: %s", bankedSample.getUserSampleId()));

        return new CellLineCmoSampleId(bankedSample.getUserSampleId(), bankedSample.getRequestId());
    }
}
