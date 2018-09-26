package org.globalbioticinteractions.dataset;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetProxy;
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
import java.util.Arrays;

public class DwCAUtil {

    public static DatasetProxy datasetWithEML(Dataset origDataset, String emlString) throws IOException {
        DatasetProxy proxy;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(IOUtils.toInputStream(emlString, Charsets.UTF_8));
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();

            String collectionName = getFirstIfPresent(doc, xpath, "//collectionName");
            String pubDate = getFirstIfPresent(doc, xpath, "//pubDate");
            String citation = getFirstIfPresent(doc, xpath, "//citation");

            ObjectNode objectNode = new ObjectMapper().createObjectNode();
            String datasetCitation = StringUtils.join(Arrays.asList(collectionName, pubDate, citation), ". ") + ".";

            objectNode.put("citation", datasetCitation);
            objectNode.put("format", "application/dwca");


            String string = new ObjectMapper().writeValueAsString(objectNode);
            proxy = new DatasetProxy(origDataset);
            proxy.setConfig(new ObjectMapper().readTree(string));



        } catch (XPathExpressionException | ParserConfigurationException | IOException | SAXException e) {
            throw new IOException("failed to handle xpath", e);
        }
        return proxy;
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
