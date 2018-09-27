package org.globalbioticinteractions.dataset;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetProxy;
import org.gbif.dwc.Archive;
import org.gbif.dwc.DwcFiles;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class DwCAUtil {

    public static Archive archiveFor(URI archiveURI, String tmpDir) throws IOException {
        Archive archive;
        Path myArchiveFile = Paths.get(archiveURI);
        if (myArchiveFile.toFile().isFile()) {
            if (StringUtils.isBlank(tmpDir)) {
                throw new IllegalArgumentException("cannot read [" + archiveURI + "] without a tmpDir");
            }
            Path extractToFolder = Paths.get(tmpDir);
            archive = DwcFiles.fromCompressed(myArchiveFile, extractToFolder);
        } else {
            archive = DwcFiles.fromLocation(myArchiveFile);
        }
        return archive;
    }
}
