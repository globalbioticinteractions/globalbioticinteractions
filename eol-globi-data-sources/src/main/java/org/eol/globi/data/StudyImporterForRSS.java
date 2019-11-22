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
import org.mapdb.DBMaker;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class StudyImporterForRSS extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForRSS.class);
    public static final String HAS_DEPENDENCIES = "hasDependencies";

    public StudyImporterForRSS(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        final String rssFeedUrl = getRssFeedUrlString();
        if (org.apache.commons.lang.StringUtils.isBlank(rssFeedUrl)) {
            throw new StudyImporterException("failed to import [" + getDataset().getNamespace() + "]: no [" + "rssFeedURL" + "] specified");
        }

        final List<Dataset> datasets = getDatasetsForFeed(getDataset());

        final Map<String, Map<String, String>> interactionsWithUnresolvedOccurrenceIds = DBMaker.newTempTreeMap();
        indexArchives(interactionsWithUnresolvedOccurrenceIds, datasets);
        importArchives(interactionsWithUnresolvedOccurrenceIds, datasets);
    }

    public void importArchives(Map<String, Map<String, String>> interactionsWithUnresolvedOccurrenceIds, List<Dataset> datasets) throws StudyImporterException {
        final String msgPrefix = "importing archive(s) from [" + getRssFeedUrlString() + "]";
        LOG.info(msgPrefix + "...");
        for (Dataset dataset : datasets) {
            handleDataset(studyImporter -> {
                if (studyImporter instanceof StudyImporterWithListener) {
                    final EnrichingInteractionListener interactionListener = new EnrichingInteractionListener(
                            interactionsWithUnresolvedOccurrenceIds,
                            ((StudyImporterWithListener) studyImporter).getInteractionListener());
                    ((StudyImporterWithListener) studyImporter).setInteractionListener(interactionListener);
                }

            }, dataset);
        }
        LOG.info(msgPrefix + " done.");
    }

    public void indexArchives(Map<String, Map<String, String>> interactionsWithUnresolvedOccurrenceIds, List<Dataset> datasets) throws StudyImporterException {
        final String msgPrefix1 = "indexing archive(s) from [" + getRssFeedUrlString() + "]";
        LOG.info(msgPrefix1 + "...");

        final IndexingInteractionListener indexingListener = new IndexingInteractionListener(interactionsWithUnresolvedOccurrenceIds);
        for (Dataset dataset : datasets) {
            if (needsIndexing(dataset)) {
                handleDataset(studyImporter -> {
                    if (studyImporter instanceof StudyImporterWithListener) {
                        ((StudyImporterWithListener) studyImporter)
                                .setInteractionListener(indexingListener);
                    }
                }, dataset);
            }
        }
        LOG.info(msgPrefix1 + " done: indexed [" + interactionsWithUnresolvedOccurrenceIds.size() + "] occurrences");
    }

    public boolean needsIndexing(Dataset dataset) {
        return StringUtils.equals(dataset.getOrDefault(HAS_DEPENDENCIES, null), "true");
    }

    interface StudyImporterConfigurator {
        void configure(StudyImporter studyImporter);
    }

    public void handleDataset(StudyImporterConfigurator studyImporterConfigurator, Dataset dataset) throws StudyImporterException {
        nodeFactory.getOrCreateDataset(dataset);
        NodeFactory nodeFactoryForDataset = new NodeFactoryWithDatasetContext(nodeFactory, dataset);
        StudyImporter studyImporter = new StudyImporterFactory().createImporter(dataset, nodeFactoryForDataset);
        studyImporter.setDataset(dataset);
        if (getLogger() != null) {
            studyImporter.setLogger(getLogger());
        }
        studyImporterConfigurator.configure(studyImporter);
        studyImporter.importStudy();
    }

    public String getRssFeedUrlString() {
        return getRSSEndpoint(getDataset());
    }

    static String getRSSEndpoint(Dataset dataset) {
        String rss = null;
        try {
            rss = DatasetUtil.getNamedResourceURI(dataset, "rss");
        } catch (IOException e) {
            //
        }
        return DatasetUtil.getValueOrDefault(dataset.getConfig(), "url", rss);
    }


    static List<Dataset> getDatasetsForFeed(Dataset datasetOrig) throws StudyImporterException {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed;
        String rss = getRSSEndpoint(datasetOrig);
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
                String title = StringUtils.trim(((SyndEntry) entry).getTitle());
                if (e == null) {
                    LOG.info("skipping [" + title + "] : not supported (yet).");
                } else {
                    LOG.info("including [" + title + "].");
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
        return embeddedDatasetFor(datasetOrig,
                citation,
                URI.create(archiveURI),
                false
        );
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

        // for now, only include Arctos, see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/134
        if (foreignEntries.containsKey("dwca")
                && StringUtils.contains(title, "(Arctos)")
                && !StringUtils.contains(title, "GGBN")
        ) {
            String citation = StringUtils.trim(title);

            dataset = embeddedDatasetFor(datasetOrig,
                    citation,
                    URI.create(foreignEntries.get("dwca")),
                    true
            );
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

    static Dataset embeddedDatasetFor(final Dataset datasetOrig,
                                      final String embeddedCitation,
                                      final URI embeddedArchiveURI) {
        return embeddedDatasetFor(datasetOrig, embeddedCitation, embeddedArchiveURI, false);
    }


    static Dataset embeddedDatasetFor(final Dataset datasetOrig,
                                      final String embeddedCitation,
                                      final URI embeddedArchiveURI,
                                      final boolean hasDependencies) {
        ObjectNode config = new ObjectMapper().createObjectNode();
        config.put("citation", embeddedCitation);
        config.put("format", "application/dwca");
        config.put(HAS_DEPENDENCIES, hasDependencies);

        DatasetProxy dataset = new DatasetProxy(datasetOrig) {
            @Override
            public URI getArchiveURI() {
                return embeddedArchiveURI;
            }
        };
        dataset.setConfig(config);
        return dataset;
    }

    static class EnrichingInteractionListener implements InteractionListener {
        private final Map<String, Map<String, String>> interactionsWithUnresolvedOccurrenceIds;
        private final InteractionListener interactionListener;

        public EnrichingInteractionListener(Map<String, Map<String, String>> interactionsWithUnresolvedOccurrenceIds, InteractionListener interactionListener) {
            this.interactionsWithUnresolvedOccurrenceIds = interactionsWithUnresolvedOccurrenceIds;
            this.interactionListener = interactionListener;
        }

        @Override
        public void newLink(Map<String, String> properties) throws StudyImporterException {
            Map<String, String> enrichedProperties = null;
            if (properties.containsKey(StudyImporterForTSV.TARGET_OCCURRENCE_ID)) {
                String targetOccurrenceId = properties.get(StudyImporterForTSV.TARGET_OCCURRENCE_ID);
                Map<String, String> targetProperties = interactionsWithUnresolvedOccurrenceIds.get(targetOccurrenceId);
                if (targetProperties != null) {
                    TreeMap<String, String> enrichedMap = new TreeMap<>(properties);
                    enrichProperties(targetProperties, enrichedMap, StudyImporterForTSV.SOURCE_TAXON_NAME, StudyImporterForTSV.TARGET_TAXON_NAME);
                    enrichProperties(targetProperties, enrichedMap, StudyImporterForTSV.SOURCE_TAXON_ID, StudyImporterForTSV.TARGET_TAXON_ID);
                    enrichProperties(targetProperties, enrichedMap, StudyImporterForTSV.SOURCE_LIFE_STAGE_NAME, StudyImporterForTSV.TARGET_LIFE_STAGE_NAME);
                    enrichProperties(targetProperties, enrichedMap, StudyImporterForTSV.SOURCE_LIFE_STAGE_ID, StudyImporterForTSV.TARGET_LIFE_STAGE_ID);
                    enrichProperties(targetProperties, enrichedMap, StudyImporterForTSV.SOURCE_BODY_PART_NAME, StudyImporterForTSV.TARGET_BODY_PART_NAME);
                    enrichProperties(targetProperties, enrichedMap, StudyImporterForTSV.SOURCE_BODY_PART_ID, StudyImporterForTSV.TARGET_BODY_PART_ID);
                    enrichedProperties = enrichedMap;
                }
            }
            interactionListener.newLink(enrichedProperties == null ? properties : enrichedProperties);
        }

        public void enrichProperties(Map<String, String> targetProperties, TreeMap<String, String> enrichedMap, String sourceKey, String targetKey) {
            String value = targetProperties.get(sourceKey);
            if (StringUtils.isNotBlank(value)) {
                enrichedMap.put(targetKey, value);
            }
        }
    }

    static class IndexingInteractionListener implements InteractionListener {
        private final Map<String, Map<String, String>> interactionsWithUnresolvedOccurrenceIds;

        public IndexingInteractionListener(Map<String, Map<String, String>> interactionsWithUnresolvedOccurrenceIds) {
            this.interactionsWithUnresolvedOccurrenceIds = interactionsWithUnresolvedOccurrenceIds;
        }

        @Override
        public void newLink(Map<String, String> properties) throws StudyImporterException {
            if (properties.containsKey(StudyImporterForTSV.TARGET_OCCURRENCE_ID)
                    && properties.containsKey(StudyImporterForTSV.SOURCE_OCCURRENCE_ID)) {
                String value = properties.get(StudyImporterForTSV.SOURCE_OCCURRENCE_ID);

                if (StringUtils.startsWith(value, "http://arctos.database.museum/guid/")) {
                    String[] splitValue = StringUtils.split(value, "?");
                    value = splitValue.length == 1 ? value : splitValue[0];
                }
                interactionsWithUnresolvedOccurrenceIds.put(value, new HashMap<>(properties));
            }
        }
    }
}
