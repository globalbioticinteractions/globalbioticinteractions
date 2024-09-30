package org.globalbioticinteractions.dataset;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.list.TreeList;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.service.DatasetZenodo;
import org.eol.globi.service.ResourceService;
import org.eol.globi.taxon.XmlUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public class DatasetRegistryZenodo implements DatasetRegistry {
    private static final String PREFIX_GITHUB_RELATION = "https://github.com/";
    private static final String PREFIX_ZENODO = "oai:zenodo.org:";

    private static final String ZENODO_ENDPOINT = "https://zenodo.org/";

    private static final String ZENODO_LIST_RECORDS_PREFIX = ZENODO_ENDPOINT + "oai2d?verb=ListRecords";

    private List<String> cachedFeed = null;

    private final ResourceService resourceService;

    private String metadataPrefix = "oai_datacite";


    public DatasetRegistryZenodo(ResourceService resourceService) {
        this.resourceService = resourceService;

    }

    static String parseResumptionToken(InputStream is) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
        String token = (String) XmlUtil.applyXPath(is, "//*[local-name()='resumptionToken']", XPathConstants.STRING);
        return StringUtils.isBlank(token) ? null : token;
    }

    public static URI generateResumptionURI(String resumptionToken) {
        return URI.create(ZENODO_LIST_RECORDS_PREFIX + "&resumptionToken=" + resumptionToken);
    }

    @Override
    public Iterable<String> findNamespaces() throws DatasetRegistryException {
        try {
            Map<String, List<Pair<Long, URI>>> zenodoArchives = findZenodoArchives(getFeedStreams());
            return zenodoArchives == null
                    ? Collections.emptyList()
                    : new TreeList<>(zenodoArchives.keySet());
        } catch (XPathExpressionException | IOException e) {
            throw new DatasetRegistryException("failed to retrieve, or parse, list of Zenodo archives", e);
        }
    }

    @Override
    public void findNamespaces(Consumer<String> namespaceConsumer) throws DatasetRegistryException {
        for (String namespace : findNamespaces()) {
            namespaceConsumer.accept(namespace);
        }
    }


    @Override
    public Dataset datasetFor(String namespace) throws DatasetRegistryException {
        try {
            Map<String, List<Pair<Long, URI>>> zenodoGitHubArchives = findZenodoArchives(getFeedStreams());

            URI latestArchive
                    = getMostRecentArchiveInNamespace(namespace, zenodoGitHubArchives);
            return latestArchive == null
                    ? null
                    : new DatasetZenodo(namespace, resourceService, latestArchive);
        } catch (XPathExpressionException | IOException e) {
            throw new DatasetRegistryException("failed to query archive url for [" + namespace + "]", e);
        }
    }

    static  Map<String, List<Pair<Long, URI>>> findZenodoArchives(List<InputStream> feedStreams) throws DatasetRegistryException, XPathExpressionException, MalformedURLException {

        Map<String, List<Pair<Long, URI>>> zenodoGitHubArchives = new TreeMap<>();
        for (InputStream feedStream : feedStreams) {
            Map<String, List<Pair<Long, URI>>> newArchives = findZenodoGitHubArchives(getRecordNodeList(feedStream));
            for (String namespace : newArchives.keySet()) {
                List<Pair<Long, URI>> existingPairs = zenodoGitHubArchives.containsKey(namespace)
                        ? zenodoGitHubArchives.get(namespace)
                        : new ArrayList<>();

                existingPairs.addAll(newArchives.get(namespace));
                zenodoGitHubArchives.put(namespace, existingPairs);

            }

        }
        return zenodoGitHubArchives;
    }

    private List<InputStream> getFeedStreams() throws DatasetRegistryException {
        initFeedCacheIfNeeded();

        List<InputStream> cachedStreams = new ArrayList<>();
        List<String> cachedFeed = getCachedFeed();
        for (String s : cachedFeed) {
            try {
                cachedStreams.add(IOUtils.toInputStream(s, StandardCharsets.UTF_8.name()));
            } catch (IOException e) {
                throw new DatasetRegistryException("failed to access cached strings", e);
            }

        }
        return cachedStreams;
    }

    private void initFeedCacheIfNeeded() throws DatasetRegistryException {
        if (getCachedFeed() == null) {
            String resumptionToken = null;
            List<String> cachedPages = getCachedPages(resumptionToken, resourceService, getMetadataPrefix());

            setCachedFeed(cachedPages);
        }
    }

    static List<String> getCachedPages(String resumptionToken, ResourceService resourceService, String metadataPrefix) throws DatasetRegistryException {
        List<String> cachedPages = new ArrayList<>();
        do {
            String nextPage = getNextPage(resumptionToken, resourceService, metadataPrefix);
            try {
                resumptionToken = parseResumptionToken(IOUtils.toInputStream(nextPage, StandardCharsets.UTF_8));
            } catch (SAXException | IOException | ParserConfigurationException | XPathExpressionException e) {
                throw new DatasetRegistryException("failed to parse Zenodo registry results");
            }
            cachedPages.add(nextPage);
        } while (StringUtils.isNoneBlank(resumptionToken));
        return cachedPages;
    }

    public String getMetadataPrefix() {
        return metadataPrefix;
    }

    public void setMetadataPrefix(String metadataPrefix) {
        this.metadataPrefix = metadataPrefix;
    }



    static URI findLatestZenodoGitHubArchiveForNamespace(NodeList records, String namespace) throws XPathExpressionException, MalformedURLException {
        Map<String, List<Pair<Long, URI>>> archives = findZenodoGitHubArchives(records);
        return getMostRecentArchiveInNamespace(namespace, archives);
    }

    static URI getMostRecentArchiveInNamespace(String namespace, Map<String, List<Pair<Long, URI>>> archives) {
        URI latestArchiveURI = null;
        if (archives != null) {
            List<Pair<Long, URI>> uris = archives.get(namespace);
            if (uris != null) {
                TreeMap<Long, URI> sortedMap = new TreeMap<>();
                for (Pair<Long, URI> longURIPair : uris) {
                    sortedMap.put(longURIPair.getKey(), longURIPair.getValue());
                }
                latestArchiveURI = sortedMap.size() == 0
                        ? null
                        : sortedMap.lastEntry().getValue();
            }
        }
        return latestArchiveURI;
    }

    static Map<String, List<Pair<Long, URI>>> findZenodoGitHubArchives(NodeList records) throws XPathExpressionException {
        Map<String, List<Pair<Long, URI>>> namespace2ZenodoPubs = new TreeMap<>();
        Long idMax = null;
        for (int i = 0; i < records.getLength(); i++) {
            Node item = records.item(i);
            String fullId = (String) XmlUtil.applyXPath(item, "header/identifier", XPathConstants.STRING);
            if (StringUtils.startsWith(fullId, PREFIX_ZENODO)) {
                String idString = StringUtils.replace(fullId, PREFIX_ZENODO, "");
                String relatedIdentifier = StringUtils.trim((String) XmlUtil.applyXPath(item, ".//*[local-name()='relatedIdentifier']", XPathConstants.STRING));
                if (StringUtils.startsWith(relatedIdentifier, PREFIX_GITHUB_RELATION)) {
                    String replace = StringUtils.replace(StringUtils.trim(relatedIdentifier), PREFIX_GITHUB_RELATION, "");
                    String[] split = StringUtils.split(replace, "/");
                    Long id = NumberUtils.createLong(idString);
                    if (split.length > 3) {
                        String githubRepo = split[0] + "/" + split[1];
                        URI archiveURI = URI.create("https://zenodo.org/records/" + idString + "/files/" + githubRepo + "-" + split[3] + ".zip");
                        List<Pair<Long, URI>> versions = namespace2ZenodoPubs
                                .getOrDefault(githubRepo, new TreeList<>());
                        namespace2ZenodoPubs.put(githubRepo, new TreeList<Pair<Long, URI>>(versions) {{
                            add(Pair.of(id, archiveURI));
                        }});

                    }
                }
            }
        }
        return namespace2ZenodoPubs;
    }

    static Collection<String> findPublishedGitHubRepos(Collection<String> refs) {
        Collection<String> refs2 = new HashSet<>();
        for (String nodeValue : refs) {
            String replace = StringUtils.replace(nodeValue, PREFIX_GITHUB_RELATION, "");
            String[] split = StringUtils.split(replace, "/");
            if (split.length > 1) {
                refs2.add(split[0] + "/" + split[1]);
            }
        }

        return refs2;
    }

    private static Collection<String> getRelations(NodeList nodes) {
        Collection<String> refs = new HashSet<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node item = nodes.item(i);
            if (item.hasChildNodes()) {
                String nodeValue = StringUtils.trim(item.getFirstChild().getNodeValue());
                if (StringUtils.startsWith(nodeValue, PREFIX_GITHUB_RELATION)) {
                    refs.add(nodeValue);
                }
            }
        }
        return refs;
    }

    static NodeList getRecordNodeList(InputStream is) throws DatasetRegistryException {
        try {
            return (NodeList) XmlUtil.applyXPath(is, "//*[local-name()='record']", XPathConstants.NODESET);
        } catch (SAXException | IOException | ParserConfigurationException | XPathExpressionException e) {
            throw new DatasetRegistryException("failed to find published github repos in zenodo", e);
        }
    }

    private static NodeList getRelationsNodeList(InputStream is) throws DatasetRegistryException {
        try {
            return (NodeList) XmlUtil.applyXPath(is, "//*[local-name()='relatedIdentifier']", XPathConstants.NODESET);
        } catch (SAXException | IOException | ParserConfigurationException | XPathExpressionException e) {
            throw new DatasetRegistryException("failed to find published github repos in zenodo", e);
        }
    }

    static String getNextPage(String resumptionToken, ResourceService resourceService, String metadataPrefix) throws DatasetRegistryException {
        try {
            URI requestURI = URI.create(ZENODO_LIST_RECORDS_PREFIX + "&set=user-globalbioticinteractions&metadataPrefix=" + metadataPrefix);
            if (StringUtils.isNoneBlank(resumptionToken)) {
                requestURI = generateResumptionURI(resumptionToken);
            }
            try (InputStream retrieve = resourceService.retrieve(requestURI)) {
                return IOUtils.toString(retrieve, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new DatasetRegistryException("failed to find published github repos in zenodo", e);
        }
    }


    private Collection<String> find(InputStream xmlFeed) throws DatasetRegistryException {
        return findPublishedGitHubRepos(getRelations(xmlFeed));
    }

    static Collection<String> getRelations(InputStream is) throws DatasetRegistryException {
        return getRelations(getRelationsNodeList(is));
    }


    private void setCachedFeed(List<String> cachedFeed) {
        this.cachedFeed = cachedFeed;
    }

    private List<String> getCachedFeed() {
        return this.cachedFeed;
    }

}
