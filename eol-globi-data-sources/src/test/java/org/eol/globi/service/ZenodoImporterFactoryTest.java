package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.taxon.XmlUtil;
import org.eol.globi.util.HttpUtil;
import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class ZenodoImporterFactoryTest {

    private static final String PREFIX_GITHUB_RELATION = "https://github.com/";
    private static final String PREFIX_ZENODO = "oai:zenodo.org:";

    @Test
    public void zenodoDataFeed() throws IOException {
        String feed = HttpUtil.getContent("https://zenodo.org/oai2d?verb=ListRecords&set=user-globalbioticinteractions&metadataPrefix=oai_datacite3");
        assertThat(feed, containsString("<?xml version"));
    }

    @Test
    public void extractGitHubRepos() throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
        Collection<String> refs = findPublishedGitHubRepos(getClass().getResourceAsStream("zenodo-oai-request-results.xml"));
        assertThat(refs.size(), is(1));
        assertThat(refs, hasItem("globalbioticinteractions/template-dataset"));
    }

    @Test
    public void extractGitHubReposArchives() throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
        NodeList nodes = (NodeList) XmlUtil.applyXPath(getClass().getResourceAsStream("zenodo-oai-request-results.xml"), "//record", XPathConstants.NODESET);
        Collection<String> refs1 = findZenodoGitHubArchives(nodes);
        assertThat(refs1.size(), is(1));
        assertThat(refs1, hasItem("https://zenodo.org/record/207958/files/globalbioticinteractions/template-dataset-0.0.2.zip"));
    }

    private static Collection<String> findZenodoGitHubArchives(NodeList nodes) throws XPathExpressionException {
        Collection<String> refs1 = new HashSet<>();
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
                        refs1.add("https://zenodo.org/record/" + id + "/files/" + split[0] + "/" + split[1] + "-" + split[3] + ".zip");
                    }
                }
            }
        }
        return refs1;
    }

    private static Collection<String> findPublishedGitHubRepos(InputStream is) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
        Collection<String> refs = getRelations(is);

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

    private static Collection<String> getRelations(InputStream is) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
        NodeList nodes = (NodeList) XmlUtil.applyXPath(is, "//*[local-name()='relatedIdentifier']", XPathConstants.NODESET);
        Collection<String> refs = new HashSet<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node item = nodes.item(i);
            if (item.hasChildNodes()) {
                String nodeValue = item.getFirstChild().getNodeValue();
                if (StringUtils.startsWith(nodeValue, PREFIX_GITHUB_RELATION)) {
                    refs.add(nodeValue);
                }
            }
        }
        return refs;
    }


}