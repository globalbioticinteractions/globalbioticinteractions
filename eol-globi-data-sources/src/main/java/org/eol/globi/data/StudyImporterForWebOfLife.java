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
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.util.CSVTSVUtil;
import org.globalbioticinteractions.dataset.CitationUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class StudyImporterForWebOfLife extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForWebOfLife.class);
    public static final String WEB_OF_LIFE_BASE_URL = "http://www.web-of-life.es";

    public StudyImporterForWebOfLife(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        try {
            List<StudyImporterException> errors = new ArrayList<StudyImporterException>();
            final String sourceCitation = "Web of Life. " + CitationUtil.createLastAccessedString("http://www.web-of-life.es/");
            final List<String> networkNames = getNetworkNames(getDataset().getResource(WEB_OF_LIFE_BASE_URL + "/networkslist.php?type=All&data=All"));
            LOG.info("found [" + networkNames.size() + "] networks.");
            for (String networkName : networkNames) {
                final List<String> networkNames1 = Collections.singletonList(networkName);
                try {
                    importNetworks(generateArchiveURL(networkNames1), sourceCitation);
                } catch (StudyImporterException e) {
                    errors.add(new StudyImporterException("networks [" + networkNames1 + "] import failed.", e));
                    LOG.error("networks [" + networkNames1 + "] import failed.", e);
                }
            }
            if (!errors.isEmpty()) {
                throw new StudyImporterException("found at least one import exception", errors.get(0));
            }

        } catch (IOException e) {
            throw new StudyImporterException("failed to retrieve network names or import networks", e);
        }
    }

    public void importNetworks(String archiveURL, String sourceCitation) throws StudyImporterException {
        try {
            InputStream inputStream = getDataset().getResource(archiveURL);
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
                throw new StudyImporterException("failed to find expected [references.csv] resource in [" + archiveURL + "]");
            }

            if (networkTempFileMap.size() == 0) {
                throw new StudyImporterException("failed to find expected network csv files");
            }

            BufferedReader assocReader = FileUtils.getUncompressedBufferedReader(new FileInputStream(referencesTempFile), CharsetConstant.UTF8);
            LabeledCSVParser parser = CSVTSVUtil.createLabeledCSVParser(assocReader);
            while (parser.getLine() != null) {
                final String citation = parser.getValueByLabel("Reference");
                if (StringUtils.isBlank(citation)) {
                    throw new StudyImporterException("found missing reference");
                }
                final String networkId = parser.getValueByLabel("ID");
                if (!networkTempFileMap.containsKey(networkId)) {
                    throw new StudyImporterException("found network id [" + networkId + "], but no associated data.");
                }
                final Study study = nodeFactory.getOrCreateStudy(new StudyImpl("bascompte:" + citation, sourceCitation, null, citation));
                importNetwork(parseInteractionType(parser),
                        parseLocation(parser), study, networkTempFileMap.get(networkId));
            }
        } catch (IOException e) {
            throw new StudyImporterException(e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException(e);
        }
    }

    public InteractType parseInteractionType(LabeledCSVParser parser) throws StudyImporterException {
        final String interactionTypeString = parser.getValueByLabel("Type of interactions");
        final Map<String, InteractType> interactionTypeMap = new HashMap<String, InteractType>() {
            {
                put("Pollination", InteractType.POLLINATED_BY);
                put("Seed Dispersal", InteractType.HAS_DISPERAL_VECTOR);
                put("Host-Parasite", InteractType.HAS_PARASITE);
                put("Plant-Herbivore", InteractType.EATEN_BY);
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
                networkLocation = nodeFactory.getOrCreateLocation(new LocationImpl(lat, lng, null, null));
            } catch (NumberFormatException ex) {
                throw new StudyImporterException("found invalid lat,lng pair: [" + latitude + "], [" + longitude + "] on line [" + parser.lastLineNumber() + "] in [references.csv]");
            }
        }
        return networkLocation;
    }

    public void importNetwork(InteractType interactType1, Location networkLocation, Study study, File file) throws IOException, NodeFactoryException {
        LabeledCSVParser interactions = CSVTSVUtil.createLabeledCSVParser(new FileInputStream(file));
        final String[] targetLabels = interactions.getLabels();
        List<String> targetTaxonNames = new ArrayList<String>();
        List<String> ignoredLabels = Arrays.asList("number of hosts sampled", "");
        for (String targetLabel : targetLabels) {
            String trimmedLabel = StringUtils.trim(targetLabel);
            if (StringUtils.isNotBlank(targetLabel) || !ignoredLabels.contains(trimmedLabel)) {
                targetTaxonNames.add(targetLabel);
            }
        }

        String[] line;
        while ((line = interactions.getLine()) != null) {
            String sourceTaxonName = line[0];
            final Specimen sourceSpecimen = nodeFactory.createSpecimen(study, new TaxonImpl(sourceTaxonName, null));
            sourceSpecimen.caughtIn(networkLocation);
            for (String targetTaxonName : targetTaxonNames) {
                final String valueByLabel = StringUtils.trim(interactions.getValueByLabel(targetTaxonName));
                if (StringUtils.isNotBlank(valueByLabel) && !StringUtils.equals("0", valueByLabel)) {
                    final Specimen targetSpecimen = nodeFactory.createSpecimen(study, new TaxonImpl(targetTaxonName, null));
                    targetSpecimen.caughtIn(networkLocation);
                    sourceSpecimen.interactsWith(targetSpecimen, interactType1);
                }
            }
        }
    }

    public static List<String> getNetworkNames(InputStream networkList) throws IOException {
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
