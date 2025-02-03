package org.eol.globi.util;

import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceConfigurationException;
import org.eol.globi.service.TermLookupServiceException;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
    public void doNotIgnoreBlankTermsByDefault() throws TermLookupServiceException, IOException {
        InteractTypeMapper interactTypeMapper = createIgnoreServiceMock();
        assertFalse(interactTypeMapper.shouldIgnoreInteractionType(""));
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

    private String getTestMap() {
        return "provided_interaction_type_label,provided_interaction_type_id,mapped_to_interaction_type_label,mapped_to_interaction_type_id\n" +
                "shouldBeMapped,,interactsWith, http://purl.obolibrary.org/obo/RO_0002437";
    }

    @Test
    public void createAndNoMappingResource() throws TermLookupServiceException, IOException {
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(IOUtils.toInputStream("interaction_type_ignored\nshouldBeIgnored", StandardCharsets.UTF_8))
                .thenReturn(IOUtils.toInputStream("interaction_type_ignored\nshouldBeIgnored", StandardCharsets.UTF_8));

        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenThrow(new IOException("kaboom!"));

        InteractTypeMapperFactory interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);

        InteractTypeMapper interactTypeMapper = interactTypeMapperFactory.create();
        assertNull(interactTypeMapper.getInteractType("shouldBeIgnored"));
        assertTrue(interactTypeMapper.shouldIgnoreInteractionType("shouldBeIgnored"));
    }

    @Test
    public void createAndFailedToAccessMappingResource() throws TermLookupServiceException, IOException {
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(IOUtils.toInputStream("interaction_type_ignored\nshouldBeIgnored", StandardCharsets.UTF_8))
                .thenReturn(IOUtils.toInputStream("interaction_type_ignored\nshouldBeIgnored", StandardCharsets.UTF_8));

        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenReturn(null);

        InteractTypeMapperFactory interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);

        InteractTypeMapper interactTypeMapper = interactTypeMapperFactory.create();
        assertTrue(interactTypeMapper.shouldIgnoreInteractionType("shouldBeIgnored"));
    }


    @Test
    public void createMappingWithQuotes() throws TermLookupServiceException, IOException {
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(IOUtils.toInputStream("interaction_type_ignored\nshouldBeIgnored", StandardCharsets.UTF_8));

        String mapping = "provided_interaction_type_label,provided_interaction_type_id,mapped_to_interaction_type_label,mapped_to_interaction_type_id" +
                "\n\\\"associates with\\\",,testing123,http://purl.obolibrary.org/obo/RO_0002437";

        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream(mapping, StandardCharsets.UTF_8));
        InteractTypeMapperFactory interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);

        InteractTypeMapper interactTypeMapper = interactTypeMapperFactory.create();
        final InteractType interactType = interactTypeMapper
                .getInteractType("\"associates with\"");
        assertThat(interactType, is(InteractType.INTERACTS_WITH));

    }

    @Test
    public void createMappingWithQuotes2() throws TermLookupServiceException, IOException {
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(IOUtils.toInputStream("interaction_type_ignored\nshouldBeIgnored", StandardCharsets.UTF_8));

        String mapping = "provided_interaction_type_label,provided_interaction_type_id,mapped_to_interaction_type_label,mapped_to_interaction_type_id" +
                "\nassociates\"\" with\"\",,testing123,http://purl.obolibrary.org/obo/RO_0002437";

        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream(mapping, StandardCharsets.UTF_8));
        InteractTypeMapperFactory interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);

        InteractTypeMapper interactTypeMapper = interactTypeMapperFactory.create();
        final InteractType interactType = interactTypeMapper
                .getInteractType("\\\"associates with\\\"");
        assertThat(interactType, is(InteractType.INTERACTS_WITH));

    }

    @Test
    public void createBlankMapping() throws TermLookupServiceException, IOException {
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(IOUtils.toInputStream("interaction_type_ignored\nshouldBeIgnored", StandardCharsets.UTF_8));

        String mapping = "provided_interaction_type_label,provided_interaction_type_id,mapped_to_interaction_type_label,mapped_to_interaction_type_id" +
                "\n,,interactsWith, http://purl.obolibrary.org/obo/RO_0002437";

        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream(mapping, StandardCharsets.UTF_8));
        InteractTypeMapperFactory interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);

        assertThat(interactTypeMapperFactory
                        .create()
                        .getInteractType(""),
                is(InteractType.INTERACTS_WITH));
    }

    @Test
    public void createDrinkingMapping() throws TermLookupServiceException, IOException {
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(null);

        String mapping = "provided_interaction_type_label,provided_interaction_type_id,mapped_to_interaction_type_label,mapped_to_interaction_type_id\n" +
                "drinking,http://purl.obolibrary.org/obo/OMIT_0005582,eats,http://purl.obolibrary.org/obo/RO_0002470\n";

        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream(mapping, StandardCharsets.UTF_8));
        InteractTypeMapperFactory interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);

        InteractTypeMapper interactTypeMapper = interactTypeMapperFactory
                .create();

        assertThat(interactTypeMapper
                        .getInteractType("http://purl.obolibrary.org/obo/OMIT_0005582"),
                is(InteractType.ATE));

        assertThat(interactTypeMapper
                        .getInteractType("drinking"),
                is(InteractType.ATE));
    }

    @Test(expected = TermLookupServiceException.class)
    public void throwOnDuplicateMapping() throws TermLookupServiceException, IOException {
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(null);

        String mapping = "provided_interaction_type_label,provided_interaction_type_id,mapped_to_interaction_type_label,mapped_to_interaction_type_id\n" +
                "drinking,http://purl.obolibrary.org/obo/OMIT_0005582,eats,http://purl.obolibrary.org/obo/RO_0002470\n" +
                "drinking,http://purl.obolibrary.org/obo/OMIT_0005582,eats,http://purl.obolibrary.org/obo/RO_0002470\n";

        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream(mapping, StandardCharsets.UTF_8));
        InteractTypeMapperFactory interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);

        try {
            interactTypeMapperFactory.create();
        } catch (TermLookupServiceException ex) {
            assertThat(ex, is(instanceOf(TermLookupServiceConfigurationException.class)));
            assertThat(ex.getMessage(), is("multiple mappings for [id]: [http://purl.obolibrary.org/obo/OMIT_0005582] were found, but only one unambiguous mapping is allowed"));
            throw ex;
        }

    }

    @Test(expected = TermLookupServiceException.class)
    public void throwOnDuplicateMappingCaseInsensitive() throws TermLookupServiceException, IOException {
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(null);

        String mapping = "provided_interaction_type_label,provided_interaction_type_id,mapped_to_interaction_type_label,mapped_to_interaction_type_id\n" +
                "Drinking,,eats,http://purl.obolibrary.org/obo/RO_0002470\n" +
                "drinking,,eats,http://purl.obolibrary.org/obo/RO_0002470\n";

        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream(mapping, StandardCharsets.UTF_8));
        InteractTypeMapperFactory interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);

        try {
            interactTypeMapperFactory.create();
        } catch (TermLookupServiceException ex) {
            assertThat(ex, is(instanceOf(TermLookupServiceConfigurationException.class)));
            assertThat(ex.getMessage(), is("multiple mappings for [name]: [drinking] were found, but only one unambiguous mapping is allowed"));
            throw ex;
        }

    }

    @Test(expected = TermLookupServiceException.class)
    public void throwOnMappingToUnsupportedInteractionType() throws TermLookupServiceException, IOException {
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(null);

        String mapping = "provided_interaction_type_label,provided_interaction_type_id,mapped_to_interaction_type_label,mapped_to_interaction_type_id\n" +
                "drinking,http://purl.obolibrary.org/obo/OMIT_0005582,eats,http://purl.obolibrary.org/obo/RO_000XXXX\n";

        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream(mapping, StandardCharsets.UTF_8));
        InteractTypeMapperFactory interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);

        try {
            interactTypeMapperFactory.create();
        } catch (TermLookupServiceException ex) {
            assertThat(ex, is(instanceOf(TermLookupServiceConfigurationException.class)));
            assertThat(ex.getMessage(), is("failed to map interaction type to [http://purl.obolibrary.org/obo/RO_000XXXX] on line [1]: interaction type unknown to GloBI"));
            throw ex;
        }

    }

    @Test
    public void nonMatchingBlankMapping() throws TermLookupServiceException, IOException {
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(IOUtils.toInputStream("interaction_type_ignored\nshouldBeIgnored", StandardCharsets.UTF_8));

        String mapping = "provided_interaction_type_label,provided_interaction_type_id,mapped_to_interaction_type_label,mapped_to_interaction_type_id" +
                "\n,,interactsWith, http://purl.obolibrary.org/obo/RO_0002437";

        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream(mapping, StandardCharsets.UTF_8));
        InteractTypeMapperFactory interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);

        assertThat(interactTypeMapperFactory
                        .create()
                        .getInteractType("foo"),
                is(nullValue()));

    }

    @Test
    public void createOverrideMappingEmptyMapping() throws TermLookupServiceException, IOException {
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(IOUtils.toInputStream("interaction_type_ignored\nshouldBeIgnored", StandardCharsets.UTF_8));

        String mapping = "provided_interaction_type_label,provided_interaction_type_id,mapped_to_interaction_type_label,mapped_to_interaction_type_id";
        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream(mapping, StandardCharsets.UTF_8));

        String defaultMapping = "provided_interaction_type_label,provided_interaction_type_id,mapped_to_interaction_type_label,mapped_to_interaction_type_id" +
                "\nassociated with,,interactsWith,http://purl.obolibrary.org/obo/RO_0002437\n";
        when(resourceService.retrieve(URI.create("classpath:/org/globalbioticinteractions/interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream(defaultMapping, StandardCharsets.UTF_8));

        InteractTypeMapperFactory interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);

        InteractTypeMapper interactTypeMapper = interactTypeMapperFactory.create();
        final InteractType interactType = interactTypeMapper
                .getInteractType("associated with");
        assertThat(interactType, is(InteractType.INTERACTS_WITH));

    }

    @Test
    public void createOverrideNonBlankMapping() throws TermLookupServiceException, IOException {
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(IOUtils.toInputStream("interaction_type_ignored\nshouldBeIgnored", StandardCharsets.UTF_8));

        String mapping = "provided_interaction_type_label,provided_interaction_type_id,mapped_to_interaction_type_label,mapped_to_interaction_type_id\n" +
                "associated with,,pollinates,http://purl.obolibrary.org/obo/RO_0002455";

        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream(mapping, StandardCharsets.UTF_8));

        String defaultMapping = "provided_interaction_type_label,provided_interaction_type_id,mapped_to_interaction_type_label,mapped_to_interaction_type_id" +
                "\nassociated with,,interactsWith,http://purl.obolibrary.org/obo/RO_0002437\n" +
                "\npollinates,,pollinates,http://purl.obolibrary.org/obo/RO_0002455\n";
        when(resourceService.retrieve(URI.create("classpath:/org/globalbioticinteractions/interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream(defaultMapping, StandardCharsets.UTF_8));

        InteractTypeMapperFactory interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);

        InteractTypeMapper mapper = interactTypeMapperFactory.create();
        assertThat(mapper.getInteractType("associated with"), is(InteractType.POLLINATES));
        assertThat(mapper.getInteractType("pollinates"), is(InteractType.POLLINATES));
    }

    @Test
    public void createOverrideNonBlankMappingROMapping() throws TermLookupServiceException, IOException {
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(IOUtils.toInputStream("interaction_type_ignored\nshouldBeIgnored", StandardCharsets.UTF_8));

        String mapping = "provided_interaction_type_label,provided_interaction_type_id,mapped_to_interaction_type_label,mapped_to_interaction_type_id\n" +
                "associated with,,pollinates,http://purl.obolibrary.org/obo/RO_0002455";

        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream(mapping, StandardCharsets.UTF_8));

        String defaultMapping = "provided_interaction_type_label,provided_interaction_type_id,mapped_to_interaction_type_label,mapped_to_interaction_type_id" +
                "\nassociated with,,interactsWith,http://purl.obolibrary.org/obo/RO_0002437\n";
        when(resourceService.retrieve(URI.create("classpath:/org/globalbioticinteractions/interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream(defaultMapping, StandardCharsets.UTF_8));

        String defaultROMapping = "interaction_type_label,interaction_type_id" +
                "\npollinates,http://purl.obolibrary.org/obo/RO_0002455\n";
        when(resourceService.retrieve(URI.create("classpath:/org/globalbioticinteractions/interaction_types_ro.csv")))
                .thenReturn(IOUtils.toInputStream(defaultROMapping, StandardCharsets.UTF_8));

        InteractTypeMapperFactory interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);

        InteractTypeMapper mapper = interactTypeMapperFactory.create();
        assertThat(mapper.getInteractType("associated with"), is(InteractType.POLLINATES));
        assertThat(mapper.getInteractType("pollinates"), is(InteractType.POLLINATES));
    }

    @Test
    public void createAndNoIgnoreResource() throws TermLookupServiceException, IOException {

        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenThrow(new IOException("kaboom!"));

        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenThrow(new IOException("kaboom!"));

        InteractTypeMapperFactoryImpl interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);
        final InteractTypeMapper interactTypeMapper = interactTypeMapperFactory.create();
        assertNotNull(interactTypeMapper);
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

    @Test
    public void createAndMapTermNoIgnore() throws TermLookupServiceException, IOException {
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(null)
                .thenReturn(null);
        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream(getTestMap(), StandardCharsets.UTF_8));

        InteractTypeMapperFactoryImpl interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);
        InteractTypeMapper interactTypeMapper = interactTypeMapperFactory.create();
        assertThat(interactTypeMapper.getInteractType("shouldBeMapped"), is(InteractType.INTERACTS_WITH));
    }

    @Test
    public void createAndMapTermThrowOnMissingIgnore() throws TermLookupServiceException, IOException {
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenThrow(new IOException("kaboom!"));
        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream(getTestMap(), StandardCharsets.UTF_8));

        InteractTypeMapperFactoryImpl interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);
        InteractTypeMapper interactTypeMapper = interactTypeMapperFactory.create();
        assertThat(interactTypeMapper.getInteractType("shouldBeMapped"), is(InteractType.INTERACTS_WITH));
    }

    @Test
    public void createAndIgnoreTermNoMap() throws TermLookupServiceException, IOException {
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(IOUtils.toInputStream("interaction_type_ignored\neats", StandardCharsets.UTF_8))
                .thenReturn(IOUtils.toInputStream("interaction_type_ignored\neats", StandardCharsets.UTF_8));
        when(resourceService.retrieve(URI.create("interaction_types_mappings.csv")))
                .thenReturn(null);

        InteractTypeMapperFactoryImpl interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);
        InteractTypeMapper interactTypeMapper = interactTypeMapperFactory.create();
        assertThat(interactTypeMapper.getInteractType("eats"), is(nullValue()));
    }

    @Test
    public void createAndIgnoreTermWithThrowingOnMapRetrieve() throws TermLookupServiceException, IOException {
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(IOUtils.toInputStream("interaction_type_ignored\neats", StandardCharsets.UTF_8))
                .thenReturn(IOUtils.toInputStream("interaction_type_ignored\neats", StandardCharsets.UTF_8));
        when(resourceService.retrieve(URI.create("interaction_types_mappings.csv")))
                .thenThrow(new IOException("kaboom!"));

        InteractTypeMapperFactoryImpl interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);
        InteractTypeMapper interactTypeMapper = interactTypeMapperFactory.create();
        assertThat(interactTypeMapper.getInteractType("eats"), is(nullValue()));
    }


    @Test()
    public void createInvalidTypeMape() throws TermLookupServiceException, IOException {
        // reproduce https://github.com/qgroom/Vespa-velutina/issues/3
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream(
                        "provided_interaction_type_label,provided_interaction_type_id,mapped_to_interaction_type_label,mapped_to_interaction_type_id\n" +
                                "stings,,participates in a biotic-biotic interaction with,http://purl.obolibrary.org/obo/RO_0002574\n" +
                                "reproductively interferes with,,participates in a biotic-biotic interaction with,http://purl.obolibrary.org/obo/RO_0002574\n",
                        StandardCharsets.UTF_8));

        InteractTypeMapperFactoryImpl interactTypeMapperFactory = new InteractTypeMapperFactoryImpl(resourceService);
        InteractTypeMapper interactTypeMapper = interactTypeMapperFactory.create();
        assertThat(interactTypeMapper.getInteractType("stings"), is(InteractType.INTERACTS_WITH));
    }

}