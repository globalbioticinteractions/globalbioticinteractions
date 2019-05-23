package org.eol.globi.data;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetConstant;
import org.eol.globi.service.DatasetImpl;
import org.eol.globi.service.DatasetUtil;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyImporterForRSSTest {

    @Test
    public void readRSS() throws StudyImporterException, IOException {
        final Dataset dataset = getDatasetGroup();
        List<Dataset> datasets = StudyImporterForRSS.getDatasetsForFeed(dataset);
        assertThat(datasets.size(), is(3));
    }

    @Test
    public void embeddedDataset() throws IOException {
        Dataset embeddedDataset = StudyImporterForRSS.embeddedDatasetFor(getDatasetGroup(), "some other citation", URI.create("http://example.com/archive.zip"), "seltmann");
        assertThat(embeddedDataset.getCitation(), is("some other citation"));
        assertThat(embeddedDataset.getOrDefault(DatasetConstant.SHOULD_RESOLVE_REFERENCES, "foo"), is("foo"));
        assertThat(DatasetUtil.getNamedResourceURI(embeddedDataset, "archive"), is("http://example.com/archive.zip"));
    }

    @Test
    public void embeddedDatasetWithConfig() throws IOException {
        Dataset embeddedDataset = StudyImporterForRSS.embeddedDatasetFor(getDatasetGroupWithProperty(), "some other citation", URI.create("http://example.com/archive.zip"), "seltmann");
        assertThat(embeddedDataset.getCitation(), is("some other citation"));
        assertThat(embeddedDataset.getOrDefault(DatasetConstant.SHOULD_RESOLVE_REFERENCES, "true"), is("false"));
        assertThat(DatasetUtil.getNamedResourceURI(embeddedDataset, "archive"), is("http://example.com/archive.zip"));
    }

    private DatasetImpl getDatasetGroup() throws IOException {
        DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create("http://example.com"));
        JsonNode config = new ObjectMapper().readTree("{ \"resources\": { \"rss\": \"http://amnh.begoniasociety.org/dwc/rss.xml\" } }");
        dataset.setConfig(config);
        return dataset;
    }

    private DatasetImpl getDatasetVertnet() throws IOException {
        DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create("http://example.com"));
        JsonNode config = new ObjectMapper().readTree("{ \"resources\": { \"rss\": \"http://ipt.vertnet.org:8080/ipt/rss.do\" } }");
        dataset.setConfig(config);
        return dataset;
    }

    private DatasetImpl getDatasetGroupWithProperty() throws IOException {
        DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create("http://example.com"));
        JsonNode config = new ObjectMapper().readTree("{ \"" + DatasetConstant.SHOULD_RESOLVE_REFERENCES + "\": false, \"resources\": { \"rss\": \"http://amnh.begoniasociety.org/dwc/rss.xml\" } }");
        dataset.setConfig(config);
        return dataset;
    }

}