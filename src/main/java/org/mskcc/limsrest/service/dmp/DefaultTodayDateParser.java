package org.mskcc.limsrest.service.dmp;

import org.apache.commons.lang3.StringUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.mskcc.limsrest.util.Constants;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class DefaultTodayDateParser {
    private static final Log LOGGER = LogFactory.getLog(DefaultTodayDateParser.class);

    public static LocalDate parse(String dateString) {
        if (StringUtils.isEmpty(dateString)) {
            LocalDate now = LocalDate.now();
            LOGGER.info(String.format("Date parameter not provided thus today's date %s will be used", now));
            return now;
        } else {
            return getParsedDate(dateString);
        }
    }

    private static LocalDate getParsedDate(String dateString) {
        LOGGER.info(String.format("Date parameter provided: %s", dateString));
        try {
            return LocalDate.parse(dateString, Constants.US_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new DateTimeParseException(String.format("Cannot parse date provided: %s. Expected format is: %s",
                    dateString, Constants.US_DATE_FORMAT), e.getParsedString(), e.getErrorIndex());
        }
    }
}
