package org.mskcc.limsrest.service.cmoinfo.cellline;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.limsrest.service.cmoinfo.retriever.CmoSampleIdResolver;

import java.util.List;

public class CellLineCmoSampleIdResolver implements CmoSampleIdResolver<CellLineCmoSampleId> {
    private static final Log log = LogFactory.getLog(CellLineCmoSampleIdResolver.class);

    @Override
    public CellLineCmoSampleId resolve(CorrectedCmoSampleView correctedCmoSampleView, List<CorrectedCmoSampleView>
            patientSamples, String requestId) {
        log.debug(String.format("Resolving corrected cmo id for Cellline sample: %s", correctedCmoSampleView.getId()));

        return new CellLineCmoSampleId(correctedCmoSampleView.getSampleId(), normalizeRequestId(requestId));
    }

    private String normalizeRequestId(String requestId) {
        String normalized = requestId.replaceAll("_", "");
        log.info(String.format("Normalizing request %s. Normalized name: %s", requestId, normalized));

        return normalized;
    }
}
