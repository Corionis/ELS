package com.groksoft.volmunger;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The type Utils.
 */
public class Utils
{
    /**
     * Do not instantiate.
     */
    private Utils() {
        // do not instantiate
    }

    /**
     * Gets scaled value.
     *
     * @param size the string to parse
     * @return the scaled value
     */
    public static long getScaledValue(String size) {
        long returnValue = -1;
        Pattern patt = Pattern.compile("([\\d.]+)([TGMK]B)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = patt.matcher(size);
        Map<String, Integer> powerMap = new HashMap<String, Integer>();
        powerMap.put("TB", 4);
        powerMap.put("GB", 3);
        powerMap.put("MB", 2);
        powerMap.put("KB", 1);
        if (matcher.find()) {
            String number = matcher.group(1);
            int pow = powerMap.get(matcher.group(2).toUpperCase());
            BigDecimal bytes = new BigDecimal(number);
            bytes = bytes.multiply(BigDecimal.valueOf(1024).pow(pow));
            returnValue = bytes.longValue();
        }
        return returnValue;
    }

    /**
     * Gets last path.
     *
     * @param full the full
     * @return the last path
     */
    public static String getLastPath(String full) {
        String path = "";
        int p = full.indexOf("\\");
        if (p >= 0) {
            path = full.substring(0, p);
        } else {
            p = full.indexOf(File.separator);
            if (p >= 0) {
                path = full.substring(0, p);
            } else {
                path = full;
            }
        }
        return path;
    }

    /**
     * Gets stack trace.
     *
     * @param throwable the throwable
     * @return the stack trace
     */
    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    /**
     * Parse the host from a location string
     *
     * Expected format: [hostname|IP address]:[port number]
     *
     * @param location
     * @return host
     */
    public static String parseHost(String location)
    {
        String host = null;
        String[] a = location.split(":");
        if (a.length >= 1)
        {
            host = a[0];
        }
        return host;
    }

    /**
     * Parse the port from a location string
     *
     * Expected format: [hostname|IP address]:[port number]
     *
     * @param location
     * @return port
     */
    public static String parsePort(String location)
    {
        String sport = "";
        String[] a = location.split(":");
        if (a.length == 2)
        {
            sport = a[1];
        }
        return sport;
    }

}
