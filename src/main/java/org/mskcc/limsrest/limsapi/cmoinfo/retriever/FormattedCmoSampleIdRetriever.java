package org.mskcc.limsrest.limsapi.cmoinfo.retriever;

import org.mskcc.domain.CorrectedCmoSampleView;
import org.mskcc.limsrest.limsapi.cmoinfo.CmoSampleId;
import org.mskcc.limsrest.limsapi.cmoinfo.formatter.CmoSampleIdFormatter;

import java.util.List;

public class FormattedCmoSampleIdRetriever implements CmoSampleIdRetriever {
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
        return cmoSampleIdFormatter.format(cmoSampleId);
    }
}
