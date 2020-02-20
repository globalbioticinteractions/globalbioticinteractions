package org.eol.globi.util;

import org.apache.commons.io.IOUtils;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TermLookupServiceException;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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
        when(resourceService.retrieve(URI.create("interaction_types.csv")))
                .thenReturn(IOUtils.toInputStream("", StandardCharsets.UTF_8));

        InteractTypeMapperFactory.InteractTypeMapper interactTypeMapper
                = new InteractTypeMapperFactoryWithFallback().create();

        assertNull(interactTypeMapper);

    }

    @Test
    public void createAndIgnoreTermSingleMapper() throws TermLookupServiceException {

        InteractTypeMapperFactory factory1 = Mockito.mock(InteractTypeMapperFactory.class);
        InteractTypeMapperFactory.InteractTypeMapper mapper = Mockito.mock(InteractTypeMapperFactory.InteractTypeMapper.class);

        when(factory1.create()).thenReturn(mapper);

        InteractTypeMapperFactory.InteractTypeMapper interactTypeMapper
                = new InteractTypeMapperFactoryWithFallback(factory1).create();

        assertNotNull(interactTypeMapper);

    }

    @Test
    public void chooseFirstWorkingMapper() throws TermLookupServiceException {

        InteractTypeMapperFactory factory1 = Mockito.mock(InteractTypeMapperFactory.class);
        InteractTypeMapperFactory.InteractTypeMapper mapper1 = Mockito.mock(InteractTypeMapperFactory.InteractTypeMapper.class);
        when(factory1.create()).thenReturn(mapper1);
        InteractTypeMapperFactory factory2 = Mockito.mock(InteractTypeMapperFactory.class);
        InteractTypeMapperFactory.InteractTypeMapper mapper2 = Mockito.mock(InteractTypeMapperFactory.InteractTypeMapper.class);
        when(factory2.create()).thenReturn(mapper2);


        InteractTypeMapperFactory.InteractTypeMapper interactTypeMapper
                = new InteractTypeMapperFactoryWithFallback(factory1, factory2).create();

        assertThat(interactTypeMapper, is(mapper1));

    }

    @Test
    public void createAndIgnoreTermSecondMapper() throws TermLookupServiceException {

        InteractTypeMapperFactory factory1 = Mockito.mock(InteractTypeMapperFactory.class);
        when(factory1.create()).thenThrow(new TermLookupServiceException("kaboom!"));

        InteractTypeMapperFactory.InteractTypeMapper mapper = Mockito.mock(InteractTypeMapperFactory.InteractTypeMapper.class);
        InteractTypeMapperFactory factory2 = Mockito.mock(InteractTypeMapperFactory.class);
        when(factory2.create()).thenReturn(mapper);

        InteractTypeMapperFactory.InteractTypeMapper interactTypeMapper
                = new InteractTypeMapperFactoryWithFallback(factory1, factory2).create();

        assertThat(interactTypeMapper, is(mapper));

    }

    @Test
    public void createAndTermFirstMapperOkSecondMapperFails() throws TermLookupServiceException {

        InteractTypeMapperFactory factory1 = Mockito.mock(InteractTypeMapperFactory.class);
        InteractTypeMapperFactory.InteractTypeMapper mapper = Mockito.mock(InteractTypeMapperFactory.InteractTypeMapper.class);
        when(factory1.create()).thenReturn(mapper);

        InteractTypeMapperFactory factory2 = Mockito.mock(InteractTypeMapperFactory.class);
        when(factory2.create()).thenThrow(new TermLookupServiceException("kaboom!"));

        InteractTypeMapperFactory.InteractTypeMapper interactTypeMapper
                = new InteractTypeMapperFactoryWithFallback(factory1, factory2).create();

        assertThat(interactTypeMapper, is(mapper));

    }

    @Test(expected = TermLookupServiceException.class)
    public void createAndThrow() throws TermLookupServiceException {

        InteractTypeMapperFactory factory1 = Mockito.mock(InteractTypeMapperFactory.class);
        when(factory1.create()).thenThrow(new TermLookupServiceException("kaboom!"));


        InteractTypeMapperFactory.InteractTypeMapper interactTypeMapper
                = new InteractTypeMapperFactoryWithFallback(factory1).create();

    }


}