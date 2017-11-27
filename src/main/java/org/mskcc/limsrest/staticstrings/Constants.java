package org.mskcc.limsrest.staticstrings;

import java.time.format.DateTimeFormatter;

public class Constants {
    public static final String POOLING_OF_SAMPLE_LIBRARIES_FOR_SEQUENCING = "Pooling of Sample Libraries for Sequencing";
    public static final String CAPTURE_FROM_KAPA_LIBRARY1 = "Capture from KAPA Library";
    public static final String LIBRARY_POOL_QUALITY_CONTROL1 = "Library/Pool Quality Control";
    public static final String WHOLE_EXOME_CAPTURE = "Whole Exome Capture";
    public static final String PRE_SEQUENCING_POOLING_OF_LIBRARIES1 = "Pre-Sequencing Pooling of Libraries";
    public static final String IMPACT_HEME_PACT_OR_CUSTOM_CAPTURE = "IMPACT/HemePACT or Custom Capture";
    public static final int SAMPLE_COUNT_MAX_VALUE = 999;
    public static final int SAMPLE_COUNT_MIN_VALUE = 0;
    public static final String ERRORS = "Errors";
    public static final String WARNINGS = "Warnings";
    public static final String STATUS = "Status";
    public static final String US_DATE_FORMAT = "MM-dd-yyyy";
    public static final DateTimeFormatter US_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(US_DATE_FORMAT);
}
