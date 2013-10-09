package org.eol.globi.service;

import java.net.URISyntaxException;
import java.net.URL;

public class UberonLookupService extends TermLookupServiceImpl {

    @Override
    public String getMappingURI() {
        URL resource = getClass().getResource("globi-uberon-mapping.txt");
        try {
            return resource.toURI().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(("failed to configure service for [" + resource.toString() + "]"));
        }
    }
}
