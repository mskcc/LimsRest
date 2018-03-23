package org.mskcc.limsrest.staticstrings;

import java.time.format.DateTimeFormatter;

public class Constants {

    public static final int SAMPLE_COUNT_MAX_VALUE = 999;
    public static final int SAMPLE_COUNT_MIN_VALUE = 0;
    public static final String ERRORS = "Errors";
    public static final String WARNINGS = "Warnings";
    public static final String STATUS = "Status";
    public static final String US_DATE_FORMAT = "MM-dd-yyyy";
    public static final DateTimeFormatter US_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(US_DATE_FORMAT);
}
