package org.globalbioticinteractions.dataset;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.eol.globi.service.ResourceService;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.net.URI;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EMLUtil {

    public static JsonNode datasetWithEML(ResourceService origDataset, URI emlURI) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(origDataset.retrieve(emlURI));
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();

            String collectionName = StringUtils.trim(getFirstIfPresent(doc, xpath, "//collectionName"));
            String datasetTitle = StringUtils.trim(getFirstIfPresent(doc, xpath, "//dataset/title"));
            String name = StringUtils.isBlank(collectionName) ? datasetTitle : collectionName;

            String pubDate = StringUtils.trim(getFirstIfPresent(doc, xpath, "//pubDate"));
            String citation = StringUtils.trim(getFirstIfPresent(doc, xpath, "//citation"));

            ObjectNode objectNode = new ObjectMapper().createObjectNode();
            String datasetCitation = Stream.of(name, pubDate,citation)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.joining(". "));

            objectNode.put("citation", datasetCitation + ".");
            objectNode.put("format", "application/dwca");


            String string = new ObjectMapper().writeValueAsString(objectNode);
            return new ObjectMapper().readTree(string);
        } catch (XPathExpressionException | ParserConfigurationException | IOException | SAXException e) {
            throw new IOException("failed to handle xpath", e);
        }
    }

    private static String getFirstIfPresent(Document doc, XPath xpath, String expression) throws XPathExpressionException {
        XPathExpression expr = xpath.compile(expression);
        NodeList list = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

        String collectionName = null;
        if (list.getLength() > 0) {
            collectionName = list.item(0).getTextContent();
        }
        return collectionName;
    }
}
