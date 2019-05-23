package org.eol.globi.data;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.eol.globi.service.DatasetLocal;
import org.globalbioticinteractions.cache.CachePullThrough;
import org.globalbioticinteractions.dataset.DatasetWithCache;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class StudyImporterForRSSIT extends GraphDBTestCase {

    @Test
    public void importVertnet() throws StudyImporterException, IOException {
        File tempFile = File.createTempFile("test", ".tmp", new File("target"));
        StudyImporter importer = new StudyImporterTestFactory(nodeFactory)
                .instantiateImporter(StudyImporterForRSS.class);

        DatasetWithCache datasetWithCache = new DatasetWithCache(new DatasetLocal(), new CachePullThrough("testing", tempFile.getParentFile().getAbsolutePath()));

        ObjectNode rssUrl = new ObjectMapper().createObjectNode();

        rssUrl.put("rss", "http://ipt.vertnet.org:8080/ipt/rss.do");
        ObjectNode configNode = new ObjectMapper().createObjectNode();
        configNode.put("resources", rssUrl);
        datasetWithCache.setConfig(configNode);

        importer.setDataset(datasetWithCache);
        importStudy(importer);
    }

    @Test
    public void importEasyArthoprodCapture() throws StudyImporterException {
        StudyImporter importer = new StudyImporterTestFactory(nodeFactory)
                .instantiateImporter(StudyImporterForRSS.class);
        DatasetLocal dataset = new DatasetLocal();
        ObjectNode rssUrl = new ObjectMapper().createObjectNode();

        rssUrl.put("rss", "http://amnh.begoniasociety.org/dwc/rss.xml");
        ObjectNode configNode = new ObjectMapper().createObjectNode();
        configNode.put("resources", rssUrl);
        dataset.setConfig(configNode);
        importer.setDataset(dataset);
        importStudy(importer);
    }

}