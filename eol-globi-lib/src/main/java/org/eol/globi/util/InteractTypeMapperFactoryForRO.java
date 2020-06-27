package org.eol.globi.util;

import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class InteractTypeMapperFactoryForRO implements InteractTypeMapperFactory {

    public static final String IGNORED_LIST_DEFAULT = "/org/globalbioticinteractions/interaction_types_ro_unmapped.csv";
    public static final String SUPPORTED_INTERACTION_TYPES = "/org/globalbioticinteractions/interaction_types_ro.csv";
    public static final String IGNORED_INTERACTION_TYPE_COLUMN_NAME = "interaction_type_ignored";

    @Override
    public InteractTypeMapper create() throws TermLookupServiceException {
        return new InteractTypeMapperImpl(
                getTermIgnoredServiceRO(),
                getTermLookupServiceRO());
    }

    private TermLookupService getTermIgnoredServiceRO() throws TermLookupServiceException {
        return InteractTypeMapperFactoryImpl.getIgnoredTermService(
                new ResourceServiceSingleResource(IGNORED_LIST_DEFAULT),
                IGNORED_INTERACTION_TYPE_COLUMN_NAME,
                URI.create(IGNORED_LIST_DEFAULT));
    }

    private TermLookupService getTermLookupServiceRO() throws TermLookupServiceException {
        return InteractTypeMapperFactoryImpl.getTermLookupService(
                new ResourceServiceSingleResource(SUPPORTED_INTERACTION_TYPES),
                IGNORED_INTERACTION_TYPE_COLUMN_NAME,
                "interaction_type_label",
                "interaction_type_id",
                "interaction_type_id",
                URI.create(IGNORED_LIST_DEFAULT),
                URI.create(SUPPORTED_INTERACTION_TYPES));
    }

    private class ResourceServiceSingleResource implements ResourceService {

        private final String resourceName;

        ResourceServiceSingleResource(String resourceName) {
            this.resourceName = resourceName;
        }

        @Override
        public InputStream retrieve(URI resourceName) throws IOException {
            return getClass().getResourceAsStream(this.resourceName);
        }

    }
}
