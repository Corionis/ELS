package com.groksoft;

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

}
