package org.eol.globi.util;

import org.apache.commons.io.IOUtils;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Term;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class InteractUtilTest {

    @Test
    public void interactionCypherClause() {
        String value = "PREYS_UPON|PARASITE_OF|HAS_HOST|HOST_OF|POLLINATES|ATE|SYMBIONT_OF|POLLINATED_BY|HAS_PARASITE|HAS_PATHOGEN|PATHOGEN_OF|HAS_VECTOR|VECTOR_OF|ENDOPARASITE_OF|HAS_ENDOPARASITE|HYPERPARASITE_OF|HAS_HYPERPARASITE|HYPERPARASITOID_OF|HAS_HYPERPARASITOID|ECTOPARASITE_OF|HAS_ECTOPARASITE|KLEPTOPARASITE_OF|HAS_KLEPTOPARASITE|PARASITOID_OF|HAS_PARASITOID|ENDOPARASITOID_OF|HAS_ENDOPARASITOID|ECTOPARASITOID_OF|HAS_ECTOPARASITOID|FARMED_BY|FARMS|DISPERSAL_VECTOR_OF|HAS_DISPERAL_VECTOR|EPIPHITE_OF|HAS_EPIPHITE|COMMENSALIST_OF|MUTUALIST_OF";
        assertThat(InteractUtil.interactionsCypherClause(InteractType.ATE, InteractType.SYMBIONT_OF)
                , is(value));
    }

    @Test
    public void interactionCypherClause2() {
        assertThat(InteractUtil.interactionsCypherClause(InteractType.VECTOR_OF)
                , is("VECTOR_OF|DISPERSAL_VECTOR_OF"));
    }

    @Test
    public void mapNotCaseSensitive() {
        assertThat(InteractUtil.getInteractTypeForName("hostOf"), is(InteractType.HOST_OF));
        assertThat(InteractUtil.getInteractTypeForName("hostof"), is(InteractType.HOST_OF));
    }

    @Test(expected = StudyImporterException.class)
    public void createInteractionTermMapper() throws StudyImporterException, TermLookupServiceException {
        TermLookupService termLookupService = InteractUtil.getTermLookupService(new ResourceService<URI>() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return IOUtils.toInputStream(
                        "observation_field_id,providedName,interaction_type_id,resolvedName\n" +
                        "someProvidedId,someProvidedName,someResolvedId,someResolvedName", StandardCharsets.UTF_8);
            }

            @Override
            public URI getLocalURI(URI resourceName) throws IOException {
                fail("not [" + resourceName + "]");
                return null;
            }
        });

        List<Term> someProvidedId = termLookupService.lookupTermByName("someProvidedId");
        assertThat(someProvidedId.size(), is(1));
        assertThat(someProvidedId.get(0).getId(), is("someResolvedId"));
        assertThat(someProvidedId.get(0).getName(), is("someResolvedName"));
    }

    @Test
    public void createInteractionTermMapperValidTerm() throws StudyImporterException, TermLookupServiceException {
        TermLookupService termLookupService = InteractUtil.getTermLookupService(new ResourceService<URI>() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                if (resourceName.equals(InteractUtil.TYPE_MAP_URI_DEFAULT)) {
                    return IOUtils.toInputStream(
                            "observation_field_id,providedName,interaction_type_id,resolvedName\n" +
                                    "someProvidedId,someProvidedName,http://purl.obolibrary.org/obo/RO_0002440,someResolvedName", StandardCharsets.UTF_8);
                } else {
                    return IOUtils.toInputStream(
                            "observation_field_id\n" +
                                    "someIgnoredId", StandardCharsets.UTF_8);

                }
            }

            @Override
            public URI getLocalURI(URI resourceName) throws IOException {
                fail("not [" + resourceName + "]");
                return null;
            }
        });

        List<Term> someProvidedId = termLookupService.lookupTermByName("someProvidedId");
        assertThat(someProvidedId.size(), is(1));
        assertThat(someProvidedId.get(0).getId(), is("http://purl.obolibrary.org/obo/RO_0002440"));
        assertThat(someProvidedId.get(0).getName(), is("symbiontOf"));
    }

}