package org.eol.globi.service;

public class EnvoLookupService extends TermLookupServiceImpl {
    @Override
    public String getMappingURI() {
        return "http://purl.obolibrary.org/obo/envo/mappings/spire-mapping.txt";
    }
}
