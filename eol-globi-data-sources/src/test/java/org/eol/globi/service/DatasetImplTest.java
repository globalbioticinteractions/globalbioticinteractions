package org.eol.globi.service;

import org.codehaus.jackson.map.ObjectMapper;
import org.globalbioticinteractions.doi.DOI;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class DatasetImplTest {

    @Test
    public void lookupMappedResourceRelative() throws IOException {
        Dataset dataset = new DatasetImpl("some/namespace", URI.create("some:uri"));
        dataset.setConfig(new ObjectMapper().readTree("{\"resources\": { \"previous/path.txt\": \"current/path.txt\" } }"));
        assertThat(dataset.getResourceURI("previous/path.txt").toString(), is("some:uri/current/path.txt"));
    }

    @Test
    public void lookupMappedResourceAbsoluteToRelative() throws IOException {
        Dataset dataset = new DatasetImpl("some/namespace", URI.create("some:uri"));
        dataset.setConfig(new ObjectMapper().readTree("{\"resources\": { \"http://example.org/previous/path.txt\": \"current/path.txt\" } }"));
        assertThat(dataset.getResourceURI("http://example.org/previous/path.txt").toString(), is("some:uri/current/path.txt"));
    }

    @Test
    public void lookupMappedResourceAbsoluteToAbsolute() throws IOException {
        Dataset dataset = new DatasetImpl("some/namespace", URI.create("some:uri"));
        dataset.setConfig(new ObjectMapper().readTree("{\"resources\": { \"http://example.org/previous/path.txt\": \"http://example.org/current/path.txt\" } }"));
        assertThat(dataset.getResourceURI("http://example.org/previous/path.txt").toString(), is("http://example.org/current/path.txt"));
    }

    @Test
    public void lookupNonMappedResourceRelative() throws IOException {
        Dataset dataset = new DatasetImpl("some/namespace", URI.create("some:uri"));
        assertThat(dataset.getResourceURI("previous/path.txt").toString(), is("some:uri/previous/path.txt"));
    }

    @Test
    public void lookupNonMappedResourceAbsolute() throws IOException {
        Dataset dataset = new DatasetImpl("some/namespace", URI.create("some:uri"));
        assertThat(dataset.getResourceURI("http://example.org/previous/path.txt").toString(), is("http://example.org/previous/path.txt"));
    }

    @Test
    public void useCitationFromTableDefinition() throws IOException {
        Dataset dataset = new DatasetImpl("some/namespace", URI.create("some:uri"));
        dataset.setConfig(new ObjectMapper().readTree(getClass().getResourceAsStream("/org/eol/globi/data/test-meta-globi-default-external-schema.json")));
        assertThat(dataset.getCitation(), is("Seltzer, Carrie; Wysocki, William; Palacios, Melissa; Eickhoff, Anna; Pilla, Hannah; Aungst, Jordan; Mercer, Aaron; Quicho, Jamie; Voss, Neil; Xu, Man; J. Ndangalasi, Henry; C. Lovett, Jon; J. Cordeiro, Norbert (2015): Plant-animal interactions from Africa. figshare. https://dx.doi.org/10.6084/m9.figshare.1526128"));
    }

    @Test
    public void useCitationFromMultipleTableDefinitions() throws IOException {
        Dataset dataset = new DatasetImpl("some/namespace", URI.create("some:uri"));
        dataset.setConfig(new ObjectMapper().readTree(getClass().getResourceAsStream("/org/eol/globi/data/test-meta-globi-default-external-schemas.json")));
        assertThat(dataset.getCitation(), is("Seltzer, Carrie; Wysocki, William; Palacios, Melissa; Eickhoff, Anna; Pilla, Hannah; Aungst, Jordan; Mercer, Aaron; Quicho, Jamie; Voss, Neil; Xu, Man; J. Ndangalasi, Henry; C. Lovett, Jon; J. Cordeiro, Norbert (2015): Plant-animal interactions from Africa. figshare. https://dx.doi.org/10.6084/m9.figshare.1526128"));
    }

    @Test
    public void useCitationFromMultipleTableDefinitionsWithDifferentCitations() throws IOException {
        Dataset dataset = new DatasetImpl("some/namespace", URI.create("some:uri"));
        dataset.setConfig(new ObjectMapper().readTree(getClass().getResourceAsStream("/org/eol/globi/data/test-meta-globi-default-external-schemas-different-citations.json")));
        assertThat(dataset.getCitation(), is("a citation; other citation"));
    }


}