package org.globalbioticinteractions.dataset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.ResourceService;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
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

import static org.eol.globi.domain.PropertyAndValueDictionary.MIME_TYPE_DWCA;

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
            String gbifElementValue = StringUtils.trim(getFirstIfPresent(doc, xpath, "//gbif"));

            ObjectNode objectNode = new ObjectMapper().createObjectNode();
            String datasetCitation = Stream.of(name, pubDate, citation)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.joining(". "));

            objectNode.put("citation", datasetCitation + ".");
            if (StringUtils.isNotBlank(collectionName) || StringUtils.isNotBlank(gbifElementValue)) {
                objectNode.put("format", MIME_TYPE_DWCA);
            }

            Node table = getFirstNodeIfPresent(doc, xpath, "//dataTable");
            if (table != null) {
                ObjectNode tableNode = new ObjectMapper().createObjectNode();
                ArrayNode arrayNode = new ObjectMapper().createArrayNode();
                arrayNode.add(tableNode);
                objectNode.set("tables", arrayNode);
                tableNode.put("delimiter", StringUtils.trim(getFirstIfPresent(table, xpath, "//fieldDelimiter")));
                tableNode.put("headerRowCount", StringUtils.trim(getFirstIfPresent(table, xpath, "//numHeaderLines")));
                tableNode.put("url", StringUtils.trim(getFirstIfPresent(table, xpath, "//distribution/online/url")));
                ObjectNode schema = new ObjectMapper().createObjectNode();
                tableNode.set("tableSchema", schema);
                ArrayNode columns = new ObjectMapper().createArrayNode();
                schema.set("columns", columns);
                Node attributeList = getFirstNodeIfPresent(table, xpath, "//attributeList");
                if (attributeList != null) {
                    NodeList childNodes = attributeList.getChildNodes();
                    for (int i=0; i<childNodes.getLength(); i++) {
                        Node attribute = childNodes.item(i);
                        if (StringUtils.equalsAny("attribute", attribute.getNodeName())) {
                            Node id = attribute.getAttributes().getNamedItem("id");
                            ObjectNode column = new ObjectMapper().createObjectNode();
                            columns.add(column);
                            column.put("name", id.getTextContent());
                            column.put("titles", getFirstIfPresent(attribute, xpath, "attributeName"));
                            column.put("datatype", getFirstIfPresent(attribute, xpath, "storageType"));
                            id.getTextContent();
                        }

                    }
                }
            }


            String string = new ObjectMapper().writeValueAsString(objectNode);
            return new ObjectMapper().readTree(string);
        } catch (XPathExpressionException | ParserConfigurationException | IOException | SAXException e) {
            throw new IOException("failed to handle xpath", e);
        }
    }

    private static String getFirstIfPresent(Node doc, XPath xpath, String expression) throws XPathExpressionException {
        Node first = getFirstNodeIfPresent(doc, xpath, expression);
        return first == null ? null : first.getTextContent();
    }

    private static Node getFirstNodeIfPresent(Node doc, XPath xpath, String expression) throws XPathExpressionException {
        XPathExpression expr = xpath.compile(expression);
        NodeList list = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

        Node first = null;
        if (list.getLength() > 0) {
            first = list.item(0);

        }
        return first;
    }
}
