package org.globalbioticinteractions.dataset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.ResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class JSONConfigurer implements DatasetConfigurer {

    @Override
    public JsonNode configure(ResourceService resourceService, URI configURI) throws IOException {
        try (InputStream inputStream = resourceService.retrieve(configURI)) {
            if (inputStream == null) {
                throw new IOException("failed to access resource [" + configURI.toString() + "]");
            }
            String descriptor = getContent(configURI, inputStream);
            if (StringUtils.isBlank(descriptor)) {
                throw new IOException("no content found at [" + configURI.toString() + "]");
            }
            return new ObjectMapper().readTree(descriptor);
        }
    }

    private String getContent(URI uri, InputStream input) throws IOException {
        try {
            return IOUtils.toString(input, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IOException("failed to find [" + uri + "]", ex);
        }
    }
}
