package org.eol.globi.util;

import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TermLookupServiceException;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class InteractTypeMapperFactoryImplTest {

    @Test
    public void createAndIgnoreTerm() throws TermLookupServiceException, IOException {

        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(IOUtils.toInputStream("observation_field_id\nshouldBeIgnored", StandardCharsets.UTF_8))
                .thenReturn(IOUtils.toInputStream("observation_field_id\nshouldBeIgnored", StandardCharsets.UTF_8));
        when(resourceService.retrieve(URI.create("interaction_types.csv")))
                .thenReturn(IOUtils.toInputStream(getTestMap(), StandardCharsets.UTF_8));

        InteractTypeMapperFactoryImpl interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);
        InteractTypeMapperFactory.InteractTypeMapper interactTypeMapper = interactTypeMapperFactory.create();
        assertTrue(interactTypeMapper.shouldIgnoreInteractionType("shouldBeIgnored"));

    }

    @Test
    public void duplicateProvidedLabelButSeparateProvidedIds() throws TermLookupServiceException, IOException {

        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(IOUtils.toInputStream("observation_field_id\nshouldBeIgnored", StandardCharsets.UTF_8))
                .thenReturn(IOUtils.toInputStream("observation_field_id\nshouldBeIgnored", StandardCharsets.UTF_8));
        when(resourceService.retrieve(URI.create("interaction_types.csv")))
                .thenReturn(IOUtils.toInputStream("observation_field_name,observation_field_id,interaction_type_label,interaction_type_id" +
                        "\nshouldBeMapped,id1,interactsWith, http://purl.obolibrary.org/obo/RO_0002437\n" +
                        "\nshouldBeMapped,id2," + InteractType.ATE.getLabel() + "," + InteractType.ATE.getIRI()
                        , StandardCharsets.UTF_8));

        InteractTypeMapperFactoryImpl interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);
        InteractTypeMapperFactory.InteractTypeMapper interactTypeMapper = interactTypeMapperFactory.create();
        assertThat(interactTypeMapper.getInteractType("id1"), is(InteractType.INTERACTS_WITH));
        assertThat(interactTypeMapper.getInteractType("id2"), is(InteractType.ATE));
        assertThat(interactTypeMapper.getInteractType("shouldBeMapped"), is(InteractType.INTERACTS_WITH));

    }

    @Test
    public void createAndIgnoreBlankTerm() throws TermLookupServiceException, IOException {

        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(IOUtils.toInputStream("observation_field_id\nshouldBeIgnored", StandardCharsets.UTF_8));
        when(resourceService.retrieve(URI.create("interaction_types.csv")))
                .thenReturn(IOUtils.toInputStream(getTestMap(), StandardCharsets.UTF_8));

        InteractTypeMapperFactoryImpl interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);
        InteractTypeMapperFactory.InteractTypeMapper interactTypeMapper = interactTypeMapperFactory.create();
        assertTrue(interactTypeMapper.shouldIgnoreInteractionType(""));

    }

    private String getTestMap() {
        return "observation_field_name,observation_field_id,interaction_type_label,interaction_type_id\n" +
                "shouldBeMapped,,interactsWith, http://purl.obolibrary.org/obo/RO_0002437";
    }

    @Test(expected = TermLookupServiceException.class)
    public void createAndNoMappingResource() throws TermLookupServiceException, IOException {
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(IOUtils.toInputStream("observation_field_id\nshouldBeIgnored", StandardCharsets.UTF_8));

        when(resourceService.retrieve(URI.create("interaction_types.csv"))).thenThrow(new IOException("kaboom!"));
        InteractTypeMapperFactoryImpl interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);

        try {
            InteractTypeMapperFactory.InteractTypeMapper interactTypeMapper = interactTypeMapperFactory.create();
            interactTypeMapper.getInteractType("shouldBeIgnored");
        } catch (TermLookupServiceException ex) {
            assertThat(ex.getMessage(), is("failed to load interaction mapping from [interaction_types.csv]"));
            throw ex;
        }
    }

    @Test(expected = TermLookupServiceException.class)
    public void createAndNoIgnoreResource() throws TermLookupServiceException, IOException {

        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv"))).thenThrow(new IOException("kaboom!"));
        when(resourceService.retrieve(URI.create("interaction_types.csv"))).thenThrow(new IOException("kaboom!"));
        InteractTypeMapperFactoryImpl interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);
        try {
            interactTypeMapperFactory.create();
        } catch (TermLookupServiceException ex) {
            assertThat(ex.getMessage(), is("failed to load ignored interaction types from [interaction_types_ignored.csv]"));
            throw ex;
        }

    }

    @Test
    public void createAndMapTerm() throws TermLookupServiceException, IOException {

        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(IOUtils.toInputStream("ignored\nshouldBeIgnored", StandardCharsets.UTF_8));
        when(resourceService.retrieve(URI.create("interaction_types.csv")))
                .thenReturn(IOUtils.toInputStream(getTestMap(), StandardCharsets.UTF_8));

        InteractTypeMapperFactoryImpl interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);
        InteractTypeMapperFactory.InteractTypeMapper interactTypeMapper = interactTypeMapperFactory.create();
        assertThat(interactTypeMapper.getInteractType("shouldBeMapped"), is(InteractType.INTERACTS_WITH));

    }

}