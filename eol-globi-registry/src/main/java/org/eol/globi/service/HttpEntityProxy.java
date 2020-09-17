package org.eol.globi.service;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.eol.globi.util.InputStreamFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class HttpEntityProxy implements HttpEntity {

    private final HttpEntity entity;
    private final InputStreamFactory inputStreamFactory;

    public HttpEntityProxy(HttpEntity entity, InputStreamFactory inputStreamFactory) {
        this.entity = entity;
        this.inputStreamFactory = inputStreamFactory;
    }

    @Override
    public boolean isRepeatable() {
        return entity.isRepeatable();
    }

    @Override
    public boolean isChunked() {
        return entity.isChunked();
    }

    @Override
    public long getContentLength() {
        return entity.getContentLength();
    }

    @Override
    public Header getContentType() {
        return entity.getContentType();
    }

    @Override
    public Header getContentEncoding() {
        return entity.getContentEncoding();
    }

    @Override
    public InputStream getContent() throws IOException, UnsupportedOperationException {
        return inputStreamFactory.create(entity.getContent());
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        entity.writeTo(outputStream);
    }

    @Override
    public boolean isStreaming() {
        return entity.isStreaming();
    }

    @Override
    public void consumeContent() throws IOException {
        try (InputStream content = entity.getContent()) {
            //
        }
    }
}
