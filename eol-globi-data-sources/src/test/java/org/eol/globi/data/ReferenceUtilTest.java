package org.eol.globi.data;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.service.DatasetImpl;
import org.eol.globi.util.ResourceUtil;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ReferenceUtilTest {

    @Test
    public void sourceCitation() {
        String s = ReferenceUtil.sourceCitationLastAccessed(new DatasetImpl("some/namespace", URI.create("http://example")), "some source citation. ");
        assertThat(s, startsWith("some source citation. Accessed at http://example on "));
        assertThat(s, endsWith("."));
    }

    @Test
    public void sourceCitationDatasetNoConfig() {
        String citation = ReferenceUtil.sourceCitationLastAccessed(new DatasetImpl("some/namespace", URI.create("http://example")));
        assertThat(citation, startsWith("http://example . Accessed at http://example on"));
    }

    @Test
    public void sourceCitationDataset() throws IOException {
        DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create("http://example"));
        JsonNode config = new ObjectMapper().readTree("{ \"resources\": { \"archive\": \"archive.zip\" } }");
        dataset.setConfig(config);
        String citation = ReferenceUtil.sourceCitationLastAccessed(dataset);
        assertThat(citation, startsWith("http://example . Accessed at http://example on"));
    }

    @Test
    public void generateSourceCitation() throws IOException, StudyImporterException {
        final InputStream inputStream = ResourceUtil.asInputStream("/org/eol/globi/data/test-meta-globi.json", StudyImporterForMetaTable.class);

        DatasetImpl dataset = new DatasetImpl(null, URI.create("http://base"));
        dataset.setConfig(new ObjectMapper().readTree(inputStream));

        String citation = ReferenceUtil.sourceCitationLastAccessed(dataset);
        assertThat(citation, startsWith("Seltzer, Carrie; Wysocki, William; Palacios, Melissa; Eickhoff, Anna; Pilla, Hannah; Aungst, Jordan; Mercer, Aaron; Quicho, Jamie; Voss, Neil; Xu, Man; J. Ndangalasi, Henry; C. Lovett, Jon; J. Cordeiro, Norbert (2015): Plant-animal interactions from Africa. figshare. https://dx.doi.org/10.6084/m9.figshare.1526128 . Accessed at https://ndownloader.figshare.com/files/2231424"));
    }

}