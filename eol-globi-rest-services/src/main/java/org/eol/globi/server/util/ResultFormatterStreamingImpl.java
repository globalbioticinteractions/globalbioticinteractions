package org.eol.globi.server.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.FIELD_NAME;
import static com.fasterxml.jackson.core.JsonToken.START_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.VALUE_FALSE;
import static com.fasterxml.jackson.core.JsonToken.VALUE_NULL;
import static com.fasterxml.jackson.core.JsonToken.VALUE_NUMBER_FLOAT;
import static com.fasterxml.jackson.core.JsonToken.VALUE_NUMBER_INT;
import static com.fasterxml.jackson.core.JsonToken.VALUE_STRING;
import static com.fasterxml.jackson.core.JsonToken.VALUE_TRUE;


public abstract class ResultFormatterStreamingImpl implements ResultFormatterStreaming {

    @Override
    public void format(InputStream is, OutputStream os) throws ResultFormattingException {
        //is = cacheResults(is);

        try (InputStream inputStream = is) {
            JsonFactory factory = new JsonFactory();
            JsonParser jsonParser = factory.createParser(inputStream);
            JsonToken token;
            while (!jsonParser.isClosed()
                    && (token = jsonParser.nextToken()) != null) {
                if (FIELD_NAME.equals(token) && "columns".equals(jsonParser.getCurrentName())) {
                    handleHeader(os, jsonParser);
                } else if (FIELD_NAME.equals(token) && "data".equals(jsonParser.getCurrentName())) {
                    handleRows(os, jsonParser);
                } else if (FIELD_NAME.equals(token) && "errors".equals(jsonParser.getCurrentName())) {
                    throw new ResultFormattingException("failed to retrieve results");
                }
            }
        } catch (IOException e) {
            throw new ResultFormattingException("failed to format incoming stream", e);
        }
    }

    public InputStream cacheResults(InputStream is) throws ResultFormattingException {
        File tempFile;
        try (InputStream is2 = is){
            tempFile = File.createTempFile("cypher", "json");
            System.out.println("logging to [" + tempFile.getAbsolutePath() + "]");
            IOUtils.copy(is2, new FileOutputStream(tempFile));
        } catch (IOException e) {
            throw new ResultFormattingException("bla", e);
        }

        try {
            is = new FileInputStream(tempFile);
        } catch (FileNotFoundException e) {
            throw new ResultFormattingException("failed to open tmpfile", e);
        }
        return is;
    }

    abstract protected void handleRows(OutputStream os, JsonParser jsonParser) throws IOException;

    abstract protected void handleHeader(OutputStream os, JsonParser jsonParser) throws IOException;


}
