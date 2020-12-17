package org.eol.globi.process;

import org.eol.globi.data.ImportLogger;

import java.util.Map;

public class LogUtil {
    public static void logIfPossible(Map<String, String> interaction, String msg, ImportLogger logger) {
        if (logger != null) {
            logger.info(org.eol.globi.data.LogUtil.contextFor(interaction), msg);
        }
    }
}
