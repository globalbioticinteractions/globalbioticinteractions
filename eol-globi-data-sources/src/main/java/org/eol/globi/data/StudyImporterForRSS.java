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
import org.eol.globi.service.StudyImporterFactory;
import org.globalbioticinteractions.dataset.DatasetWithCache;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class StudyImporterForRSS extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForRSS.class);

    public StudyImporterForRSS(ParserFactory parserFactory, NodeFactory nodeFactory) {
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
        final List<Dataset> datasets = getDatasetsForFeed(getDataset());
        for (Dataset dataset : datasets) {
            nodeFactory.getOrCreateDataset(dataset);
            NodeFactory nodeFactoryForDataset = new NodeFactoryWithDatasetContext(nodeFactory, dataset);
            StudyImporter studyImporter = new StudyImporterFactory().createImporter(dataset, nodeFactoryForDataset);
            studyImporter.setDataset(dataset);

            if (getLogger() != null) {
                studyImporter.setLogger(getLogger());
            }
            studyImporter.importStudy();
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

    public static List<Dataset> getDatasetsForFeed(Dataset datasetOrig) throws StudyImporterException {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed;
        String rss = getRss(datasetOrig);
        try {
            feed = input.build(new XmlReader(new URL(rss)));
        } catch (FeedException | IOException e) {
            throw new StudyImporterException("failed to read rss feed [" + rss + "]", e);
        }

        List<Dataset> datasets = new ArrayList<>();
        final List entries = feed.getEntries();
        for (Object entry : entries) {
            if (entry instanceof SyndEntry) {
                Dataset e = datasetFor(datasetOrig, (SyndEntry) entry);
                if (e == null) {
                    LOG.info("skipping [" + ((SyndEntry) entry).getTitle() + "] : not supported (yet).");
                } else {
                    datasets.add(e);
                }
            }
        }
        return datasets;
    }

    public static Dataset datasetFor(Dataset datasetOrig, SyndEntry entry) {
        return isLikelyIPTEntry(entry)
                ? attemptActosIPT(datasetOrig, entry)
                : attemptEasyArthropodCapture(datasetOrig, entry);

    }

    private static Dataset attemptEasyArthropodCapture(Dataset datasetOrig, SyndEntry entry) {
        String citation = StringUtils.trim(entry.getDescription().getValue());
        String archiveURI = StringUtils.trim(entry.getLink());
        String format = "seltmann";
        return embeddedDatasetFor(datasetOrig,
                citation,
                URI.create(archiveURI),
                format);
    }

    private static boolean isLikelyIPTEntry(SyndEntry entry) {
        Map<String, String> foreignEntries = parseForeignEntries(entry);
        return foreignEntries.containsKey("eml")
                || foreignEntries.containsKey("dwca");

    }

    private static Dataset attemptActosIPT(Dataset datasetOrig, SyndEntry entry) {
        Dataset dataset = null;
        Map<String, String> foreignEntries = parseForeignEntries(entry);
        String title = entry.getTitle();

        // for now, only include Arctos, see https://github.com/jhpoelen/eol-globi-data/issues/134
        if (foreignEntries.containsKey("dwca") && StringUtils.contains(title, "(Arctos)")) {
            String citation = StringUtils.trim(title);

            dataset = embeddedDatasetFor(datasetOrig,
                    citation,
                    URI.create(foreignEntries.get("dwca")),
                    "application/dwca");
        }

        return dataset;
    }

    private static Map<String, String> parseForeignEntries(SyndEntry entry) {
        Object foreignMarkup = entry.getForeignMarkup();
        Map<String, String> foreignEntries = new TreeMap<>();
        if (foreignMarkup instanceof Collection) {
            Collection foreign = (Collection) foreignMarkup;
            for (Object o : foreign) {
                if (o instanceof org.jdom.Element) {
                    org.jdom.Element elem = ((org.jdom.Element) o);
                    foreignEntries.put(elem.getName(), elem.getValue());
                }
            }
        }
        return foreignEntries;
    }

    static Dataset embeddedDatasetFor(Dataset datasetOrig, String embeddedCitation, final URI embeddedArchiveURI, String format) {
        ObjectNode config = new ObjectMapper().createObjectNode();
        config.put("citation", embeddedCitation);
        ObjectNode referencesNode = new ObjectMapper().createObjectNode();
        referencesNode.put("archive", embeddedArchiveURI.toString());
        config.put("resources", referencesNode);
        config.put("format", format);

        DatasetProxy dataset = new DatasetProxy(datasetOrig) {
            @Override
            public URI getArchiveURI() {
                return embeddedArchiveURI;
            }
        };
        dataset.setConfig(config);
        return dataset;
    }

}
