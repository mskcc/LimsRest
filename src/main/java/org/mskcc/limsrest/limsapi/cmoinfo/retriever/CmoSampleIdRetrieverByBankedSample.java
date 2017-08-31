package org.mskcc.limsrest.limsapi.cmoinfo.retriever;

import org.mskcc.domain.BankedSample;
import org.mskcc.limsrest.limsapi.cmoinfo.CmoSampleId;
import org.mskcc.limsrest.limsapi.cmoinfo.formatter.CmoSampleIdFormatter;

import java.util.List;
import java.util.Map;

public class CmoSampleIdRetrieverByBankedSample implements CmoSampleIdRetriever {
    private CmoSampleIdResolver cmoSampleIdResolver;
    private CmoSampleIdFormatter cmoSampleIdFormatter;

    public CmoSampleIdRetrieverByBankedSample(CmoSampleIdResolver cmoSampleIdResolver,
                                              CmoSampleIdFormatter cmoSampleIdFormatter) {
        this.cmoSampleIdResolver = cmoSampleIdResolver;
        this.cmoSampleIdFormatter = cmoSampleIdFormatter;
    }

    @Override
    public String retrieve(Map<String, Object> fields, List<String> patientCmoSampleIds) {
        BankedSample bankedSample = new BankedSample(fields);

        CmoSampleId cmoSampleId = cmoSampleIdResolver.resolve(bankedSample, patientCmoSampleIds);
        return cmoSampleIdFormatter.format(cmoSampleId);
    }
}
