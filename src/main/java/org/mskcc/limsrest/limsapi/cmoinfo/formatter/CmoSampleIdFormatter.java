package org.mskcc.limsrest.limsapi.cmoinfo.formatter;

import org.mskcc.limsrest.limsapi.cmoinfo.CmoSampleId;

public interface CmoSampleIdFormatter<T extends CmoSampleId> {
    String format(T cmoSampleId);
}
