package org.eol.globi.util;

import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InteractTypeMapperFactoryImplTest {

    @Test
    public void createAndIgnoreTerm() throws TermLookupServiceException, IOException {

        InteractTypeMapperImpl interactTypeMapper = createIgnoreServiceMock();
        assertTrue(interactTypeMapper.shouldIgnoreInteractionType("shouldBeIgnored"));

    }

    public InteractTypeMapperImpl createIgnoreServiceMock() throws IOException, TermLookupServiceException {
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(IOUtils.toInputStream("provided_interaction_type_id\nshouldBeIgnored", StandardCharsets.UTF_8))
                .thenReturn(IOUtils.toInputStream("provided_interaction_type_id\nshouldBeIgnored", StandardCharsets.UTF_8));
        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream(getTestMap(), StandardCharsets.UTF_8));

        TermLookupService ignoreTermService = InteractTypeMapperFactoryImpl.getIgnoredTermService(
                resourceService,
                "provided_interaction_type_id",
                URI.create("interaction_types_ignored.csv"));

        TermLookupService termMapper = Mockito.mock(TermLookupService.class);
        verify(termMapper, never()).lookupTermByName(anyString());

        return new InteractTypeMapperImpl(ignoreTermService, termMapper);
    }

    @Test
    public void duplicateProvidedLabelButSeparateProvidedIds() throws TermLookupServiceException, IOException {

        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(IOUtils.toInputStream("provided_interaction_type_id\nshouldBeIgnored", StandardCharsets.UTF_8))
                .thenReturn(IOUtils.toInputStream("provided_interaction_type_id\nshouldBeIgnored", StandardCharsets.UTF_8));
        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream("provided_interaction_type_label,provided_interaction_type_id,mapped_to_interaction_type_label,mapped_to_interaction_type_id" +
                        "\nshouldBeMapped,id1,interactsWith, http://purl.obolibrary.org/obo/RO_0002437\n" +
                        "\nshouldBeMapped,id2," + InteractType.ATE.getLabel() + "," + InteractType.ATE.getIRI()
                        , StandardCharsets.UTF_8));

        InteractTypeMapperFactoryImpl interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);
        InteractTypeMapper interactTypeMapper = interactTypeMapperFactory.create();
        assertThat(interactTypeMapper.getInteractType("id1"), is(InteractType.INTERACTS_WITH));
        assertThat(interactTypeMapper.getInteractType("id2"), is(InteractType.ATE));
        assertThat(interactTypeMapper.getInteractType("shouldBeMapped"), is(InteractType.INTERACTS_WITH));

    }

    @Test
    public void createAndIgnoreBlankTerm() throws TermLookupServiceException, IOException {
        InteractTypeMapper interactTypeMapper = createIgnoreServiceMock();
        assertTrue(interactTypeMapper.shouldIgnoreInteractionType(""));

    }

    private String getTestMap() {
        return "provided_interaction_type_label,provided_interaction_type_id,mapped_to_interaction_type_label,mapped_to_interaction_type_id\n" +
                "shouldBeMapped,,interactsWith, http://purl.obolibrary.org/obo/RO_0002437";
    }

    @Test(expected = TermLookupServiceException.class)
    public void createAndNoMappingResource() throws TermLookupServiceException, IOException {
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(IOUtils.toInputStream("interaction_type_ignored\nshouldBeIgnored", StandardCharsets.UTF_8));

        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv"))).thenThrow(new IOException("kaboom!"));
        InteractTypeMapperFactoryImpl interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);

        try {
            InteractTypeMapper interactTypeMapper = interactTypeMapperFactory.create();
            interactTypeMapper.getInteractType("shouldBeIgnored");
        } catch (TermLookupServiceException ex) {
            assertThat(ex.getMessage(), is("failed to load interaction mapping from [interaction_types_mapping.csv]"));
            throw ex;
        }
    }

    @Test(expected = TermLookupServiceException.class)
    public void createAndNoIgnoreResource() throws TermLookupServiceException, IOException {

        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv"))).thenThrow(new IOException("kaboom!"));
        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv"))).thenThrow(new IOException("kaboom!"));
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
                .thenReturn(IOUtils.toInputStream("interaction_type_ignored\nshouldBeIgnored", StandardCharsets.UTF_8));
        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream(getTestMap(), StandardCharsets.UTF_8));

        InteractTypeMapperFactoryImpl interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);
        InteractTypeMapper interactTypeMapper = interactTypeMapperFactory.create();
        assertThat(interactTypeMapper.getInteractType("shouldBeMapped"), is(InteractType.INTERACTS_WITH));

    }

}