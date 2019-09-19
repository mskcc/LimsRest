package org.mskcc.limsrest.service.cmoinfo.retriever;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.domain.sample.SpecimenType;

import java.util.Objects;

import static org.mskcc.util.Constants.MRN_REDACTED;

public class CmoSampleIdRetrieverFactory {
    private final static Log LOGGER = LogFactory.getLog(CmoSampleIdRetrieverFactory.class);

    private final CmoSampleIdRetriever patientCmoSampleIdRetriever;
    private final CmoSampleIdRetriever cellLineCmoSampleIdRetriever;

    public CmoSampleIdRetrieverFactory(CmoSampleIdRetriever patientCmoSampleIdRetriever,
                                       CmoSampleIdRetriever cellLineCmoSampleIdRetriever) {
        this.patientCmoSampleIdRetriever = patientCmoSampleIdRetriever;
        this.cellLineCmoSampleIdRetriever = cellLineCmoSampleIdRetriever;
    }

    public CmoSampleIdRetriever getCmoSampleIdRetriever(CorrectedCmoSampleView correctedCmoSampleView) {
        if (isCellLine(correctedCmoSampleView)) {
            LOGGER.info(String.format("Sample: %s is of type CellLine", correctedCmoSampleView.getId()));
            return cellLineCmoSampleIdRetriever;
        }

        LOGGER.info(String.format("Sample: %s is NOT CellLine", correctedCmoSampleView.getId()));
        return patientCmoSampleIdRetriever;
    }

    private boolean isCellLine(CorrectedCmoSampleView view) {
        return view.getSpecimenType() == SpecimenType.CELLLINE && !Objects.equals(view.getNormalizedPatientId(),
                MRN_REDACTED);
    }
}
