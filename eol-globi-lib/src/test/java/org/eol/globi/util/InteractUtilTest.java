package org.eol.globi.util;

import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.io.IOUtils;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Term;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.MatcherAssert.assertThat;

public class InteractUtilTest {

    private static final Set<String> UNLIKELY_INTERACTION_TYPE_NAMES
            = UnmodifiableSet.unmodifiableSet(new TreeSet<String>() {{
        add("(collected with)");
        add("collector number");
        add("(littermate or nestmate of)");
        add("(mate of)");
        add("mixed species flock");
        add("mosses");
        add("(offspring of)");
        add("(parent of)");
        add("(same individual as)");
        add("same litter");
        add("(same lot as)");
        add("(sibling of)");
    }});

    private static final Map<String, InteractType> INTERACTION_TYPE_NAME_MAP =
            UnmodifiableMap.unmodifiableMap(new HashMap<String, InteractType>() {{
                put("associated with", InteractType.INTERACTS_WITH);
                put("ex", InteractType.HAS_HOST);
                put("ex.", InteractType.HAS_HOST);
                put("reared ex", InteractType.HAS_HOST);
                put("reared ex.", InteractType.HAS_HOST);
                put("host to", InteractType.HOST_OF);
                put("host", InteractType.HAS_HOST);
                put("h", InteractType.HAS_HOST);
                put("larval foodplant", InteractType.ATE);
                put("ectoparasite of", InteractType.ECTOPARASITE_OF);
                put("parasite of", InteractType.PARASITE_OF);
                put("stomach contents of", InteractType.EATEN_BY);
                put("stomach contents", InteractType.ATE);
                put("eaten by", InteractType.EATEN_BY);
                put("(ate)", InteractType.ATE);
                put("(eaten by)", InteractType.EATEN_BY);
                put("(parasite of)", InteractType.PARASITE_OF);
                put("(host of)", InteractType.HAS_PARASITE); //see https://arctos.database.museum/info/ctDocumentation.cfm?table=ctid_references#host_of
                put("(in amplexus with)", InteractType.INTERACTS_WITH);
                put("consumption", InteractType.ATE);
                put("flower predator", InteractType.ATE);
                put("flower visitor", InteractType.VISITS_FLOWERS_OF);
                put("folivory", InteractType.ATE);
                put("fruit thief", InteractType.ATE);
                put("ingestion", InteractType.ATE);
                put("pollinator", InteractType.POLLINATES);
                put("seed disperser", InteractType.VECTOR_OF);
                put("seed predator", InteractType.ATE);
                put("vector of", InteractType.VECTOR_OF);
                put("found on", InteractType.ADJACENT_TO);
                put("visitsflowersof", InteractType.VISITS_FLOWERS_OF);
                put("collected on", InteractType.ADJACENT_TO);
                put("reared from", InteractType.INTERACTS_WITH);
                put("emerged from", InteractType.INTERACTS_WITH);
                put("collected in", InteractType.INTERACTS_WITH);
                put("hyperparasitoid of", InteractType.HYPERPARASITE_OF);
                put("on", InteractType.ADJACENT_TO);
                put("under", InteractType.INTERACTS_WITH);
                put("inside", InteractType.INTERACTS_WITH);
                put("in", InteractType.INTERACTS_WITH);
            }});

    @Test
    public void interactionCypherClause() {
        String value = "PREYS_UPON|PARASITE_OF|HAS_HOST|HOST_OF|POLLINATES|ATE|SYMBIONT_OF|POLLINATED_BY|HAS_PARASITE|HAS_VECTOR|VECTOR_OF|ENDOPARASITE_OF|HAS_ENDOPARASITE|HYPERPARASITE_OF|HAS_HYPERPARASITE|ECTOPARASITE_OF|HAS_ECTOPARASITE|KLEPTOPARASITE_OF|HAS_KLEPTOPARASITE|PARASITOID_OF|HAS_PARASITOID|ENDOPARASITOID_OF|HAS_ENDOPARASITOID|ECTOPARASITOID_OF|HAS_ECTOPARASITOID|FARMED_BY|FARMS|DISPERSAL_VECTOR_OF|HAS_DISPERAL_VECTOR|EPIPHITE_OF|HAS_EPIPHITE|COMMENSALIST_OF|MUTUALIST_OF|HEMIPARASITE_OF|ROOTPARASITE_OF";
        assertThat(InteractUtil.interactionsCypherClause(InteractType.ATE, InteractType.SYMBIONT_OF)
                , is(value));
    }

    @Test
    public void interactionCypherClause2() {
        assertThat(InteractUtil.interactionsCypherClause(InteractType.VECTOR_OF)
                , is("VECTOR_OF|DISPERSAL_VECTOR_OF"));
    }

