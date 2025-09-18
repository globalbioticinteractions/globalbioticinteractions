package org.eol.globi.server.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.CypherUtil;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class CypherHttpMessageConverter extends AbstractHttpMessageConverter<CypherQuery> {

    public CypherHttpMessageConverter() {
        super(MediaType.parseMediaType("application/json;charset=UTF-8"),
                MediaType.parseMediaType("application/ld+json;charset=UTF-8"),
                MediaType.parseMediaType("text/html;charset=UTF-8"),
                MediaType.parseMediaType("text/vnd.graphviz;charset=UTF-8"),
                MediaType.parseMediaType("text/plain;charset=UTF-8"),
                MediaType.parseMediaType("text/tab-separated-values;charset=UTF-8"),
                MediaType.parseMediaType("text/csv;charset=UTF-8"),
                MediaType.parseMediaType("image/svg+xml;charset=UTF-8"));
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return CypherQuery.class.equals(clazz);
    }

    @Override
    protected boolean canRead(MediaType mediaType) {
        return false;
    }

    @Override
    protected CypherQuery readInternal(Class<? extends CypherQuery> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return null;
    }

    @Override
    protected void writeInternal(CypherQuery cypherQuery, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        MediaType contentType = outputMessage.getHeaders().getContentType();
        ResultFormatter formatter = new ResultFormatterFactory().create(contentType);
        if (formatter == null) {
            throw new IOException("found unsupported return format type request for [" + contentType + "]");
        }

        cypherQuery = optimizeQueryForType(cypherQuery, formatter);

        if (formatter instanceof ResultFormatterStreaming) {
            HttpResponse res = CypherUtil.execute(cypherQuery);
            try (InputStream is = IOUtils.buffer(res.getEntity().getContent());
                 OutputStream os = IOUtils.buffer(outputMessage.getBody())) {
                ((ResultFormatterStreaming) formatter).format(is, os);
                os.flush();
            }
        } else {
            String result = CypherUtil.executeRemote(cypherQuery);
            StreamUtils.copy(
                    formatter.format(result),
                    (contentType == null || contentType.getCharset() == null) ? StandardCharsets.UTF_8 : contentType.getCharset(),
                    outputMessage.getBody()
            );
        }
    }

    static CypherQuery optimizeQueryForType(CypherQuery cypherQuery, ResultFormatter formatter) {
        if (formatter instanceof ResultFormatterSVG) {
            cypherQuery = attemptQueryRewrite(cypherQuery, " LIMIT ");
            cypherQuery = attemptQueryRewrite(cypherQuery, " limit ");
        }
        return cypherQuery;
    }

    private static CypherQuery attemptQueryRewrite(CypherQuery cypherQuery, String limit_) {
        if (StringUtils.contains(cypherQuery.getQuery(), limit_)) {
            String[] queryParts = StringUtils.splitByWholeSeparator(cypherQuery.getQuery(), limit_);
            cypherQuery = new CypherQuery(queryParts[0] + limit_ + "1", cypherQuery.getParams());
        }
        return cypherQuery;
    }

}
