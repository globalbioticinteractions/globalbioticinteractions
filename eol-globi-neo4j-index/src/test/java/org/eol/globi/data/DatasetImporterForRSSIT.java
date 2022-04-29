package org.eol.globi.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eol.globi.service.DatasetLocal;
import org.eol.globi.util.ResourceServiceLocal;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.cache.CachePullThrough;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetWithCache;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;

public class DatasetImporterForRSSIT extends GraphDBTestCase {

    @Test
    public void importVertnet() throws StudyImporterException, IOException {
        File tempFile = File.createTempFile("test", ".tmp", new File("target"));
        DatasetImporter importer = new StudyImporterTestFactory(nodeFactory)
                .instantiateImporter(DatasetImporterForRSS.class);

        DatasetWithCache datasetWithCache = new DatasetWithCache(new DatasetLocal(new ResourceServiceLocal(inStream -> inStream)), new CachePullThrough("testing", tempFile.getParentFile().getAbsolutePath(), new ResourceServiceLocalAndRemote(inStream -> inStream), new ResourceServiceLocal(inStream -> inStream)));

        ObjectNode rssUrl = new ObjectMapper().createObjectNode();

        rssUrl.put("rss", "http://ipt.vertnet.org:8080/ipt/rss.do");
        ObjectNode configNode = new ObjectMapper().createObjectNode();
        configNode.set("resources", rssUrl);
        datasetWithCache.setConfig(configNode);

        importer.setDataset(datasetWithCache);
        importStudy(importer);
    }

    @Test
    public void importUCSB() throws StudyImporterException, IOException {
        File tempFile = File.createTempFile("test", ".tmp", new File("target"));
        DatasetImporter importer = new StudyImporterTestFactory(nodeFactory)
                .instantiateImporter(DatasetImporterForRSS.class);

        final DatasetLocal dataset = new DatasetLocal(new ResourceServiceLocal(inStream -> inStream));
        DatasetWithCache datasetWithCache = new DatasetWithCache(dataset, new CachePullThrough("testing", tempFile.getParentFile().getAbsolutePath(), new ResourceServiceLocalAndRemote(inStream -> inStream), new ResourceServiceLocal(inStream -> inStream)));

        ObjectNode rssUrl = new ObjectMapper().createObjectNode();

        rssUrl.put("rss", "https://symbiota.ccber.ucsb.edu/webservices/dwc/rss.xml");
        ObjectNode configNode = new ObjectMapper().createObjectNode();
        configNode.set("resources", rssUrl);
        datasetWithCache.setConfig(configNode);

        importer.setDataset(datasetWithCache);
        importStudy(importer);

        final Dataset datasetImported = nodeFactory.getOrCreateDataset(dataset);

        assertThat(datasetImported.getNamespace(), Is.is("jhpoelen/eol-globidata"));
    }

    @Test
    public void importEasyArthoprodCapture() throws StudyImporterException {
        DatasetImporter importer = new StudyImporterTestFactory(nodeFactory)
                .instantiateImporter(DatasetImporterForRSS.class);
        DatasetLocal dataset = new DatasetLocal(new ResourceServiceLocal(inStream -> inStream));
        ObjectNode rssUrl = new ObjectMapper().createObjectNode();

        rssUrl.put("rss", "http://amnh.begoniasociety.org/dwc/rss.xml");
        ObjectNode configNode = new ObjectMapper().createObjectNode();
        configNode.set("resources", rssUrl);
        dataset.setConfig(configNode);
        importer.setDataset(dataset);
        importStudy(importer);
    }

}