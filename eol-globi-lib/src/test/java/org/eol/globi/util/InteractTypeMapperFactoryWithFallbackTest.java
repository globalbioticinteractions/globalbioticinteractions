package org.eol.globi.util;

import org.apache.commons.io.IOUtils;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TermLookupServiceException;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class InteractTypeMapperFactoryWithFallbackTest {

    @Test(expected = TermLookupServiceException.class)
    public void createAndIgnoreTermNoMapper() throws TermLookupServiceException, IOException {

        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(IOUtils.toInputStream("observation_field_id\nshouldBeIgnored", StandardCharsets.UTF_8))
                .thenReturn(IOUtils.toInputStream("observation_field_id\nshouldBeIgnored", StandardCharsets.UTF_8));
        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream("", StandardCharsets.UTF_8));

        InteractTypeMapper interactTypeMapper
                = new InteractTypeMapperFactoryWithFallback().create();

        assertNull(interactTypeMapper);

    }

    @Test
    public void createAndIgnoreTermSingleMapper() throws TermLookupServiceException {

        InteractTypeMapperFactory factory1 = Mockito.mock(InteractTypeMapperFactory.class);
        InteractTypeMapper mapper = Mockito.mock(InteractTypeMapper.class);

        when(factory1.create()).thenReturn(mapper);

        InteractTypeMapper interactTypeMapper
                = new InteractTypeMapperFactoryWithFallback(factory1).create();

        assertNotNull(interactTypeMapper);

    }

    @Test
    public void chooseFirstWorkingMapper() throws TermLookupServiceException {

        InteractTypeMapperFactory factory1 = Mockito.mock(InteractTypeMapperFactory.class);
        InteractTypeMapper mapper1 = Mockito.mock(InteractTypeMapper.class);
        when(factory1.create()).thenReturn(mapper1);
        InteractTypeMapperFactory factory2 = Mockito.mock(InteractTypeMapperFactory.class);
        InteractTypeMapper mapper2 = Mockito.mock(InteractTypeMapper.class);
        when(factory2.create()).thenReturn(mapper2);


        InteractTypeMapper interactTypeMapper
                = new InteractTypeMapperFactoryWithFallback(factory1, factory2).create();

        assertThat(interactTypeMapper, is(mapper1));

    }

    @Test
    public void createAndIgnoreTermSecondMapper() throws TermLookupServiceException {

        InteractTypeMapperFactory factory1 = Mockito.mock(InteractTypeMapperFactory.class);
        when(factory1.create()).thenThrow(new TermLookupServiceException("kaboom!"));

        InteractTypeMapper mapper = Mockito.mock(InteractTypeMapper.class);
        InteractTypeMapperFactory factory2 = Mockito.mock(InteractTypeMapperFactory.class);
        when(factory2.create()).thenReturn(mapper);

        InteractTypeMapper interactTypeMapper
                = new InteractTypeMapperFactoryWithFallback(factory1, factory2).create();

        assertThat(interactTypeMapper, is(mapper));

    }

    @Test
    public void createAndTermFirstMapperOkSecondMapperFails() throws TermLookupServiceException {

        InteractTypeMapperFactory factory1 = Mockito.mock(InteractTypeMapperFactory.class);
        InteractTypeMapper mapper = Mockito.mock(InteractTypeMapper.class);
        when(factory1.create()).thenReturn(mapper);

        InteractTypeMapperFactory factory2 = Mockito.mock(InteractTypeMapperFactory.class);
        when(factory2.create()).thenThrow(new TermLookupServiceException("kaboom!"));

        InteractTypeMapper interactTypeMapper
                = new InteractTypeMapperFactoryWithFallback(factory1, factory2).create();

        assertThat(interactTypeMapper, is(mapper));

    }

    @Test(expected = TermLookupServiceException.class)
    public void createAndThrow() throws TermLookupServiceException {

        InteractTypeMapperFactory factory1 = Mockito.mock(InteractTypeMapperFactory.class);
        when(factory1.create()).thenThrow(new TermLookupServiceException("kaboom!"));


        InteractTypeMapper interactTypeMapper
                = new InteractTypeMapperFactoryWithFallback(factory1).create();

    }


}