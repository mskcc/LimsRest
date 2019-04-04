package org.mskcc.limsrest.limsapi.dmp;

import org.apache.commons.lang3.StringUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.mskcc.limsrest.staticstrings.Constants;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class DefaultTodayDateRetriever implements DateRetriever {
    private static final Log LOGGER = LogFactory.getLog(DefaultTodayDateRetriever.class);

    @Override
    public LocalDate retrieve(String dateString) {
        if (StringUtils.isEmpty(dateString)) {
            LocalDate now = LocalDate.now();
            LOGGER.info(String.format("Date parameter not provided thus today's date %s will be used", now));

            return now;
        } else {
            return getParsedDate(dateString);
        }
    }

    private LocalDate getParsedDate(String dateString) {
        LOGGER.info(String.format("Date parameter provided: %s", dateString));

        try {
            return LocalDate.parse(dateString, Constants.US_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new DateTimeParseException(String.format("Cannot parse date provided: %s. Expected format is: %s",
                    dateString, Constants.US_DATE_FORMAT), e.getParsedString(), e.getErrorIndex());
        }
    }
}
