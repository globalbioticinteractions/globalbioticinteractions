package org.eol.globi.util;

import org.eol.globi.domain.InteractType;
import org.eol.globi.service.TermLookupServiceException;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class InteractTypeMapperWithFallbackImplTest {

    @Test
    public void firstMapperGetToMap() throws TermLookupServiceException {

        InteractTypeMapperFactory.InteractTypeMapper mapper1 = Mockito.mock(InteractTypeMapperFactory.InteractTypeMapper.class);
        when(mapper1.getInteractType("eats")).thenReturn(InteractType.ATE);
        InteractTypeMapperFactory.InteractTypeMapper mapper2 = Mockito.mock(InteractTypeMapperFactory.InteractTypeMapper.class);
        when(mapper2.getInteractType("eats")).thenThrow(new RuntimeException("kaboom!"));

        InteractTypeMapperFactory.InteractTypeMapper interactTypeMapperWithFallback
                = new InteractTypeMapperWithFallbackImpl(mapper1, mapper2);

        assertThat(interactTypeMapperWithFallback.getInteractType("eats"), is(InteractType.ATE));


    }

    @Test
    public void secondMapperGetToMap() {
        InteractTypeMapperFactory.InteractTypeMapper mapper1 = Mockito.mock(InteractTypeMapperFactory.InteractTypeMapper.class);
        when(mapper1.getInteractType("eats")).thenReturn(InteractType.ATE);
        InteractTypeMapperFactory.InteractTypeMapper mapper2 = Mockito.mock(InteractTypeMapperFactory.InteractTypeMapper.class);
        when(mapper2.getInteractType("eats")).thenReturn(null);

        InteractTypeMapperFactory.InteractTypeMapper interactTypeMapperWithFallback
                = new InteractTypeMapperWithFallbackImpl(mapper2, mapper1);

        assertThat(interactTypeMapperWithFallback.getInteractType("eats"), is(InteractType.ATE));
    }

    @Test
    public void firstMapperToIgnore() {
        InteractTypeMapperFactory.InteractTypeMapper mapper1 = Mockito.mock(InteractTypeMapperFactory.InteractTypeMapper.class);
        when(mapper1.shouldIgnoreInteractionType("eats")).thenReturn(true);
        InteractTypeMapperFactory.InteractTypeMapper mapper2 = Mockito.mock(InteractTypeMapperFactory.InteractTypeMapper.class);
        when(mapper2.shouldIgnoreInteractionType("eats")).thenReturn(false);

        InteractTypeMapperFactory.InteractTypeMapper interactTypeMapperWithFallback
                = new InteractTypeMapperWithFallbackImpl(mapper1, mapper2);

        assertThat(interactTypeMapperWithFallback.shouldIgnoreInteractionType("eats"), is(true));
    }

    @Test
    public void secondMapperToIgnore() {
        InteractTypeMapperFactory.InteractTypeMapper mapper1 = Mockito.mock(InteractTypeMapperFactory.InteractTypeMapper.class);
        when(mapper1.shouldIgnoreInteractionType("eats")).thenReturn(false);
        InteractTypeMapperFactory.InteractTypeMapper mapper2 = Mockito.mock(InteractTypeMapperFactory.InteractTypeMapper.class);
        when(mapper2.shouldIgnoreInteractionType("eats")).thenReturn(true);

        InteractTypeMapperFactory.InteractTypeMapper interactTypeMapperWithFallback
                = new InteractTypeMapperWithFallbackImpl(mapper1, mapper2);

        assertThat(interactTypeMapperWithFallback.shouldIgnoreInteractionType("eats"), is(true));
    }

    @Test
    public void noMapperToIgnore() {
        InteractTypeMapperFactory.InteractTypeMapper mapper1 = Mockito.mock(InteractTypeMapperFactory.InteractTypeMapper.class);
        when(mapper1.shouldIgnoreInteractionType("eats")).thenReturn(false);
        InteractTypeMapperFactory.InteractTypeMapper mapper2 = Mockito.mock(InteractTypeMapperFactory.InteractTypeMapper.class);
        when(mapper2.shouldIgnoreInteractionType("eats")).thenReturn(false);

        InteractTypeMapperFactory.InteractTypeMapper interactTypeMapperWithFallback
                = new InteractTypeMapperWithFallbackImpl(mapper1, mapper2);

        assertThat(interactTypeMapperWithFallback.shouldIgnoreInteractionType("eats"), is(false));
    }


}