package org.eol.globi.util;

import org.eol.globi.service.TermLookupServiceException;

import java.util.Arrays;
import java.util.List;

public class InteractTypeMapperFactoryWithFallback implements InteractTypeMapperFactory {

    private final List<InteractTypeMapperFactory> factoryAlternatives;

    public InteractTypeMapperFactoryWithFallback(InteractTypeMapperFactory... factory) {
        this.factoryAlternatives = Arrays.asList(factory);
    }

    @Override
    public InteractTypeMapper create() throws TermLookupServiceException {
        InteractTypeMapper mapper = null;
        TermLookupServiceException lastException = null;
        for (InteractTypeMapperFactory factory : factoryAlternatives) {
            try {
                mapper = factory.create();
                if (mapper != null) {
                    break;
                }
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
