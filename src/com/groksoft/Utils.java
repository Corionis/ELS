package com.groksoft;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * The type Utils.
 */
public class Utils
{
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
