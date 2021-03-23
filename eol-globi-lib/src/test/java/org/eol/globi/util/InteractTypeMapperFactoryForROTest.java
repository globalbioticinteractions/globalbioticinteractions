package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.TermLookupServiceException;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Stream;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class InteractTypeMapperFactoryForROTest {

    @Test
    public void defaultMapping() throws TermLookupServiceException {

        InteractTypeMapper interactTypeMapper
                = new InteractTypeMapperFactoryForRO().create();

        assertNotNull(interactTypeMapper);

        InteractType interactsWithByName = interactTypeMapper.getInteractType("interactsWith");
        assertThat(interactsWithByName, is(InteractType.INTERACTS_WITH));

        InteractType interactsWithById = interactTypeMapper.getInteractType("http://purl.obolibrary.org/obo/RO_0002437");
        assertThat(interactsWithById, is(InteractType.INTERACTS_WITH));

    }


    @Test
    public void ensureAllInteractTypesAreSupported() throws TermLookupServiceException {
        InteractTypeMapper interactTypeMapper
                = new InteractTypeMapperFactoryForRO().create();

        Arrays.stream(InteractType.values())
                .forEach(value -> {
                    if (StringUtils.equals(PropertyAndValueDictionary.NO_MATCH, value.getIRI())) {
                        assertTrue("[" + value.getLabel() + "] should be ignored", interactTypeMapper.shouldIgnoreInteractionType(value.getLabel()));
                        assertTrue("[" + value.getIRI() + "] should be ignored", interactTypeMapper.shouldIgnoreInteractionType(value.getIRI()));
                    } else {
                        InteractType interactTypeByName = interactTypeMapper.getInteractType(value.getLabel());
                        InteractType interactTypeById = interactTypeMapper.getInteractType(value.getIRI());

                        InteractType expectedType = InteractType.typeOf(value.getIRI());
                        assertThat(interactTypeById, is(expectedType));
                        assertThat(interactTypeByName, is(expectedType));
                        assertFalse("[" + value.getLabel() + "] should not be ignored", interactTypeMapper.shouldIgnoreInteractionType(value.getLabel()));
                        assertFalse("[" + value.getIRI() + "] should not be ignored", interactTypeMapper.shouldIgnoreInteractionType(value.getIRI()));
                    }
                });
    }
}