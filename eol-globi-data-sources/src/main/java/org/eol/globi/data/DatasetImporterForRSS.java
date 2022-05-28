package org.eol.globi.data;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.util.DatasetImportUtil;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetProxy;
import org.globalbioticinteractions.dataset.DatasetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DatasetImporterForRSS extends NodeBasedImporter {
    private static final Logger LOG = LoggerFactory.getLogger(DatasetImporterForRSS.class);
    public static final String HAS_DEPENDENCIES = "hasDependencies";

    public DatasetImporterForRSS(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        final List<Dataset> datasets = getDatasets(getDataset());
        List<Dataset> dependencies = getDependencies(getDataset());
        DatasetImportUtil.resolveAndImportDatasets(
                dependencies,
                datasets,
                getLogger(),
                getNodeFactory(),
                getRssFeedUrlString());
    }

    public static List<Dataset> getDatasets(Dataset dataset) throws StudyImporterException {
        return getDatasets(
                dataset,
                x -> StringUtils.equals("false", x.getOrDefault("isDependency", "false")));
    }

    public static List<Dataset> getDatasets(Dataset dataset, Predicate<Dataset> datasetPredicate) throws StudyImporterException {
        List<Dataset> datasetsForFeed = getDatasetsForFeed(dataset);
        return datasetsForFeed
                .stream()
                .filter(datasetPredicate)
                .collect(Collectors.toList());
    }

    public static List<Dataset> getDependencies(Dataset dataset) throws StudyImporterException {
        return getDatasets(dataset, x -> true);
    }

    public String getRssFeedUrlString() {
        return getRSSEndpoint(getDataset());
    }

    static String getRSSEndpoint(Dataset dataset) {
        return DatasetUtil.getValueOrDefault(
                dataset.getConfig(),
                "url", null);
    }


    static List<Dataset> getDatasetsForFeed(Dataset datasetOrig) throws StudyImporterException {
        SyndFeed feed = parseFeed(datasetOrig);
        return extractDatasets(datasetOrig, feed);
    }

    private static List<Dataset> extractDatasets(Dataset datasetOrig, SyndFeed feed) {
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
                } else {
                    LOG.info("skipping [" + title + "] : was not included or excluded.");
                }
            }
        }
        return datasets;
    }

    private static SyndFeed parseFeed(Dataset datasetOrig) throws StudyImporterException {
        SyndFeed feed;
        String rss = getRSSEndpoint(datasetOrig);
        try {
            URI rssURI = URI.create(StringUtils.isBlank(rss) ? "rss" : rss);
            feed = new SyndFeedInput().build(new XmlReader(datasetOrig.retrieve(rssURI)));
        } catch (FeedException | IOException | IllegalArgumentException e) {
            throw new StudyImporterException("failed to read rss feed [" + rss + "]", e);
        }
        return feed;
    }

    static boolean shouldIncludeTitleInDatasetCollection(String title, Dataset dataset) {
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
                URI.create(archiveURI),
                parseForeignEntriesAndCategories(entry)
        );
    }

    private static boolean isLikelyIPTEntry(SyndEntry entry) {
        Map<String, String> foreignEntries = parseForeignEntriesAndCategories(entry);
        return foreignEntries.containsKey("eml")
                || foreignEntries.containsKey("dwca");

    }

    private static Dataset datasetForIPT(Dataset datasetOrig, SyndEntry entry) {
        Dataset dataset = null;
        Map<String, String> foreignEntries = parseForeignEntriesAndCategories(entry);
        if (foreignEntries.containsKey("dwca")) {
            dataset = datasetFor(datasetOrig, entry, foreignEntries);
        }
        return dataset;
    }

    private static Dataset datasetFor(Dataset datasetOrig, SyndEntry entry, Map<String, String> foreignEntries) {
        Dataset dataset;
        String title = entry.getTitle();
        String citation = StringUtils.trim(title);
        dataset = embeddedDatasetFor(
                datasetOrig,
                citation,
                URI.create(foreignEntries.get("dwca")),
                parseForeignEntriesAndCategories(entry));
        return dataset;
    }

    private static Map<String, String> parseForeignEntriesAndCategories(SyndEntry entry) {
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

        List categories = entry.getCategories();
        for (Object category : categories) {
            if (category instanceof SyndCategory) {
                SyndCategory cat = (SyndCategory) category;
                if (StringUtils.equals(cat.getTaxonomyUri(), "http://www.w3.org/ns/prov")
                    && StringUtils.equals(cat.getName(), "http://www.w3.org/ns/prov#wasUsedBy")) {
                    foreignEntries.put("isDependency", "true");
                } else if (StringUtils.equals(cat.getTaxonomyUri(), "http://purl.org/dc/terms/MediaType")) {
                    foreignEntries.put("format", StringUtils.replace(cat.getName(), "application/globi+", ""));
                }
            }
        }
        return foreignEntries;
    }


    static Dataset embeddedDatasetFor(final Dataset datasetOrig,
                                      final String embeddedCitation,
                                      final URI embeddedArchiveURI,
                                      final Map<String, String> properties) {
        String hasDependencies = datasetOrig.getOrDefault(HAS_DEPENDENCIES, "false");
        ObjectNode config = new ObjectMapper().createObjectNode();
        config.put("citation", embeddedCitation);
        config.put("format", properties.getOrDefault("format", PropertyAndValueDictionary.MIME_TYPE_DWCA));
        config.put("url", embeddedArchiveURI.toString());
        config.put(HAS_DEPENDENCIES, hasDependencies);
        config.put("isDependency", properties.getOrDefault("isDependency", "false"));

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
