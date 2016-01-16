package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.util.CSVUtil;
import org.eol.globi.util.ResourceUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class StudyImporterForBascompte extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForBascompte.class);
    public static final String WEB_OF_LIFE_BASE_URL = "http://www.web-of-life.es";

    public StudyImporterForBascompte(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        final String archiveURL;
        try {
            archiveURL = generateArchiveURL(getNetworkNames());
        } catch (IOException e) {
            throw new StudyImporterException("failed to retrieve network names", e);
        }

        return importNetworks(archiveURL);
    }

    public Study importNetworks(String archiveURL) throws StudyImporterException {
        try {
            InputStream inputStream = ResourceUtil.asInputStream(archiveURL, StudyImporterForBascompte.class);
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            ZipEntry entry;
            File referencesTempFile = null;
            Map<String, File> networkTempFileMap = new HashMap<String, File>();
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().matches("(^|(.*/))references\\.csv$")) {
                    referencesTempFile = FileUtils.saveToTmpFile(zipInputStream, entry);
                } else if (entry.getName().matches(".*\\.csv$")) {
                    networkTempFileMap.put(entry.getName().replace(".csv", ""), FileUtils.saveToTmpFile(zipInputStream, entry));
                } else {
                    IOUtils.copy(zipInputStream, new NullOutputStream());
                }
            }
            IOUtils.closeQuietly(zipInputStream);

            if (referencesTempFile == null) {
                throw new StudyImporterException("failed to find expected [references.csv] resource");
            }

            if (networkTempFileMap.size() == 0) {
                throw new StudyImporterException("failed to find expected network csv files");
            }

            final String sourceCitation = "Web of Life. " + ReferenceUtil.createLastAccessedString("http://www.web-of-life.es/");

            BufferedReader assocReader = FileUtils.getUncompressedBufferedReader(new FileInputStream(referencesTempFile), CharsetConstant.UTF8);
            LabeledCSVParser parser = CSVUtil.createLabeledCSVParser(assocReader);
            while (parser.getLine() != null) {
                final String citation = parser.getValueByLabel("Reference");
                if (StringUtils.isBlank(citation)) {
                    throw new StudyImporterException("found missing reference");
                }
                final String networkId = parser.getValueByLabel("ID");
                if (!networkTempFileMap.containsKey(networkId)) {
                    throw new StudyImporterException("found network id [" + networkId + "], but no associated data.");
                }
                final Study study = nodeFactory.getOrCreateStudy("bascompte:" + networkId, sourceCitation, citation);
                importNetwork(parseInteractionType(parser),
                        parseLocation(parser), study, networkTempFileMap.get(networkId));
            }
        } catch (IOException e) {
            throw new StudyImporterException(e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException(e);
        }
        return null;
    }

    public InteractType parseInteractionType(LabeledCSVParser parser) throws StudyImporterException {
        final String interactionTypeString = parser.getValueByLabel("Type of interactions");
        final Map<String, InteractType> interactionTypeMap = new HashMap<String, InteractType>() {
            {
                put("Pollination", InteractType.POLLINATED_BY);
                put("Seed Dispersal", InteractType.HAS_DISPERAL_VECTOR);
                put("Host-Parasite", InteractType.HAS_PARASITE);
            }
        };
        final InteractType interactType1 = interactionTypeMap.get(interactionTypeString);
        if (interactType1 == null) {
            LOG.warn("found unsupported interaction type [" + interactionTypeString + "]");
        }
        return interactType1 == null ? InteractType.INTERACTS_WITH : interactType1;
    }

    public Location parseLocation(LabeledCSVParser parser) throws StudyImporterException {
        Location networkLocation = null;
        final String latitude = parser.getValueByLabel("Latitude");
        final String longitude = parser.getValueByLabel("Longitude");
        if (StringUtils.isNotBlank(latitude) && StringUtils.isNotBlank(longitude)) {
            try {
                final double lat = Double.parseDouble(latitude);
                final double lng = Double.parseDouble(longitude);
                networkLocation = nodeFactory.getOrCreateLocation(lat, lng, null);
            } catch (NumberFormatException ex) {
                throw new StudyImporterException("found invalid lat,lng pair: [" + latitude + "], [" + longitude + "] on line [" + parser.lastLineNumber() + "] in [references.csv]");
            }
        }
        return networkLocation;
    }

    public void importNetwork(InteractType interactType1, Location networkLocation, Study study, File file) throws IOException, NodeFactoryException {
        LabeledCSVParser interactions = CSVUtil.createLabeledCSVParser(new FileInputStream(file));
        final String[] targetLabels = interactions.getLabels();
        List<String> targetTaxonNames = new ArrayList<String>();
        List<String> ignoredLabels = Arrays.asList("number of hosts sampled", "");
        for (String targetLabel : targetLabels) {
            String trimmedLabel = StringUtils.trim(targetLabel);
            if (StringUtils.isBlank(targetLabel) || ignoredLabels.contains(trimmedLabel)) {
                targetTaxonNames.add(trimmedLabel);
            }
        }

        String[] line;
        while ((line = interactions.getLine()) != null) {
            String sourceTaxonName = line[0];
            final Specimen sourceSpecimen = nodeFactory.createSpecimen(study, sourceTaxonName);
            sourceSpecimen.caughtIn(networkLocation);
            for (String targetTaxonName : targetTaxonNames) {
                final Specimen targetSpecimen = nodeFactory.createSpecimen(study, targetTaxonName);
                targetSpecimen.caughtIn(networkLocation);
                sourceSpecimen.interactsWith(targetSpecimen, interactType1);
            }
        }
    }

    public static List<String> getNetworkNames() throws IOException {
        final InputStream networkList = ResourceUtil.asInputStream(WEB_OF_LIFE_BASE_URL + "/networkslist.php?type=All&data=All", null);
        final JsonNode networks = new ObjectMapper().readTree(networkList);
        final List<String> networkNames = new ArrayList<String>();
        for (JsonNode network : networks) {
            final JsonNode networkName = network.get("networkName");
            if (networkName != null && networkName.isTextual()) {
                networkNames.add(networkName.asText());
            }
        }
        return networkNames;
    }

    public static String generateArchiveURL(List<String> networkNames) {
        final String prefix = WEB_OF_LIFE_BASE_URL + "/map_download_fast2.php?format=csv&networks=";
        final String suffix = "&species=yes&type=All&data=All&speciesrange=&interactionsrange=&searchbox=&checked=false";
        final String joinedNames = StringUtils.join(networkNames, ",");
        return StringUtils.join(Arrays.asList(prefix, joinedNames, suffix), "");
    }

}
