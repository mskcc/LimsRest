package org.mskcc.limsrest.service.cmoinfo.retriever;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.limsrest.service.cmoinfo.CmoSampleId;
import org.mskcc.limsrest.service.cmoinfo.formatter.CmoSampleIdFormatter;

import java.util.List;

public class FormattedCmoSampleIdRetriever implements CmoSampleIdRetriever {
    private static final Log LOGGER = LogFactory.getLog(FormattedCmoSampleIdRetriever.class);

    private CmoSampleIdResolver cmoSampleIdResolver;
    private CmoSampleIdFormatter cmoSampleIdFormatter;

    public FormattedCmoSampleIdRetriever(CmoSampleIdResolver cmoSampleIdResolver,
                                         CmoSampleIdFormatter cmoSampleIdFormatter) {
        this.cmoSampleIdResolver = cmoSampleIdResolver;
        this.cmoSampleIdFormatter = cmoSampleIdFormatter;
    }

    @Override
    public String retrieve(CorrectedCmoSampleView correctedCmoSampleView, List<CorrectedCmoSampleView>
            cmoSampleViews, String requestId) {
        CmoSampleId cmoSampleId = cmoSampleIdResolver.resolve(correctedCmoSampleView, cmoSampleViews, requestId);

        LOGGER.info(String.format("CMO Sample Id properties retrieved: %s for sample: %s", cmoSampleId,
                correctedCmoSampleView.getId()));
        return cmoSampleIdFormatter.format(cmoSampleId);
    }
}
