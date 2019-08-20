package org.mskcc.limsrest.service.converter;

import org.mskcc.domain.sample.BankedSample;

public interface ExternalToBankedSampleConverter<T> {
    BankedSample convert(T external, long transactionId);
}
