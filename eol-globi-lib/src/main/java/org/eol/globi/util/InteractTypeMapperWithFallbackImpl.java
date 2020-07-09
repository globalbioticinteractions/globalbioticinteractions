package org.eol.globi.util;

import org.eol.globi.domain.InteractType;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class InteractTypeMapperWithFallbackImpl implements InteractTypeMapper {

    private final List<InteractTypeMapper> mappers;

    public InteractTypeMapperWithFallbackImpl(InteractTypeMapper... mappers) {
        this.mappers = Arrays.asList(mappers);
    }

    @Override
    public boolean shouldIgnoreInteractionType(String interactionTypeNameOrId) {
        // combine ignore lists - so if any of the mappers ignores a term, the term will be ignored.
        Optional<InteractTypeMapper> first = mappers
                .stream().
                        filter(x -> x.shouldIgnoreInteractionType(interactionTypeNameOrId))
                .findFirst();
        return first.isPresent();
    }

    @Override
    public InteractType getInteractType(String interactionTypeNameOrId) {
        final Optional<InteractTypeMapper> lastMapper = mappers
                .stream()
                .filter(x -> x.shouldIgnoreInteractionType(interactionTypeNameOrId))
                .findFirst();

        int lastIndex = mappers.size() - 1;
        if (lastMapper.isPresent()) {
            lastIndex = mappers.indexOf(lastMapper.get());
        }
        Optional<InteractType> first = mappers.subList(0, lastIndex + 1)
                .stream()
                .map(x -> x.getInteractType(interactionTypeNameOrId))
                .filter(Objects::nonNull)
                .findFirst();
        return first.orElse(null);
    }
}