    @Test
    public void mapNotCaseSensitive() throws TermLookupServiceException {
        assertThat(new InteractTypeMapperFactoryImpl().create().getInteractType("hostOf"),
                is(InteractType.HOST_OF));
        assertThat(new InteractTypeMapperFactoryImpl().create().getInteractType("hostof"),
                is(InteractType.HOST_OF));
    }

    @Test
    public void mapKnownNonROTerms() throws TermLookupServiceException {
        Set<Map.Entry<String, InteractType>> entries = INTERACTION_TYPE_NAME_MAP.entrySet();
        assertThat(entries.size(), greaterThan(0));
        for (Map.Entry<String, InteractType> entry : entries) {
            InteractType interactTypeForName = new InteractTypeMapperFactoryImpl().create().getInteractType(entry.getKey());
            assertThat("failed to map [" + entry.getKey() + "]", interactTypeForName, is(entry.getValue()));
        }
    }

    @Test
    public void mapUnknownNonROTerms() throws TermLookupServiceException {
        InteractType interactTypeForName = new InteractTypeMapperFactoryImpl().create().getInteractType("donaldduck");
        assertThat(interactTypeForName, is(nullValue()));
    }

    @Test
    public void ensureIgnoredTermMappings() throws TermLookupServiceException {
        Set<String> unlikelyInteractionTypeNames = UNLIKELY_INTERACTION_TYPE_NAMES;
        assertThat(unlikelyInteractionTypeNames.size(), greaterThan(0));
        for (String unlikelyInteractionType : unlikelyInteractionTypeNames) {
            boolean condition = new InteractTypeMapperFactoryImpl().create()
                    .shouldIgnoreInteractionType(unlikelyInteractionType);
            Assert.assertTrue("failed to ignore [" + unlikelyInteractionType + "]", condition);
        }
    }

    @Test(expected = TermLookupServiceException.class)
    public void createInteractionTermMapperUnknownResolvedId() throws StudyImporterException, TermLookupServiceException {

        TermLookupService ignoredTermService = new TermLookupService() {
            @Override
            public List<Term> lookupTermByName(String name) throws TermLookupServiceException {
                return null;
            }
        };
        ResourceService resourceService = new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return IOUtils.toInputStream(
                        "observation_field_id,providedName,interaction_type_id,resolvedName\n" +
                                "someProvidedId,someProvidedName,someResolvedId,someResolvedName", StandardCharsets.UTF_8);
            }

        };
        TermLookupService termLookupService = InteractTypeMapperFactoryImpl.getTermLookupService(
                ignoredTermService,
                resourceService,
                "observation_field_id",
                "observation_field_name",
                "interaction_type_id",
                InteractTypeMapperFactoryImpl.TYPE_MAP_URI_DEFAULT);

        List<Term> someProvidedId = termLookupService.lookupTermByName("someProvidedId");
        assertThat(someProvidedId.size(), is(1));
        assertThat(someProvidedId.get(0).getId(), is("someResolvedId"));
        assertThat(someProvidedId.get(0).getName(), is("someResolvedName"));
    }

    @Test
    public void createInteractionTermMapperValidTerm() throws StudyImporterException, TermLookupServiceException {
        ResourceService testResourceService = new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return IOUtils.toInputStream(
                        "provided_interaction_type_id,provided_interaction_type_label,mapped_to_interaction_type_id,mapped_to_interaction_type_label\n" +
                                "someProvidedId,someProvidedName,http://purl.obolibrary.org/obo/RO_0002440,someResolvedName", StandardCharsets.UTF_8);
            }

        };

        ResourceService ignoredResourceService = new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return IOUtils.toInputStream(
                        "interaction_type_ignored\n" +
                                "someIgnoredId", StandardCharsets.UTF_8);
            }

        };

        TermLookupService ignoredTermService = InteractTypeMapperFactoryImpl.getIgnoredTermService(ignoredResourceService,
                "interaction_type_ignored",
                InteractTypeMapperFactoryImpl.TYPE_IGNORED_URI_DEFAULT);

        TermLookupService termLookupService = InteractTypeMapperFactoryImpl.getTermLookupService(
                ignoredTermService,
                testResourceService,
                "provided_interaction_type_id",
                "provided_interaction_type_label",
                "mapped_to_interaction_type_id",
                InteractTypeMapperFactoryImpl.TYPE_MAP_URI_DEFAULT);

        List<Term> someProvidedId = termLookupService.lookupTermByName("someProvidedId");
        assertThat(someProvidedId.size(), is(1));
        assertThat(someProvidedId.get(0).getId(), is("http://purl.obolibrary.org/obo/RO_0002440"));
        assertThat(someProvidedId.get(0).getName(), is("symbiontOf"));
    }

}