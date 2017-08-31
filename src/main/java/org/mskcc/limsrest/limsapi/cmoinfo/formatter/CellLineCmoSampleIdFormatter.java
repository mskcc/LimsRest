package org.mskcc.limsrest.limsapi.cmoinfo.formatter;

import org.mskcc.limsrest.limsapi.cmoinfo.CellLineCmoSampleId;

public class CellLineCmoSampleIdFormatter implements CmoSampleIdFormatter<CellLineCmoSampleId> {
    private static final String DELIMITER = "-";

    @Override
    public String format(CellLineCmoSampleId cmoSampleId) {
        return String.format("%s%s%s", cmoSampleId.getUserSampleId(), DELIMITER, cmoSampleId.getRequestId());

    }
}
