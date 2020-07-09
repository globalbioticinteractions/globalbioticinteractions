package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Term;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;

import java.util.List;

public class InteractTypeMapperImpl implements InteractTypeMapper {

    private final TermLookupService ignoredTermLookupService;
    private final TermLookupService termMappingService;

    public InteractTypeMapperImpl(TermLookupService ignoredTermLookupService, TermLookupService termMappingService) {
        this.ignoredTermLookupService = ignoredTermLookupService;
        this.termMappingService = termMappingService;
    }

    @Override
    public boolean shouldIgnoreInteractionType(String interactionTypeNameOrId) {
        boolean shouldIgnore = false;
        try {
            List<Term> terms = ignoredTermLookupService.lookupTermByName(interactionTypeNameOrId);
            shouldIgnore = terms != null && !terms.isEmpty();
        } catch (TermLookupServiceException e) {
            //
        }
        return shouldIgnore;
    }

    @Override
    public InteractType getInteractType(String interactionTypeNameOrId) {
        InteractType firstMatchingType = null;
        try {
            List<Term> terms = termMappingService.lookupTermByName(interactionTypeNameOrId);
            if (terms != null && terms.size() > 0) {
                firstMatchingType = InteractType.typeOf(terms.get(0).getId());
            }
        } catch (TermLookupServiceException e) {
            //
        }
        return firstMatchingType;

    }
}
