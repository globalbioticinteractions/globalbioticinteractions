package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.service.DOIResolver;
import org.eol.globi.service.DOIResolverImplIT;
import org.eol.globi.service.DatasetConstant;
import org.eol.globi.service.DatasetImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.ExternalIdUtil;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class LinkerDOITest extends GraphDBTestCase {

    @Test
    public void doLink() throws NodeFactoryException, PropertyEnricherException {
        StudyNode study = getNodeFactory().getOrCreateStudy(new StudyImpl("title", "some source", null, "some citation"));
        new LinkerDOI(getGraphDb()).link();
        Study studyResolved = nodeFactory.getOrCreateStudy(study);
        assertThat(studyResolved.getDOI(), is(nullValue()));
        assertThat(study.getDOI(), is(nullValue()));
    }

    @Test
    public void shouldResolveStudy() {
        StudyImpl study = new StudyImpl("some title", "some source", "doi:some/doi", "some citation");
        assertFalse(LinkerDOI.shouldResolve(study));
    }

    @Test
    public void shouldResolveStudyEmptyCitation() {
        StudyImpl study = new StudyImpl("some title", "some source", "", "");
        assertFalse(LinkerDOI.shouldResolve(study));
    }

    @Test
    public void shouldResolveStudyHttps() {
        StudyImpl study = new StudyImpl("some title", "some source", "", "http://example.com");
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

    @Test
    public void doLinkMany2() throws NodeFactoryException, PropertyEnricherException {
        assertLinkMany(LinkerDOI.BATCH_SIZE*4 + 2);
    }

    public void assertLinkMany(int numberOfStudies) throws NodeFactoryException {
        StudyNode study = getNodeFactory().getOrCreateStudy(new StudyImpl("title", "some source", null, DOIResolverImplIT.HOCKING));
        getNodeFactory().getOrCreateStudy(new StudyImpl("title1", "some source", null, DOIResolverImplIT.MEDAN));
        assertThat(study.getDOI(), is(nullValue()));
        for (int i = 0; i< numberOfStudies; i++) {
            getNodeFactory().getOrCreateStudy(new StudyImpl("id" + i, "some source", null, "foo bar this is not a citation" + i));

        }
        new LinkerDOI(getGraphDb()).link();
        StudyNode studyResolved = getNodeFactory().getOrCreateStudy(study);
        assertThat(studyResolved.getDOI(), is(DOIResolverImplIT.HOCKING_DOI));
    }

    @Test
    public void createStudyDOIlookup() throws NodeFactoryException {
        StudyNode study = getNodeFactory().getOrCreateStudy(new StudyImpl("title", "some source", null, "some citation"));
        new LinkerDOI(getGraphDb()).linkStudy(new DOIResolverThatExplodes(), study);
        assertThat(study.getSource(), is("some source"));
        assertThat(study.getCitation(), is("some citation"));
        assertThat(study.getTitle(), is("title"));
    }

    @Test
    public void createStudyDOIlookupCitationWithURL() throws NodeFactoryException {
        StudyNode study = getNodeFactory().getOrCreateStudy(new StudyImpl("title", "some source", null, "http://bla"));
        new LinkerDOI(getGraphDb()).linkStudy(new DOIResolverThatFails(), study);
        assertThat(study.getSource(), is("some source"));
        assertThat(study.getCitation(), is("http://bla"));
        assertThat(study.getTitle(), is("title"));
    }

    @Test
    public void createStudyDOIlookupCitationEnabled() throws NodeFactoryException {
        StudyImpl title = new StudyImpl("title", "some source", null, "some citation");
        DatasetImpl originatingDataset = new DatasetImpl("some/namespace", URI.create("some:uri"));
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put(DatasetConstant.SHOULD_RESOLVE_REFERENCES, true);
        originatingDataset.setConfig(objectNode);
        title.setOriginatingDataset(originatingDataset);
        StudyNode study = getNodeFactory().getOrCreateStudy(title);

        new LinkerDOI(getGraphDb()).linkStudy(new TestDOIResolver(), study);
        assertThat(study.getSource(), is("some source"));
        assertThat(study.getDOI(), is("doi:some citation"));
        assertThat(study.getCitation(), is("some citation"));
        assertThat(study.getTitle(), is("title"));
    }

    @Test
    public void createStudyDOIlookupCitationDisabled() throws NodeFactoryException {
        StudyImpl study1 = new StudyImpl("title", "some source", null, "some citation");
        study1.setExternalId("some:id");
        DatasetImpl originatingDataset = new DatasetImpl("some/namespace", URI.create("some:uri"));
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put(DatasetConstant.SHOULD_RESOLVE_REFERENCES, false);
        originatingDataset.setConfig(objectNode);
        study1.setOriginatingDataset(originatingDataset);
        Study study = getNodeFactory().getOrCreateStudy(study1);
        assertThat(study.getSource(), is("some source"));
        assertThat(study.getDOI(), is(nullValue()));
        assertThat(study.getCitation(), is("some citation"));
        assertThat(study.getTitle(), is("title"));
        assertThat(study.getExternalId(), is("some:id"));
    }

    @Test
    public void addDOIToStudy() throws NodeFactoryException {
        DOIResolver doiResolver = new DOIResolver() {
            @Override
            public Map<String, String> resolveDoiFor(Collection<String> references) throws IOException {
                Map<String, String> doiMap = new HashMap<>();
                for (String reference : references) {
                    doiMap.put(reference, resolveDoiFor(reference));
                }
                return doiMap;
            }

            @Override
            public String resolveDoiFor(String reference) throws IOException {
                return "doi:1234";
            }
        };
        StudyNode study = getNodeFactory().getOrCreateStudy(new StudyImpl("my title", "some source", null, ExternalIdUtil.toCitation("my contr", "some description", null)));
        new LinkerDOI(getGraphDb()).linkStudy(doiResolver, study);
        assertThat(study.getDOI(), is("doi:1234"));
        assertThat(study.getExternalId(), is("https://doi.org/1234"));
        assertThat(study.getCitation(), is("my contr. some description"));

        study = getNodeFactory().getOrCreateStudy(new StudyImpl("my other title", "some source", null, ExternalIdUtil.toCitation("my contr", "some description", null)));
        new LinkerDOI(getGraphDb()).linkStudy(new DOIResolverThatExplodes(), study);
        assertThat(study.getDOI(), nullValue());
        assertThat(study.getExternalId(), nullValue());
        assertThat(study.getCitation(), is("my contr. some description"));
    }

    private static class DOIResolverThatExplodes implements DOIResolver {
        @Override
        public Map<String, String> resolveDoiFor(Collection<String> references) throws IOException {
            throw new IOException("kaboom!");
        }

        @Override
        public String resolveDoiFor(String reference) throws IOException {
            throw new IOException("kaboom!");
        }
    }

    private static class DOIResolverThatFails implements DOIResolver {
        @Override
        public Map<String, String> resolveDoiFor(Collection<String> references) throws IOException {
            fail("should not call this");
            return new HashMap<>();
        }

        @Override
        public String resolveDoiFor(String reference) throws IOException {
            fail("should not call this");
            return "bla";
        }

    }


    public static class TestDOIResolver implements DOIResolver {
        @Override
        public Map<String, String> resolveDoiFor(Collection<String> references) throws IOException {
            Map<String, String> doiMap = new HashMap<>();
            for (String reference : references) {
                doiMap.put(reference, resolveDoiFor(reference));
            }
            return doiMap;
        }

        @Override
        public String resolveDoiFor(String reference) throws IOException {
            return StringUtils.isBlank(reference) ? null : "doi:" + reference;
        }

    }
}