package org.mskcc.limsrest.limsapi.cmoinfo.converter;

import org.mskcc.domain.CorrectedCmoSampleView;
import org.mskcc.limsrest.limsapi.LimsException;

public interface CorrectedCmoIdConverter<T> {
    CorrectedCmoSampleView convert(T t) throws LimsException;
}
