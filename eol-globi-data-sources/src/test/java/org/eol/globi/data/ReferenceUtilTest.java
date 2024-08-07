package org.eol.globi.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocal;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.CitationUtil;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.hamcrest.core.StringStartsWith.startsWith;
public class ReferenceUtilTest {

    @Test
    public void sourceCitation() {
        String s = CitationUtil.sourceCitationLastAccessed(new DatasetWithResourceMapping("some/namespace", URI.create("http://example"), new ResourceServiceLocal(new InputStreamFactoryNoop())), "some source citation.");
        assertThat(s, startsWith("some source citation. Accessed at <http://example> on "));
        assertThat(s, endsWith("."));
    }

    @Test
    public void sourceCitationDatasetNoConfig() {
        String citation = CitationUtil.sourceCitationLastAccessed(
                new DatasetWithResourceMapping("some/namespace", URI.create("http://example"), new ResourceServiceLocal(new InputStreamFactoryNoop())));
        assertThat(citation, startsWith("<http://example>. Accessed at <http://example> on"));
    }

    @Test
    public void sourceCitationDataset() throws IOException {
        DatasetImpl dataset = new DatasetWithResourceMapping("some/namespace", URI.create("http://example"), new ResourceServiceLocal(new InputStreamFactoryNoop()));
        JsonNode config = new ObjectMapper().readTree("{ \"resources\": { \"archive\": \"archive.zip\" } }");
        dataset.setConfig(config);
        String citation = CitationUtil.sourceCitationLastAccessed(dataset);
        assertThat(citation, startsWith("<http://example>. Accessed at <http://example> on"));
    }

    @Test
    public void sourceCitationDatasetLocalResource() throws IOException {
        DatasetImpl dataset = new DatasetWithResourceMapping("some/namespace", URI.create("http://example"), new ResourceServiceLocal(new InputStreamFactoryNoop()));
        JsonNode config = new ObjectMapper().readTree("{ \"url\": \"interactions.tsv\" }");
        dataset.setConfig(config);
        String citation = CitationUtil.sourceCitationLastAccessed(dataset);
        assertThat(citation, startsWith("<http://example>. Accessed at <http://example> on"));
    }

    @Test
    public void sourceCitationDatasetLocalResourceNonURI() throws IOException {
        DatasetImpl dataset = new DatasetWithResourceMapping("some/namespace", URI.create("http://example"), new ResourceServiceLocal(new InputStreamFactoryNoop()));
        JsonNode config = new ObjectMapper().readTree("{ \"url\": \"foo bar\" }");
        dataset.setConfig(config);
        String citation = CitationUtil.sourceCitationLastAccessed(dataset);
        assertThat(citation, startsWith("<http://example>. Accessed at <http://example> on"));
    }

    @Test
    public void sourceCitationDatasetLocalResourceURI() throws IOException {
        DatasetImpl dataset = new DatasetWithResourceMapping("some/namespace", URI.create("http://example"), new ResourceServiceLocal(new InputStreamFactoryNoop()));
        JsonNode config = new ObjectMapper().readTree("{ \"url\": \"https://example.org/foo.tsv\" }");
        dataset.setConfig(config);
        String citation = CitationUtil.sourceCitationLastAccessed(dataset);
        assertThat(citation, startsWith("<http://example>. Accessed at <https://example.org/foo.tsv> on"));
    }

    @Test
    public void generateSourceCitation() throws IOException, StudyImporterException {
        final InputStream inputStream = getClass().getResourceAsStream("/org/eol/globi/data/test-meta-globi.json");

        DatasetImpl dataset = new DatasetWithResourceMapping(null, URI.create("http://base"), new ResourceServiceLocal(new InputStreamFactoryNoop()));
        dataset.setConfig(new ObjectMapper().readTree(inputStream));

        String citation = CitationUtil.sourceCitationLastAccessed(dataset);
        assertThat(citation, startsWith("Seltzer, Carrie; Wysocki, William; Palacios, Melissa; Eickhoff, Anna; Pilla, Hannah; Aungst, Jordan; Mercer, Aaron; Quicho, Jamie; Voss, Neil; Xu, Man; J. Ndangalasi, Henry; C. Lovett, Jon; J. Cordeiro, Norbert (2015): Plant-animal interactions from Africa. figshare. https://dx.doi.org/10.6084/m9.figshare.1526128. Accessed at <https://ndownloader.figshare.com/files/2231424>"));
    }

    @Test
    public void citationFor() throws IOException, StudyImporterException {
        DatasetImpl dataset = new DatasetWithResourceMapping(null, URI.create("http://base"), new ResourceServiceLocal(new InputStreamFactoryNoop()));
        dataset.setConfig(new ObjectMapper().readTree("{ \"citation\": \"http://gomexsi.tamucc.edu\" }"));

        String citation = CitationUtil.citationFor(dataset);
        assertThat(citation, is("http://gomexsi.tamucc.edu"));
    }

    @Test
    public void generateReferenceAndReferenceId() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(DatasetImporterForMetaTable.AUTHOR, "Johnny");
                put(DatasetImporterForMetaTable.TITLE, "My first pony");
                put(DatasetImporterForMetaTable.YEAR, "1981");
                put(DatasetImporterForMetaTable.JOURNAL, "journal of bla");
            }
        };

        assertThat(ReferenceUtil.generateReferenceCitation(properties), Is.is("Johnny, 1981. My first pony. journal of bla."));
        properties.put(DatasetImporterForMetaTable.VOLUME, "123");
        assertThat(ReferenceUtil.generateReferenceCitation(properties), Is.is("Johnny, 1981. My first pony. journal of bla, 123."));
        properties.put(DatasetImporterForMetaTable.NUMBER, "11");
        assertThat(ReferenceUtil.generateReferenceCitation(properties), Is.is("Johnny, 1981. My first pony. journal of bla, 123(11)."));
        properties.put(DatasetImporterForMetaTable.PAGES, "33");

        assertThat(ReferenceUtil.generateReferenceCitation(properties), Is.is("Johnny, 1981. My first pony. journal of bla, 123(11), pp.33."));

    }

    @Test
    public void generateReferenceCitation() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(DatasetImporterForTSV.REFERENCE_URL, "https://example.org/");
            }
        };

        assertThat(ReferenceUtil.generateReferenceCitation(properties), Is.is("https://example.org/"));

    }

    @Test
    public void generateReferenceCitationFromDOI() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(DatasetImporterForTSV.REFERENCE_DOI, "10.12/345");
            }
        };

        assertThat(ReferenceUtil.generateReferenceCitation(properties), Is.is("doi:10.12/345"));

    }

    @Test
    public void generateReferenceCitationFromDOINotURL() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(DatasetImporterForTSV.REFERENCE_DOI, "10.12/345");
                put(DatasetImporterForTSV.REFERENCE_URL, "https://example.org/");
            }
        };

        assertThat(ReferenceUtil.generateReferenceCitation(properties), Is.is("doi:10.12/345"));

    }

    @Test
    public void generateReferenceCitationFromURLNotMalformedDOI() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(DatasetImporterForTSV.REFERENCE_DOI, "foo");
                put(DatasetImporterForTSV.REFERENCE_URL, "https://example.org/");
            }
        };

        assertThat(ReferenceUtil.generateReferenceCitation(properties), Is.is("https://example.org/"));

    }



}