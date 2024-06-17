package org.mskcc.limsrest.controller;

import java.util.regex.Pattern;

/*
  Just being extra stringent about what we let through as parameters, mostly for parameters that will be used by the data record manager to do sql queries.
  I'm hopeful the lims is resistant to attack vectors this way, but I haven't tested
*/
public class Whitelists {
    private static final Pattern REQUEST_PATTERN = Pattern.compile("^[0-9]{5,}[A-Z_]*$");
    public  static final Pattern SAMPLE_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");
    private static final Pattern GENERIC_PARAM_PATTERN = Pattern.compile("^[ A-Za-z0-9_'.+-]+$");
    private static final Pattern FILE_PATH_PATTERN = Pattern.compile("^[A-Za-z0-9_/.-]*$");
    private static final Pattern SERVICE_PATTERN = Pattern.compile("^(IGO|igo|iGO|iGo)[-_][0-9]{4,8}$");
    private static final Pattern SPECIAL_PARAM_PATTERN = Pattern.compile("^[ A-Za-z0-9_'()&/.,+-]+$");

    public static String serviceFormatText() {
        return "The service id must be igo  followed by a dash or hyphen followed by numbers";
    }

    public static boolean serviceMatches(String service) {
        if (service == null || service.equals("NULL"))
            return true;
        return SERVICE_PATTERN.matcher(service).matches(); //banked requests can have "igo/IGO prefix
    }

    public static String requestFormatText() {
        return "The format must be five digits followed an optional underscore and capital letter";
    }

    public static boolean requestMatches(String request) {
        if (request == null || request.equals("NULL"))
            return true;
        return REQUEST_PATTERN.matcher(request).matches();
    }

    public static String filePathFormatText() {
        return "The format must be a valid unix file path";
    }

    public static boolean filePathMatches(String path) {
        if (path == null || path.equals("NULL"))
            return true;
        return FILE_PATH_PATTERN.matcher(path).matches();
    }

    public static String sampleFormatText() {
        return "The format must only contain upper case and lower case letters, numbers, underscores and dashes";
    }

    public static boolean sampleMatches(String sample) {
        if (sample == null || sample.equals("NULL"))
            return true;
        return SAMPLE_NAME_PATTERN.matcher(sample).matches();
    }

    public static String textFormatText() {
        return "The format must only contain upper case and lower case letters, numbers, underscores, single quotes, periods, plusses and minuses";
    }

    public static boolean textMatches(String text) {
        if (text == null || text.equals("NULL"))
            return true;
        return GENERIC_PARAM_PATTERN.matcher(text).matches();
    }

    public static boolean specialMatches(String text) {
        if (text == null || text.equals("NULL"))
            return true;
        return SPECIAL_PARAM_PATTERN.matcher(text).matches();
    }
}