package org.eol.globi.service;

import org.apache.http.HttpEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.util.EntityUtils;
import org.eol.globi.util.InputStreamFactory;

import java.io.IOException;

public class ResponseHandlerWithInputStreamFactory extends BasicResponseHandler {
    private final InputStreamFactory inputStreamFactory;

    public ResponseHandlerWithInputStreamFactory(InputStreamFactory inputStreamFactory) {
        this.inputStreamFactory = inputStreamFactory;
    }

    @Override
    public String handleEntity(final HttpEntity entity) throws IOException {
        HttpEntityProxy httpEntityProxy = new HttpEntityProxy(entity, inputStreamFactory);
        return EntityUtils.toString(httpEntityProxy);
    }
}
