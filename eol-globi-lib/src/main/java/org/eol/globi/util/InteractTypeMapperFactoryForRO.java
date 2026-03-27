package org.eol.globi.util;

import org.apache.commons.text.CaseUtils;
import org.eol.globi.domain.Term;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class InteractTypeMapperFactoryForRO implements InteractTypeMapperFactory {

    public static final String IGNORED_LIST_DEFAULT = "classpath:/org/globalbioticinteractions/interaction_types_ro_unmapped.csv";
    public static final String SUPPORTED_INTERACTION_TYPES = "classpath:/org/globalbioticinteractions/interaction_types_ro.csv";
    public static final String IGNORED_INTERACTION_TYPE_COLUMN_NAME = "interaction_type_ignored";
    public static final Pattern LETTERS_AND_SPACES = Pattern.compile("[A-Za-z ]+");
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
        TermLookupService termLookupService = InteractTypeMapperFactoryImpl.getTermLookupService(
                termIgnoredServiceRO,
                resourceService,
                "interaction_type_id",
                "interaction_type_label",
                "interaction_type_id",
                URI.create(SUPPORTED_INTERACTION_TYPES));
        return name -> {
            return termLookupService.lookupTermByName(camelCaseForInteractionTypeNameWithOnlyLettersAndSpaces(name));
        };
    }

    private static String camelCaseForInteractionTypeNameWithOnlyLettersAndSpaces(String name) {
        return LETTERS_AND_SPACES.matcher(name).matches() ? CaseUtils.toCamelCase(name, false) : name;
    }
}
