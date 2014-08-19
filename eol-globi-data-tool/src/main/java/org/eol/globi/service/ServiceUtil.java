package org.eol.globi.service;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.List;

public class ServiceUtil {
    public static String extractPath(String xmlContent, String elementName) throws TaxonPropertyLookupServiceException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc;
        try {
            doc = factory.newDocumentBuilder().parse(IOUtils.toInputStream(xmlContent, "UTF-8"));
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            Object result = xpath.compile("//*[local-name() = '" + elementName + "']").evaluate(doc, XPathConstants.NODESET);
            List<String> ranks = new ArrayList<String>();
            NodeList nodes = (NodeList) result;
            for (int i = 0; i < nodes.getLength(); i++) {
                Node item = nodes.item(i);
                Node firstChild = item.getFirstChild();
                if (null != firstChild) {
                    String nodeValue = firstChild.getNodeValue();
                    if (StringUtils.isNotBlank(nodeValue)) {
                        ranks.add(nodeValue);
                    }
                }
            }
            return StringUtils.join(ranks, CharsetConstant.SEPARATOR);
        } catch (Exception e) {
            throw new TaxonPropertyLookupServiceException("failed to handle response [" + xmlContent + "]", e);
        }
    }
}
