package org.eol.globi.util;

import org.eol.globi.domain.InteractType;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class InteractTypeMapperWithFallbackImplTest {

    @Test
    public void firstMapperGetToMap() {

        InteractTypeMapper mapper1 = Mockito.mock(InteractTypeMapper.class);
        when(mapper1.getInteractType("eats")).thenReturn(InteractType.ATE);
        InteractTypeMapper mapper2 = Mockito.mock(InteractTypeMapper.class);
        when(mapper2.getInteractType("eats")).thenThrow(new RuntimeException("kaboom!"));

        InteractTypeMapper interactTypeMapperWithFallback
                = new InteractTypeMapperWithFallbackImpl(mapper1, mapper2);

        assertThat(interactTypeMapperWithFallback.getInteractType("eats"), is(InteractType.ATE));


    }

    @Test
    public void secondMapperGetToMap() {
        InteractTypeMapper second = Mockito.mock(InteractTypeMapper.class);
        when(second.getInteractType("eats")).thenReturn(InteractType.ATE);
        when(second.shouldIgnoreInteractionType("eats")).thenReturn(false);
        InteractTypeMapper first = Mockito.mock(InteractTypeMapper.class);
        when(first.getInteractType("eats")).thenReturn(null);
        when(first.shouldIgnoreInteractionType("eats")).thenReturn(false);

        InteractTypeMapper interactTypeMapperWithFallback
                = new InteractTypeMapperWithFallbackImpl(first, second);

        assertThat(interactTypeMapperWithFallback.getInteractType("eats"), is(InteractType.ATE));
    }

    @Test
    public void secondMapperDoesNotGetToMapBecauseTermIgnoredByFirst() {
        InteractTypeMapper first = Mockito.mock(InteractTypeMapper.class);
        when(first.getInteractType("eats")).thenReturn(null);
        when(first.shouldIgnoreInteractionType("eats")).thenReturn(true);

        InteractTypeMapper second = Mockito.mock(InteractTypeMapper.class);
        when(second.getInteractType("eats")).thenReturn(InteractType.ATE);
        when(second.shouldIgnoreInteractionType("eats")).thenReturn(false);

        InteractTypeMapper interactTypeMapperWithFallback
                = new InteractTypeMapperWithFallbackImpl(first, second);

        assertThat(interactTypeMapperWithFallback.getInteractType("eats"), is(nullValue()));
    }

    @Test
    public void firstMapperToIgnore() {
        InteractTypeMapper mapper1 = Mockito.mock(InteractTypeMapper.class);
        when(mapper1.shouldIgnoreInteractionType("eats")).thenReturn(true);
        InteractTypeMapper mapper2 = Mockito.mock(InteractTypeMapper.class);
        when(mapper2.shouldIgnoreInteractionType("eats")).thenReturn(false);

        InteractTypeMapper interactTypeMapperWithFallback
                = new InteractTypeMapperWithFallbackImpl(mapper1, mapper2);

        assertThat(interactTypeMapperWithFallback.shouldIgnoreInteractionType("eats"), is(true));
    }

    @Test
    public void secondMapperToIgnore() {
        InteractTypeMapper mapper1 = Mockito.mock(InteractTypeMapper.class);
        when(mapper1.shouldIgnoreInteractionType("eats")).thenReturn(false);
        InteractTypeMapper mapper2 = Mockito.mock(InteractTypeMapper.class);
        when(mapper2.shouldIgnoreInteractionType("eats")).thenReturn(true);

        InteractTypeMapper interactTypeMapperWithFallback
                = new InteractTypeMapperWithFallbackImpl(mapper1, mapper2);

        assertThat(interactTypeMapperWithFallback.shouldIgnoreInteractionType("eats"), is(true));
    }

    @Test
    public void noMapperToIgnore() {
        InteractTypeMapper mapper1 = Mockito.mock(InteractTypeMapper.class);
        when(mapper1.shouldIgnoreInteractionType("eats")).thenReturn(false);
        InteractTypeMapper mapper2 = Mockito.mock(InteractTypeMapper.class);
        when(mapper2.shouldIgnoreInteractionType("eats")).thenReturn(false);

        InteractTypeMapper interactTypeMapperWithFallback
                = new InteractTypeMapperWithFallbackImpl(mapper1, mapper2);

        assertThat(interactTypeMapperWithFallback.shouldIgnoreInteractionType("eats"), is(false));
    }


}