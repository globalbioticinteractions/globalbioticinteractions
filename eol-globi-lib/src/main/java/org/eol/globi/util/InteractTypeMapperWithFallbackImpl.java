package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Term;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class InteractTypeMapperWithFallbackImpl implements InteractTypeMapperFactory.InteractTypeMapper {

    private final List<InteractTypeMapperFactory.InteractTypeMapper> mappers;

    public InteractTypeMapperWithFallbackImpl(InteractTypeMapperFactory.InteractTypeMapper... mappers) {
        this.mappers = Arrays.asList(mappers);
    }

    @Override
    public boolean shouldIgnoreInteractionType(String interactionTypeNameOrId) {
        Optional<InteractTypeMapperFactory.InteractTypeMapper> first = mappers
                .stream().
                        filter(x -> x.shouldIgnoreInteractionType(interactionTypeNameOrId))
                .findFirst();
        return first.isPresent();
    }

    @Override
    public InteractType getInteractType(String interactionTypeNameOrId) {
        Optional<InteractType> first = mappers
                .stream()
                .map(x -> x.getInteractType(interactionTypeNameOrId))
                .filter(Objects::nonNull)
                .findFirst();
        return first.orElse(null);
    }
}
