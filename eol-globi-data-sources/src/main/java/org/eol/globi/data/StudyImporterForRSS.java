package org.eol.globi.data;

import com.sun.syndication.feed.synd.SyndContent;
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
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetProxy;
import org.globalbioticinteractions.dataset.DatasetUtil;
import org.eol.globi.service.StudyImporterFactoryImpl;
import org.eol.globi.service.TaxonUtil;
import org.mapdb.DBMaker;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class StudyImporterForRSS extends NodeBasedImporter {
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
            try {
                handleDataset(studyImporter -> {
                    if (studyImporter instanceof StudyImporterWithListener) {
                        final EnrichingInteractionListener interactionListener = new EnrichingInteractionListener(
                                interactionsWithUnresolvedOccurrenceIds,
                                ((StudyImporterWithListener) studyImporter).getInteractionListener());
                        ((StudyImporterWithListener) studyImporter).setInteractionListener(interactionListener);
                    }

                }, dataset);
            } catch (StudyImporterException | IllegalStateException ex) {
                LogUtil.logError(getLogger(), ex);
            }
        }
        LOG.info(msgPrefix + " done.");
    }

    public void indexArchives(Map<String, Map<String, String>> interactionsWithUnresolvedOccurrenceIds, List<Dataset> datasets) throws StudyImporterException {
        final String msgPrefix1 = "indexing archive(s) from [" + getRssFeedUrlString() + "]";
        LOG.info(msgPrefix1 + "...");

        final IndexingInteractionListener indexingListener = new IndexingInteractionListener(interactionsWithUnresolvedOccurrenceIds);
        for (Dataset dataset : datasets) {
            if (needsIndexing(dataset)) {
                try {
                    handleDataset(studyImporter -> {
                        studyImporter.setLogger(getLogger());
                        if (studyImporter instanceof StudyImporterWithListener) {
                            ((StudyImporterWithListener) studyImporter)
                                    .setInteractionListener(indexingListener);
                        }
                    }, dataset);
                } catch (StudyImporterException | IllegalStateException ex) {
                    LogUtil.logError(getLogger(), ex);
                }
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
        getNodeFactory().getOrCreateDataset(dataset);
        NodeFactory nodeFactoryForDataset = new NodeFactoryWithDatasetContext(getNodeFactory(), dataset);
        StudyImporter studyImporter = new StudyImporterFactoryImpl(nodeFactoryForDataset).createImporter(dataset);
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
        URI rss = null;
        try {
            rss = dataset.getLocalURI(URI.create("rss"));
        } catch (IOException e) {
            //
        }
        return DatasetUtil.getValueOrDefault(dataset.getConfig(), "url", rss == null ? null : rss.toString());
    }


    static List<Dataset> getDatasetsForFeed(Dataset datasetOrig) throws StudyImporterException {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed;
        String rss = getRSSEndpoint(datasetOrig);
        try {
            feed = input.build(new XmlReader(datasetOrig.retrieve(URI.create(rss))));
        } catch (FeedException | IOException e) {
            throw new StudyImporterException("failed to read rss feed [" + rss + "]", e);
        }

        List<Dataset> datasets = new ArrayList<>();
        final List entries = feed.getEntries();
        for (Object entry : entries) {
            if (entry instanceof SyndEntry) {
                String title = StringUtils.trim(((SyndEntry) entry).getTitle());

                if (shouldIncludeTitleInDatasetCollection(title, datasetOrig)) {
                    Dataset e = datasetFor(datasetOrig, (SyndEntry) entry);
                    LOG.info("including [" + title + "].");
                    if (e != null) {
                        datasets.add(e);
                    }
                    ;
                } else {
                    LOG.info("skipping [" + title + "] : was not included or excluded.");
                }
            }
        }
        return datasets;
    }

    protected static boolean shouldIncludeTitleInDatasetCollection(String title, Dataset dataset) {
        Predicate<String> includes = new Predicate<String>() {
            private String includePatternString = dataset.getOrDefault("include", null);
            private Pattern includePattern
                    = StringUtils.isBlank(includePatternString)
                    ? null
                    : Pattern.compile(includePatternString);

            @Override
            public boolean test(String s) {
                return includePattern == null || includePattern.matcher(title).matches();
            }
        };

        Predicate<String> excludes = new Predicate<String>() {
            private String excludePatternString = dataset.getOrDefault("exclude", null);
            private Pattern excludePattern
                    = StringUtils.isBlank(excludePatternString)
                    ? null
                    : Pattern.compile(excludePatternString);

            @Override
            public boolean test(String s) {
                return excludePattern != null && excludePattern.matcher(title).matches();
            }
        };


        return includes.and(excludes.negate()).test(title);
    }

    public static Dataset datasetFor(Dataset datasetOrig, SyndEntry entry) {
        return isLikelyIPTEntry(entry)
                ? datasetForIPT(datasetOrig, entry)
                : attemptEasyArthropodCapture(datasetOrig, entry);

    }

    private static Dataset attemptEasyArthropodCapture(Dataset datasetOrig, SyndEntry entry) {
        SyndContent description = entry.getDescription();
        String descriptionString = description == null ? null : description.getValue();
        String title = entry.getTitle();
        String citation = StringUtils.trim(StringUtils.join(Arrays.asList(title, descriptionString), CharsetConstant.SEPARATOR));
        String archiveURI = StringUtils.trim(entry.getLink());
        return embeddedDatasetFor(datasetOrig,
                citation,
                URI.create(archiveURI)
        );
    }

    private static boolean isLikelyIPTEntry(SyndEntry entry) {
        Map<String, String> foreignEntries = parseForeignEntries(entry);
        return foreignEntries.containsKey("eml")
                || foreignEntries.containsKey("dwca");

    }

    private static Dataset datasetForIPT(Dataset datasetOrig, SyndEntry entry) {
        Dataset dataset = null;
        Map<String, String> foreignEntries = parseForeignEntries(entry);
        if (foreignEntries.containsKey("dwca")) {
            dataset = datasetFor(datasetOrig, entry, foreignEntries);
        }
        return dataset;
    }

    private static Dataset datasetFor(Dataset datasetOrig, SyndEntry entry, Map<String, String> foreignEntries) {
        Dataset dataset;
        String title = entry.getTitle();
        String citation = StringUtils.trim(title);
        dataset = embeddedDatasetFor(datasetOrig,
                citation,
                URI.create(foreignEntries.get("dwca")));
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
        String hasDependencies = datasetOrig.getOrDefault("hasDependencies", "false");
        ObjectNode config = new ObjectMapper().createObjectNode();
        config.put("citation", embeddedCitation);
        config.put("format", "application/dwca");
        config.put("url", embeddedArchiveURI.toString());
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
                    enrichProperties(targetProperties, enrichedMap, TaxonUtil.SOURCE_TAXON_NAME, TaxonUtil.TARGET_TAXON_NAME);
                    enrichProperties(targetProperties, enrichedMap, TaxonUtil.SOURCE_TAXON_ID, TaxonUtil.TARGET_TAXON_ID);
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
