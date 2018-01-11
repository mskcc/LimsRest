package org.mskcc.limsrest.limsapi.cmoinfo.cellline;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.limsapi.cmoinfo.formatter.CmoSampleIdFormatter;

public class CellLineCmoSampleIdFormatter implements CmoSampleIdFormatter<CellLineCmoSampleId> {
    private final static Log LOGGER = LogFactory.getLog(CellLineCmoSampleIdFormatter.class);

    private static final String DELIMITER = "-";

    @Override
    public String format(CellLineCmoSampleId cmoSampleId) {
        String formatted = String.format("%s%s%s", cmoSampleId.getSampleId(), DELIMITER, cmoSampleId.getRequestId());

        LOGGER.info(String.format("Formatted CMO Sample Id: %s for cell line sample: %s", formatted, cmoSampleId));

        return formatted;
    }
}
