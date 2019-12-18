package org.eol.globi.tool;

import org.eol.globi.data.ImportLogger;
import org.eol.globi.domain.LogContext;

import java.util.logging.Level;

public class NullImportLogger implements ImportLogger {

    @Override
    public void warn(LogContext ctx, String message) {

    }

    @Override
    public void info(LogContext ctx, String message) {

    }

    @Override
    public void severe(LogContext ctx, String message) {

    }
    
}
