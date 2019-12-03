package org.eol.globi.data;

import java.io.PrintWriter;
import java.io.StringWriter;

public class LogUtil {
    public static void logError(ImportLogger logger, Throwable cause) {
        logError(logger, "", cause);
    }
    public static void logError(ImportLogger logger, String msg, Throwable cause) {
        if (logger != null) {
            StringWriter out = new StringWriter();
            cause.printStackTrace(new PrintWriter(out));
            logger.severe(null, "[" + msg + "] Caused by: " + out.toString());
        }
    }
}
