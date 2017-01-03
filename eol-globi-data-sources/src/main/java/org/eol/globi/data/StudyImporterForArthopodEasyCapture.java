package org.eol.globi.data;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetProxy;
import org.eol.globi.service.DatasetUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class StudyImporterForArthopodEasyCapture extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForArthopodEasyCapture.class);

    public StudyImporterForArthopodEasyCapture(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        final String rssFeedUrl = getRssFeedUrlString();
        if (org.apache.commons.lang.StringUtils.isBlank(rssFeedUrl)) {
            throw new StudyImporterException("failed to import [" + getDataset().getNamespace() + "]: no [" + "rssFeedURL" + "] specified");
        }

        final String msgPrefix = "importing archive(s) from [" + getRssFeedUrlString() + "]";
        LOG.info(msgPrefix + "...");
        final List<StudyImporter> studyImporters = getStudyImportersForRSSFeed(getDataset(), parserFactory, nodeFactory);
        for (StudyImporter importer : studyImporters) {
            if (importer != null) {
                if (getLogger() != null) {
                    importer.setLogger(getLogger());
                }
                importer.importStudy();
            }
        }
        LOG.info(msgPrefix + " done.");
    }

    public String getRssFeedUrlString() {
        Dataset dataset = getDataset();
        return getRss(dataset);
    }

    static String getRss(Dataset dataset) {
        return DatasetUtil.getNamedResourceURI(dataset, "rss");
    }

    public static List<StudyImporter> getStudyImportersForRSSFeed(Dataset datasetOrig, ParserFactory parserFactory, NodeFactory
            nodeFactory) throws StudyImporterException {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed;
        String rss = getRss(datasetOrig);
        try {
            feed = input.build(new XmlReader(new URL(rss)));
        } catch (FeedException | IOException e) {
            throw new StudyImporterException("failed to read rss feed [" + rss + "]", e);
        }

        List<StudyImporter> importers = new ArrayList<StudyImporter>();
        final List entries = feed.getEntries();
        for (Object entry : entries) {
            if (entry instanceof SyndEntry) {
                SyndEntry syndEntry = (SyndEntry) entry;
                Dataset dataset = embeddedDatasetFor(datasetOrig, StringUtils.trim(syndEntry.getDescription().getValue()), URI.create(StringUtils.trim(syndEntry.getLink())));

                final StudyImporterForSeltmann studyImporter = new StudyImporterForSeltmann(parserFactory, nodeFactory);
                studyImporter.setDataset(dataset);

                importers.add(studyImporter);
            }
        }
        return importers;
    }

    static Dataset embeddedDatasetFor(Dataset datasetOrig, String embeddedCitation, URI embeddedArchiveURI) {
        ObjectNode config = new ObjectMapper().createObjectNode();
        config.put("citation", embeddedCitation);
        ObjectNode referencesNode = new ObjectMapper().createObjectNode();
        referencesNode.put("archive", embeddedArchiveURI.toString());
        config.put("resources", referencesNode);

        DatasetProxy dataset = new DatasetProxy(datasetOrig);
        dataset.setConfig(config);
        return dataset;
    }

}
