package org.eol.globi.service;

import java.net.URISyntaxException;
import java.net.URL;

public class UberonLookupService extends EnvoServiceImpl {

    public UberonLookupService() {
        URL resource = getClass().getResource("globi-uberon-mapping.txt");
        try {
            setMappingURI(resource.toURI().toString());
        } catch (URISyntaxException e) {
            throw new RuntimeException(("failed to configure service for [" + resource.toString() + "]"));
        }
    }

}
