package org.mskcc.limsrest.limsapi.cmoinfo.cellline;

import org.mskcc.limsrest.limsapi.cmoinfo.formatter.CmoSampleIdFormatter;

public class CellLineCmoSampleIdFormatter implements CmoSampleIdFormatter<CellLineCmoSampleId> {
    private static final String DELIMITER = "-";

    @Override
    public String format(CellLineCmoSampleId cmoSampleId) {
        return String.format("%s%s%s", cmoSampleId.getSampleId(), DELIMITER, cmoSampleId.getRequestId());
    }
}
