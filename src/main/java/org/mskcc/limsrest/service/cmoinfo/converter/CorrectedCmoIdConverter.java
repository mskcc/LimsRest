package org.mskcc.limsrest.service.cmoinfo.converter;

import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.limsrest.service.LimsException;

public interface CorrectedCmoIdConverter<T> {
    CorrectedCmoSampleView convert(T t) throws LimsException;
}
