package org.globalbioticinteractions.dataset;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.eol.globi.service.DatasetZenodo;
import org.eol.globi.service.ResponseHandlerWithInputStreamFactory;
import org.eol.globi.taxon.XmlUtil;
import org.eol.globi.util.HttpUtil;
import org.eol.globi.util.InputStreamFactory;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetFinderException;
import org.globalbioticinteractions.dataset.DatasetRegistry;
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
import java.util.Collection;
import java.util.HashSet;

public class DatasetRegistryZenodo implements DatasetRegistry {
    private static final String PREFIX_GITHUB_RELATION = "https://github.com/";
    private static final String PREFIX_ZENODO = "oai:zenodo.org:";
    private String cachedFeed = null;

    private final InputStreamFactory inputStreamFactory;

    public DatasetRegistryZenodo(InputStreamFactory inputStreamFactory) {
        this.inputStreamFactory = inputStreamFactory;
    }

    @Override
    public Collection<String> findNamespaces() throws DatasetFinderException {
        return find(getFeedStream());
    }

    @Override
    public Dataset datasetFor(String namespace) throws DatasetFinderException {
        try {
            InputStream feedStream = getFeedStream();
            URI zenodoGitHubArchives = findZenodoGitHubArchives(getRecordNodeList(feedStream), namespace);
            return zenodoGitHubArchives == null ? null : new DatasetZenodo(namespace, zenodoGitHubArchives, getInputStreamFactory());
        } catch (XPathExpressionException | IOException e) {
            throw new DatasetFinderException("failed to resolve archive url for [" + namespace + "]", e);
        }
    }

    private InputStream getFeedStream() throws DatasetFinderException {
        initFeedCacheIfNeeded();

        try {
            return IOUtils.toInputStream(getCachedFeed(), StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new DatasetFinderException("failed to get Zenodo registry feed", e);
        }
    }

    public void initFeedCacheIfNeeded() throws DatasetFinderException {
        if (StringUtils.isBlank(getCachedFeed())) {
            setCachedFeed(getFeed(getInputStreamFactory()));
        }
    }

    public InputStreamFactory getInputStreamFactory() {
        return inputStreamFactory;
    }

    static URI findZenodoGitHubArchives(NodeList records, String requestedRepo) throws XPathExpressionException, MalformedURLException {
        URI archiveURI = null;
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
                    if (split.length > 3 && (idMax == null || id > idMax)) {
                        String githubRepo = split[0] + "/" + split[1];
                        if (StringUtils.equals(githubRepo, requestedRepo)) {
                            archiveURI = URI.create("https://zenodo.org/record/" + idString + "/files/" + githubRepo + "-" + split[3] + ".zip");
                            idMax = id;
                        }
                    }
                }
            }
        }
        return archiveURI;
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

    static NodeList getRecordNodeList(InputStream is) throws DatasetFinderException {
        try {
            return (NodeList) XmlUtil.applyXPath(is, "//*[local-name()='record']", XPathConstants.NODESET);
        } catch (SAXException | IOException | ParserConfigurationException | XPathExpressionException e) {
            throw new DatasetFinderException("failed to find published github repos in zenodo", e);
        }
    }


    static NodeList getRelationsNodeList(InputStream is) throws DatasetFinderException {
        try {
            return (NodeList) XmlUtil.applyXPath(is, "//*[local-name()='relatedIdentifier']", XPathConstants.NODESET);
        } catch (SAXException | IOException | ParserConfigurationException | XPathExpressionException e) {
            throw new DatasetFinderException("failed to find published github repos in zenodo", e);
        }
    }

    static String getFeed() throws DatasetFinderException {
        return getFeed(inStream -> inStream);
    }

    static String getFeed(InputStreamFactory factory) throws DatasetFinderException {
        try {
            HttpClient httpClient = HttpUtil.getHttpClient();
            String requestUrl = "https://zenodo.org/oai2d?verb=ListRecords&set=user-globalbioticinteractions&metadataPrefix=oai_datacite3";

            return HttpUtil.executeAndRelease(
                    new HttpGet(requestUrl),
                    httpClient,
                    new ResponseHandlerWithInputStreamFactory(factory));
        } catch (IOException e) {
            throw new DatasetFinderException("failed to find published github repos in zenodo", e);
        }
    }


    private Collection<String> find(InputStream xmlFeed) throws DatasetFinderException {
        return findPublishedGitHubRepos(getRelations(xmlFeed));
    }

    static Collection<String> getRelations(InputStream is) throws DatasetFinderException {
        return getRelations(getRelationsNodeList(is));
    }


    public void setCachedFeed(String cachedFeed) {
        this.cachedFeed = cachedFeed;
    }

    public String getCachedFeed() {
        return this.cachedFeed;
    }

}
