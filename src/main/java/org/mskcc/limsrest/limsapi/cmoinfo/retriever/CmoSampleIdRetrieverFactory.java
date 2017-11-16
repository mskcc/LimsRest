package org.mskcc.limsrest.limsapi.cmoinfo.retriever;

import org.apache.log4j.Logger;
import org.mskcc.domain.CorrectedCmoSampleView;
import org.mskcc.domain.sample.SpecimenType;

public class CmoSampleIdRetrieverFactory {
    private static final Logger LOGGER = Logger.getLogger(CmoSampleIdRetrieverFactory.class);

    private final CmoSampleIdRetriever patientCmoSampleIdRetriever;
    private final CmoSampleIdRetriever cellLineCmoSampleIdRetriever;

    public CmoSampleIdRetrieverFactory(CmoSampleIdRetriever patientCmoSampleIdRetriever, CmoSampleIdRetriever
            cellLineCmoSampleIdRetriever) {
        this.patientCmoSampleIdRetriever = patientCmoSampleIdRetriever;
        this.cellLineCmoSampleIdRetriever = cellLineCmoSampleIdRetriever;
    }

    public CmoSampleIdRetriever getCmoSampleIdRetriever(CorrectedCmoSampleView correctedCmoSampleView) {
        if (isCellLine(correctedCmoSampleView.getSpecimenType())) {
            LOGGER.info(String.format("Sample: %s is of type CellLine", correctedCmoSampleView.getId()));
            return cellLineCmoSampleIdRetriever;
        }

        return patientCmoSampleIdRetriever;
    }

    private boolean isCellLine(SpecimenType specimenType) {
        return specimenType == SpecimenType.CELLLINE;
    }
}
