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
import org.eol.globi.service.DatasetImpl;
import org.eol.globi.service.DatasetUtil;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.ExternalIdUtil;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;

import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class LinkerDOITest extends GraphDBTestCase {

    @Test
    public void doLink() throws NodeFactoryException, PropertyEnricherException {
        StudyNode study = getNodeFactory().getOrCreateStudy(new StudyImpl("title", "some source", null, "some citation"));
        new LinkerDOI().link(getGraphDb());
        assertThat(study.getSource(), is("some source"));
        assertThat(study.getCitation(), is("some citation"));
        assertThat(study.getTitle(), is("title"));
    }

    @Test
    public void createStudyDOIlookup() throws NodeFactoryException {
        StudyNode study = getNodeFactory().getOrCreateStudy(new StudyImpl("title", "some source", null, "some citation"));
        new LinkerDOI().linkStudy(new DOIResolverThatExplodes(), study);
        assertThat(study.getSource(), is("some source"));
        assertThat(study.getCitation(), is("some citation"));
        assertThat(study.getTitle(), is("title"));
    }

    @Test
    public void createStudyDOIlookupCitationWithURL() throws NodeFactoryException {
        StudyNode study = getNodeFactory().getOrCreateStudy(new StudyImpl("title", "some source", null, "http://bla"));
        new LinkerDOI().linkStudy(new DOIResolverThatFails(), study);
        assertThat(study.getSource(), is("some source"));
        assertThat(study.getCitation(), is("http://bla"));
        assertThat(study.getTitle(), is("title"));
    }

    @Test
    public void createStudyDOIlookupCitationEnabled() throws NodeFactoryException {
        StudyImpl title = new StudyImpl("title", "some source", null, "some citation");
        DatasetImpl originatingDataset = new DatasetImpl("some/namespace", URI.create("some:uri"));
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put(DatasetUtil.SHOULD_RESOLVE_REFERENCES, true);
        originatingDataset.setConfig(objectNode);
        title.setOriginatingDataset(originatingDataset);
        StudyNode study = getNodeFactory().getOrCreateStudy(title);

        new LinkerDOI().linkStudy(new TestDOIResolver(), study);
        assertThat(study.getSource(), is("some source"));
        assertThat(study.getDOI(), is("doi:some citation"));
        assertThat(study.getCitation(), is("citation:doi:some citation"));
        assertThat(study.getTitle(), is("title"));
    }

    @Test
    public void createStudyDOIlookupCitationDisabled() throws NodeFactoryException {
        StudyImpl study1 = new StudyImpl("title", "some source", null, "some citation");
        DatasetImpl originatingDataset = new DatasetImpl("some/namespace", URI.create("some:uri"));
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put(DatasetUtil.SHOULD_RESOLVE_REFERENCES, false);
        originatingDataset.setConfig(objectNode);
        study1.setOriginatingDataset(originatingDataset);
        Study study = getNodeFactory().getOrCreateStudy(study1);
        assertThat(study.getSource(), is("some source"));
        assertThat(study.getDOI(), is(nullValue()));
        assertThat(study.getCitation(), is("some citation"));
        assertThat(study.getTitle(), is("title"));
    }

    @Test
    public void addDOIToStudy() throws NodeFactoryException {
        DOIResolver doiResolver = new DOIResolver() {
            @Override
            public String findDOIForReference(String reference) throws IOException {
                return "doi:1234";
            }

            @Override
            public String findCitationForDOI(String doi) throws IOException {
                return "my citation";
            }
        };
        StudyNode study = getNodeFactory().getOrCreateStudy(new StudyImpl("my title", "some source", null, ExternalIdUtil.toCitation("my contr", "some description", null)));
        new LinkerDOI().linkStudy(doiResolver, study);
        assertThat(study.getDOI(), is("doi:1234"));
        assertThat(study.getExternalId(), is("http://dx.doi.org/1234"));
        assertThat(study.getCitation(), is("my citation"));

        study = getNodeFactory().getOrCreateStudy(new StudyImpl("my other title", "some source", null, ExternalIdUtil.toCitation("my contr", "some description", null)));
        new LinkerDOI().linkStudy(new DOIResolverThatExplodes(), study);
        assertThat(study.getDOI(), nullValue());
        assertThat(study.getExternalId(), nullValue());
        assertThat(study.getCitation(), is("my contr. some description"));
    }


    private static class DOIResolverThatExplodes implements DOIResolver {
        @Override
        public String findDOIForReference(String reference) throws IOException {
            throw new IOException("kaboom!");
        }

        @Override
        public String findCitationForDOI(String doi) throws IOException {
            throw new IOException("kaboom!");
        }
    }

    private static class DOIResolverThatFails implements DOIResolver {
        @Override
        public String findDOIForReference(String reference) throws IOException {
            fail("should not call this");
            return "bla";
        }

        @Override
        public String findCitationForDOI(String doi) throws IOException {
            fail("should not call this");
            return "bla";
        }
    }


    public static class TestDOIResolver implements DOIResolver {
        @Override
        public String findDOIForReference(String reference) throws IOException {
            return StringUtils.isBlank(reference) ? null : "doi:" + reference;
        }

        @Override
        public String findCitationForDOI(String doi) throws IOException {
            return StringUtils.isBlank(doi) ? null : "citation:" + doi;
        }
    }
}