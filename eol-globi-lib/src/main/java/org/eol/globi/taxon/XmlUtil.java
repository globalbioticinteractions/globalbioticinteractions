package org.eol.globi.taxon;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.service.PropertyEnricherException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class XmlUtil {
    public static String extractPath(String xmlContent, String elementName, String valuePrefix) throws PropertyEnricherException {
        return extractPath(xmlContent, elementName, valuePrefix, "");
    }

    public static List<String> extractPathNoJoin(String xmlContent, String elementName, String valuePrefix) throws PropertyEnricherException {
        return extractPathNoJoin(xmlContent, elementName, valuePrefix, "");
    }

    public static String extractPath(String xmlContent, String elementName, String valuePrefix, String valueSuffix) throws PropertyEnricherException {
        List<String> ranks = extractPathNoJoin(xmlContent, elementName, valuePrefix, valueSuffix);
        return StringUtils.join(ranks, CharsetConstant.SEPARATOR);
    }

    public static List<String> extractPathNoJoin(String xmlContent, String elementName, String valuePrefix, String valueSuffix) throws PropertyEnricherException {
        List<String> ranks = new ArrayList<String>();
        try {
            InputStream is = IOUtils.toInputStream(xmlContent, "UTF-8");
            String xpathExpr = "//*[local-name() = '" + elementName + "']";
            NodeList nodes = (NodeList) applyXPath(is, xpathExpr, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node item = nodes.item(i);
                Node firstChild = item.getFirstChild();
                if (null != firstChild) {
                    String nodeValue = firstChild.getNodeValue();
                    if (StringUtils.isNotBlank(nodeValue)) {
                        ranks.add(valuePrefix + nodeValue + valueSuffix);
                    }
                }
            }
        } catch (Exception e) {
            throw new PropertyEnricherException("failed to handle response [" + xmlContent + "]", e);
        }
        return ranks;
    }

    public static String extractName(String xmlContent, String elementName) throws PropertyEnricherException {
        try {
            return extractName(IOUtils.toInputStream(xmlContent, "UTF-8"), elementName);
        } catch (Exception e) {
            throw new PropertyEnricherException("failed to handle response [" + xmlContent + "]", e);
        }
    }

    public static String extractName(InputStream is, String elementName) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
        return (String) applyXPath(is, "//*[local-name() = '" + elementName + "']", XPathConstants.STRING);
    }

    public static Object applyXPath(InputStream is, String xpathExpr, QName qname) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().parse(is);
        return applyXPath(doc, xpathExpr, qname);
    }

    public static Object applyXPath(Node node, String xpathExpr, QName qname) throws XPathExpressionException {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        return xpath.compile(xpathExpr).evaluate(node, qname);
    }
}
