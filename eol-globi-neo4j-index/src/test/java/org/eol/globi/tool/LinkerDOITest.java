package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.db.GraphServiceFactoryProxy;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.service.DOIResolver;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.ExternalIdUtil;
import org.globalbioticinteractions.dataset.DatasetConstant;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.doi.DOI;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class LinkerDOITest extends GraphDBTestCase {

    @Test
    public void doLink() throws NodeFactoryException, PropertyEnricherException {
        StudyNode study = getNodeFactory().getOrCreateStudy(new StudyImpl("title", null, "some citation"));
        new LinkerDOI(new GraphServiceFactoryProxy(getGraphDb())).index();
        Study studyResolved = nodeFactory.getOrCreateStudy(study);
        assertThat(studyResolved.getDOI(), is(nullValue()));
        assertThat(study.getDOI(), is(nullValue()));
    }

    @Test
    public void shouldResolveStudy() {
        StudyImpl study = new StudyImpl("some title", new DOI("some", "doi"), "some citation");
        assertFalse(LinkerDOI.shouldResolve(study));
    }

    @Test
    public void shouldResolveStudyEmptyCitation() {
        StudyImpl study = new StudyImpl("some title", null, "");
        assertFalse(LinkerDOI.shouldResolve(study));
    }

    @Test
    public void shouldResolveStudyHttps() {
        StudyImpl study = new StudyImpl("some title", null, "http://example.com");
        assertFalse(LinkerDOI.shouldResolve(study));
    }

    @Test
    public void doLinkTwo() throws NodeFactoryException, PropertyEnricherException {
        assertLinkMany(0);
    }

    @Test
    public void doLinkMany() throws NodeFactoryException, PropertyEnricherException {
        assertLinkMany(LinkerDOI.BATCH_SIZE + 2);
    }

    private void assertLinkMany(long numberOfStudies) throws NodeFactoryException {
        StudyNode study = getNodeFactory().getOrCreateStudy(new StudyImpl("title", null, "HOCKING"));
        getNodeFactory().getOrCreateStudy(new StudyImpl("title1", null, "MEDAN"));
        assertThat(study.getDOI(), is(nullValue()));
        for (int i = 0; i< numberOfStudies; i++) {
            getNodeFactory().getOrCreateStudy(new StudyImpl("id" + i, null, "foo bar this is not a citation" + i));

        }
        new LinkerDOI(new GraphServiceFactoryProxy(getGraphDb()), new DOIResolver() {
            @Override
            public Map<String, DOI> resolveDoiFor(Collection<String> references) throws IOException {
                Map<String, DOI> resolved = new HashMap<>();
                for (String reference : references) {
                    resolved.put(reference, resolveDoiFor(reference));
                }
                return resolved;
            }

            @Override
            public DOI resolveDoiFor(String reference) throws IOException {
                return new DOI("123", "456");
            }
        }).index();
        StudyNode studyResolved = getNodeFactory().getOrCreateStudy(study);
        assertThat(studyResolved.getDOI(), is(new DOI("123", "456")));
    }

    @Test
    public void createStudyDOIlookup() throws NodeFactoryException {
        StudyNode study = getNodeFactory().getOrCreateStudy(new StudyImpl("title", null, "some citation"));
        LinkerDOI.linkStudy(new DOIResolverThatExplodes(), study);
        assertThat(study.getCitation(), is("some citation"));
        assertThat(study.getTitle(), is("title"));
    }

    @Test
    public void createStudyDOIlookupCitationWithURL() throws NodeFactoryException {
        StudyNode study = getNodeFactory().getOrCreateStudy(new StudyImpl("title", null, "http://bla"));
        LinkerDOI.linkStudy(new DOIResolverThatFails(), study);
        assertThat(study.getTitle(), is("title"));
    }

    @Test
    public void createStudyDOIlookupCitationEnabled() throws NodeFactoryException {
        StudyImpl title = new StudyImpl("title", null, "some citation");
        DatasetImpl originatingDataset = new DatasetImpl("some/namespace", URI.create("some:uri"), inStream -> inStream);
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put(DatasetConstant.SHOULD_RESOLVE_REFERENCES, true);
        originatingDataset.setConfig(objectNode);
        title.setOriginatingDataset(originatingDataset);
        StudyNode study = getNodeFactory().getOrCreateStudy(title);

        LinkerDOI.linkStudy(new TestDOIResolver(), study);
        assertThat(study.getDOI().toString(), is("10.some/some citation"));
        assertThat(study.getExternalId(), is("https://doi.org/10.some/some%20citation"));
        assertThat(study.getCitation(), is("some citation"));
        assertThat(study.getTitle(), is("title"));
    }

    @Test
    public void createStudyDOIlookupCitationDisabled() throws NodeFactoryException {
        StudyImpl study1 = new StudyImpl("title", null, "some citation");
        study1.setExternalId("some:id");
        DatasetImpl originatingDataset = new DatasetImpl("some/namespace", URI.create("some:uri"), inStream -> inStream);
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put(DatasetConstant.SHOULD_RESOLVE_REFERENCES, false);
        originatingDataset.setConfig(objectNode);
        study1.setOriginatingDataset(originatingDataset);
        Study study = getNodeFactory().getOrCreateStudy(study1);
        assertThat(study.getDOI(), is(nullValue()));
        assertThat(study.getCitation(), is("some citation"));
        assertThat(study.getTitle(), is("title"));
        assertThat(study.getExternalId(), is("some:id"));
    }

    @Test
    public void addDOIToStudy() throws NodeFactoryException {
        DOIResolver doiResolver = new DOIResolver() {
            @Override
            public Map<String, DOI> resolveDoiFor(Collection<String> references) throws IOException {
                Map<String, DOI> doiMap = new HashMap<>();
                for (String reference : references) {
                    doiMap.put(reference, resolveDoiFor(reference));
                }
                return doiMap;
            }

            @Override
            public DOI resolveDoiFor(String reference) throws IOException {
                return new DOI("1234", "567");
            }
        };
        StudyNode study = getNodeFactory().getOrCreateStudy(new StudyImpl("my title", null, ExternalIdUtil.toCitation("my contr", "some description", null)));
        LinkerDOI.linkStudy(doiResolver, study);
        assertThat(study.getDOI().toString(), is("10.1234/567"));
        assertThat(study.getExternalId(), is("https://doi.org/10.1234/567"));
        assertThat(study.getCitation(), is("my contr. some description"));

        StudyImpl study1 = new StudyImpl("my other title", null, ExternalIdUtil.toCitation("my contr", "some description", null));
        assertThat(study1.getExternalId(), nullValue());
        study = getNodeFactory().getOrCreateStudy(study1);
        assertThat(study.getExternalId(), nullValue());
        LinkerDOI.linkStudy(new DOIResolverThatExplodes(), study);
        assertThat(study.getDOI(), nullValue());
        assertThat(study.getExternalId(), nullValue());
        assertThat(study.getCitation(), is("my contr. some description"));
    }

    private static class DOIResolverThatExplodes implements DOIResolver {
        @Override
        public Map<String, DOI> resolveDoiFor(Collection<String> references) throws IOException {
            throw new IOException("kaboom!");
        }

        @Override
        public DOI resolveDoiFor(String reference) throws IOException {
            throw new IOException("kaboom!");
        }
    }

    private static class DOIResolverThatFails implements DOIResolver {
        @Override
        public Map<String, DOI> resolveDoiFor(Collection<String> references) throws IOException {
            fail("should not call this");
            return new HashMap<>();
        }

        @Override
        public DOI resolveDoiFor(String reference) throws IOException {
            fail("should not call this");
            return new DOI("some", "doi");
        }

    }


    public static class TestDOIResolver implements DOIResolver {
        @Override
        public Map<String, DOI> resolveDoiFor(Collection<String> references) throws IOException {
            Map<String, DOI> doiMap = new HashMap<>();
            for (String reference : references) {
                doiMap.put(reference, resolveDoiFor(reference));
            }
            return doiMap;
        }

        @Override
        public DOI resolveDoiFor(String reference) throws IOException {
            return StringUtils.isBlank(reference) ? null : new DOI("some", reference);
        }

    }
}