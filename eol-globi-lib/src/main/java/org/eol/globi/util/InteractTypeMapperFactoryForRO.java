package org.eol.globi.util;

import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;

import java.net.URI;

public class InteractTypeMapperFactoryForRO implements InteractTypeMapperFactory {

    public static final String IGNORED_LIST_DEFAULT = "/org/globalbioticinteractions/interaction_types_ro_unmapped.csv";
    public static final String SUPPORTED_INTERACTION_TYPES = "/org/globalbioticinteractions/interaction_types_ro.csv";
    public static final String IGNORED_INTERACTION_TYPE_COLUMN_NAME = "interaction_type_ignored";
    private final ResourceService resourceService;

    public InteractTypeMapperFactoryForRO(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public InteractTypeMapper create() throws TermLookupServiceException {
        TermLookupService termIgnoredServiceRO = getTermIgnoredServiceRO();
        return new InteractTypeMapperImpl(
                termIgnoredServiceRO,
                getTermLookupServiceRO(termIgnoredServiceRO));
    }

    private TermLookupService getTermIgnoredServiceRO() throws TermLookupServiceException {
        return InteractTypeMapperFactoryImpl.getIgnoredTermService(
                resourceService,
                IGNORED_INTERACTION_TYPE_COLUMN_NAME,
                URI.create(IGNORED_LIST_DEFAULT));
    }

    private TermLookupService getTermLookupServiceRO(TermLookupService termIgnoredServiceRO) throws TermLookupServiceException {
        return InteractTypeMapperFactoryImpl.getTermLookupService(
                termIgnoredServiceRO,
                resourceService,
                "interaction_type_id",
                "interaction_type_label",
                "interaction_type_id",
                URI.create(SUPPORTED_INTERACTION_TYPES));
    }
}
