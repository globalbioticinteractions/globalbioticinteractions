package org.eol.globi.util;

import org.eol.globi.service.TermLookupServiceException;

import java.util.List;

public class InteractTypeMapperFactoryWithFallback implements InteractTypeMapperFactory {

    private final List<InteractTypeMapperFactory> factoryAlternatives;

    public InteractTypeMapperFactoryWithFallback(List<InteractTypeMapperFactory> factories) {
        this.factoryAlternatives = factories;
    }

    @Override
    public InteractTypeMapper create() throws TermLookupServiceException {
        InteractTypeMapper mapper = null;
        TermLookupServiceException lastException = null;
        for (InteractTypeMapperFactory factory : factoryAlternatives) {
            try {
                mapper = factory.create();
            } catch (TermLookupServiceException ex) {
                lastException = ex;
            }
        }
        if (mapper == null) {
            throw lastException == null
                    ? new TermLookupServiceException("failed to create interaction type mapper")
                    : lastException;
        }

        return mapper;
    }
}
