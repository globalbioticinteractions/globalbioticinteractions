package org.eol.globi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.globalbioticinteractions.doi.DOI;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DatasetImplTest {


    @Test
    public void useCitationFromTableDefinition() throws IOException {
        Dataset dataset = new DatasetWithResourceMapping("some/namespace", URI.create("some:uri"), new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop()));
        dataset.setConfig(new ObjectMapper().readTree(getClass().getResourceAsStream("/org/eol/globi/data/test-meta-globi-default-external-schema.json")));
        assertThat(dataset.getCitation(), is("Seltzer, Carrie; Wysocki, William; Palacios, Melissa; Eickhoff, Anna; Pilla, Hannah; Aungst, Jordan; Mercer, Aaron; Quicho, Jamie; Voss, Neil; Xu, Man; J. Ndangalasi, Henry; C. Lovett, Jon; J. Cordeiro, Norbert (2015): Plant-animal interactions from Africa. figshare. https://dx.doi.org/10.6084/m9.figshare.1526128"));
    }

    @Test
    public void useCitationFromMultipleTableDefinitions() throws IOException {
        Dataset dataset = new DatasetWithResourceMapping("some/namespace", URI.create("some:uri"), new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop()));
        dataset.setConfig(new ObjectMapper().readTree(getClass().getResourceAsStream("/org/eol/globi/data/test-meta-globi-default-external-schemas.json")));
        assertThat(dataset.getCitation(), is("Seltzer, Carrie; Wysocki, William; Palacios, Melissa; Eickhoff, Anna; Pilla, Hannah; Aungst, Jordan; Mercer, Aaron; Quicho, Jamie; Voss, Neil; Xu, Man; J. Ndangalasi, Henry; C. Lovett, Jon; J. Cordeiro, Norbert (2015): Plant-animal interactions from Africa. figshare. https://dx.doi.org/10.6084/m9.figshare.1526128"));
    }

    @Test
    public void useCitationFromMultipleTableDefinitionsWithDifferentCitations() throws IOException {
        Dataset dataset = new DatasetWithResourceMapping("some/namespace", URI.create("some:uri"), new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop()));
        dataset.setConfig(new ObjectMapper().readTree(getClass().getResourceAsStream("/org/eol/globi/data/test-meta-globi-default-external-schemas-different-citations.json")));
        assertThat(dataset.getCitation(), is("a citation; other citation"));
    }

    @Test
    public void doiScrubbing() throws IOException {
        Dataset dataset = new DatasetWithResourceMapping("some/namespace", URI.create("some:uri"), new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop()));
        dataset.setConfig(new ObjectMapper().readTree("{\"doi\": \"doi:http://dx.doi.org/10.2980/1195-6860(2006)13[23:TDOFUB]2.0.CO;2\" }"));
        assertThat(dataset.getDOI(), is(new DOI("2980", "1195-6860(2006)13[23:TDOFUB]2.0.CO;2")));
    }

    @Test
    public void doiExpected() throws IOException {
        Dataset dataset = new DatasetWithResourceMapping("some/namespace", URI.create("some:uri"), new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop()));
        dataset.setConfig(new ObjectMapper().readTree("{\"doi\": \"http://dx.doi.org/10.2980/1195-6860(2006)13[23:TDOFUB]2.0.CO;2\" }"));
        assertThat(dataset.getDOI(), is(new DOI("2980", "1195-6860(2006)13[23:TDOFUB]2.0.CO;2")));
    }

    @Test
    public void doiExpectedPrefix() throws IOException {
        Dataset dataset = new DatasetWithResourceMapping("some/namespace", URI.create("some:uri"), new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop()));
        dataset.setConfig(new ObjectMapper().readTree("{\"doi\": \"doi:10.2980/1195-6860(2006)13[23:TDOFUB]2.0.CO;2\" }"));
        assertThat(dataset.getDOI(), is(new DOI("2980", "1195-6860(2006)13[23:TDOFUB]2.0.CO;2")));
    }


}