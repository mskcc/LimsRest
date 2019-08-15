package org.mskcc.limsrest.service.cmoinfo.formatter;

import org.mskcc.limsrest.service.cmoinfo.CmoSampleId;

public interface CmoSampleIdFormatter<T extends CmoSampleId> {
    String format(T cmoSampleId);
}
