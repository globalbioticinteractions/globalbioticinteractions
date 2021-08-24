package org.eol.globi.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eol.globi.domain.LogContext;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

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

    public static String createLogMessage(String message, Map<String, String> context) {
        try {
            LogContext logContext = new LogContextMap(context);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(logContext.toString());
            ObjectNode rootNode = mapper.createObjectNode();
            rootNode.set("context", jsonNode);
            rootNode.put("message", message);
            return rootNode.toString();
        } catch (IOException e) {
            return null;
        }
    }


    public static LogContext contextFor(Map<String, String> contextMap) {
        return new LogContextMap(contextMap);
    }

    private static class LogContextMap implements LogContext {
        private final Map<String, String> context;

        public LogContextMap(Map<String, String> context) {
            this.context = context;
        }

        @Override
        public String toString() {
            try {
                return new ObjectMapper().writeValueAsString(context);
            } catch (IOException e) {
                return "{}";
            }
        }

        ;
    }
}
