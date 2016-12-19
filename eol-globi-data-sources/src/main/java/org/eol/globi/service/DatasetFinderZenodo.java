package org.eol.globi.service;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.taxon.XmlUtil;
import org.eol.globi.util.HttpUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;

public class DatasetFinderZenodo implements DatasetFinder {
    private static final String PREFIX_GITHUB_RELATION = "https://github.com/";
    private static final String PREFIX_ZENODO = "oai:zenodo.org:";

    static URL findZenodoGitHubArchives(NodeList nodes, String requestedRepo) throws XPathExpressionException, MalformedURLException {
        URL archiveUrl = null;
        for (int i = 0; i < nodes.getLength(); i++) {
            Node item = nodes.item(i);
            String fullId = (String) XmlUtil.applyXPath(item, "//header/identifier", XPathConstants.STRING);
            if (StringUtils.startsWith(fullId, PREFIX_ZENODO)) {
                String id = StringUtils.replace(fullId, PREFIX_ZENODO, "");
                String relatedIdentifier = StringUtils.trim((String) XmlUtil.applyXPath(item, "//*[local-name()='relatedIdentifier']", XPathConstants.STRING));
                if (StringUtils.startsWith(relatedIdentifier, PREFIX_GITHUB_RELATION)) {
                    String replace = StringUtils.replace(StringUtils.trim(relatedIdentifier), PREFIX_GITHUB_RELATION, "");
                    String[] split = StringUtils.split(replace, "/");
                    if (split.length > 3) {
                        String githubRepo = split[0] + "/" + split[1];
                        if (StringUtils.equals(githubRepo, requestedRepo)) {
                            archiveUrl = new URL("https://zenodo.org/record/" + id + "/files/" + githubRepo + "-" + split[3] + ".zip");
                        }
                    }
                }
            }
        }
        return archiveUrl;
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

    private static NodeList getRelationsNodeList(InputStream is) throws DatasetFinderException {
        try {
            return (NodeList) XmlUtil.applyXPath(is, "//*[local-name()='relatedIdentifier']", XPathConstants.NODESET);
        } catch (SAXException | IOException | ParserConfigurationException | XPathExpressionException e) {
            throw new DatasetFinderException("failed to find published github repos in zenodo", e);
        }
    }

    static InputStream getFeed() throws DatasetFinderException {
        try {
            String feedString = HttpUtil.getContent("https://zenodo.org/oai2d?verb=ListRecords&set=user-globalbioticinteractions&metadataPrefix=oai_datacite3");
            return IOUtils.toInputStream(feedString);
        } catch (IOException e) {
            throw new DatasetFinderException("failed to find published github repos in zenodo", e);
        }
    }

    @Override
    public Collection<String> find() throws DatasetFinderException {
        return find(getFeed());
    }

    public Collection<String> find(InputStream xmlFeed) throws DatasetFinderException {
        return findPublishedGitHubRepos(getRelations(xmlFeed));
    }

    static Collection<String> getRelations(InputStream is) throws DatasetFinderException {
        return getRelations(getRelationsNodeList(is));
    }

    @Override
    public URL archiveUrlFor(String repo) throws DatasetFinderException {
        try {
            return findZenodoGitHubArchives(getRelationsNodeList(getFeed()), repo);
        } catch (XPathExpressionException | MalformedURLException e) {
            throw new DatasetFinderException("failed to resolve archive url for [" + repo + "]", e);
        }
    }

}
