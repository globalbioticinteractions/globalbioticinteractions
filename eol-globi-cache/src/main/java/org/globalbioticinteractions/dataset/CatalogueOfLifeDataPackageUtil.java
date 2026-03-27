package org.globalbioticinteractions.dataset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.DateUtil;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

public class CatalogueOfLifeDataPackageUtil {

    public static JsonNode datasetFor(ResourceService origDataset, URI dataPackageConfig) throws IOException {
        try {
            InputStream config = origDataset.retrieve(dataPackageConfig);

            List<String> availableFilenames = new ArrayList<>();

            // try name combinations
            // https://catalogueoflife.github.io/coldp/
            List<String> extensions = Arrays.asList("csv", "tsv", "txt", "tab");

            List<String> nameCandidates = Arrays.asList("name", "Name");
            List<String> taxonCandidates = Arrays.asList("taxon", "Taxon");
            List<String> nameUsageCandidates = Arrays.asList("nameusage", "name-usage", "nameUsage", "NameUsage", "name_usage");
            List<String> speciesInteractionCandidates = Arrays.asList("speciesinteraction", "species-interaction", "speciesInteraction", "SpeciesInteraction", "species_interaction");
            List<String> referenceCandidates = Arrays.asList("reference", "Reference");

            TreeMap<String, String> namesDetected = new TreeMap<String, String>() {
                {
                    put("name.csv",
                            detectTableCandidates(origDataset, nameCandidates, extensions, availableFilenames));
                    put("taxon.csv",
                            detectTableCandidates(origDataset, taxonCandidates, extensions, availableFilenames));
                    put("nameusage.csv",
                            detectTableCandidates(origDataset, nameUsageCandidates, extensions, availableFilenames));
                    put("speciesinteractions.csv",
                            detectTableCandidates(origDataset, speciesInteractionCandidates, extensions, availableFilenames));
                    put("reference.csv",
                            detectTableCandidates(origDataset, referenceCandidates, extensions, availableFilenames));
                }
            };

            JsonNode configNode = new ObjectMapper(new YAMLFactory()).readTree(config);

            InputStream schemaStream = namesDetected.get("nameusage.csv") == null
                    ? getSchemaNoneNameUsage()
                    : getSchemaNameUsage();
            JsonNode jsonNode = new ObjectMapper().readTree(schemaStream);
            JsonNode tables = jsonNode.at("/tables");

            for (JsonNode table : tables) {
                setBibliographicCitation(table, configNode);
                JsonNode url = table.at("/url");
                String detectedFilename = namesDetected.get(url.asText());
                if (StringUtils.isBlank(detectedFilename)) {
                    throw new IOException("failed to resolve equivalent for table [" + url.asText() + "]");
                }
                ((ObjectNode) table).put("url", detectedFilename);
                String delimiter = StringUtils.endsWith(detectedFilename, "csv") ? "," : "\t";
                ((ObjectNode) table).put("delimiter", delimiter);
            }

            return jsonNode;
        } catch (IOException e) {
            throw new IOException("failed to handle", e);
        }
    }

    private static String detectTableCandidates(ResourceService origDataset, List<String> nameUsageCandidates, List<String> extensions, List<String> availableFilenames) {
        String detectedFilename = null;
        for (String name : nameUsageCandidates) {
            for (String plural : Arrays.asList("", "s")) {
                for (String subfolder : Arrays.asList("", "data/")) {
                    for (String ext : extensions) {
                        String filename = name + plural + "." + ext;
                        try (InputStream retrieve = origDataset.retrieve(URI.create("/" + subfolder + filename))) {
                            IOUtils.copy(retrieve, NullOutputStream.NULL_OUTPUT_STREAM);
                            detectedFilename = filename;
                            break;
                        } catch (IOException ex) {
                            // ignore
                        }
                    }
                }
            }
        }
        return detectedFilename;
    }

    private static void setBibliographicCitation(JsonNode jsonNode, JsonNode configNode) {
        if (jsonNode instanceof ObjectNode) {
            StringBuilder bibtexEntry = new StringBuilder();
            bibtexEntry.append("@misc{");
            String key = StringUtils.defaultIfBlank(configNode.at("/key").asText(), configNode.at("/alias").asText());

            bibtexEntry.append("ChecklistBankDataset" + key);
            appendFieldIfAvailable(bibtexEntry, "publisher", configNode.at("/publisher/organisation").asText());
            appendFieldIfAvailable(bibtexEntry, "address", configNode.at("/publisher/address").asText());
            appendFieldIfAvailable(bibtexEntry, "version", configNode.at("/version").asText());
            appendFieldIfAvailable(bibtexEntry, "url", configNode.at("/url").asText());
            appendFieldIfAvailable(bibtexEntry, "title", configNode.at("/title").asText());
            DateTime issuedDate = DateUtil.parseDateUTC(configNode.at("/issued").asText());
            JsonNode creators = configNode.at("/creator");
            StringBuilder authorString = new StringBuilder();
            for (JsonNode creator : creators) {
                appendPropertyValue(creator, "/family", authorString, " and ");
                appendPropertyValue(creator, "/given", authorString, ", ");
            }

            appendFieldIfAvailable(bibtexEntry, "author", authorString.toString());
            appendFieldIfAvailable(bibtexEntry, "year", issuedDate.getYear());
            appendFieldIfAvailable(bibtexEntry, "month", issuedDate.getMonthOfYear());
            bibtexEntry.append("}");
            ((ObjectNode) jsonNode).put("dcterms:bibliographicCitation", bibtexEntry.toString());
        }
    }

    private static void appendFieldIfAvailable(StringBuilder bibtexEntry, String fieldName, int fieldValue) {
        bibtexEntry.append(", " + fieldName + " = " + fieldValue);
    }

    private static void insertDelimiter(StringBuilder authorString, String delimiter) {
        if (authorString.length() > 0) {
            authorString.append(delimiter);
        }
    }

    private static void appendPropertyValue(JsonNode creator, String propertyPointer, StringBuilder authorString, String delimiter) {
        JsonNode property = creator.at(propertyPointer);
        if (!property.isMissingNode()
                && StringUtils.isNoneBlank(property.asText())) {
            insertDelimiter(authorString, delimiter);
            authorString.append("{");
            authorString.append(property.asText());
            authorString.append("}");
        }
    }

    private static void appendFieldIfAvailable(StringBuilder bibtexEntry, String fieldName, String fieldValue) {
        if (StringUtils.isNoneBlank(fieldValue)) {
            bibtexEntry.append(", " + fieldName + " = {" + fieldValue + "}");
        }
    }

    private static InputStream getSchemaNameUsage() {
        return CatalogueOfLifeDataPackageUtil.class.getResourceAsStream("coldp/coldp-schema-name-usage.json");
    }

    private static InputStream getSchemaNoneNameUsage() {
        return CatalogueOfLifeDataPackageUtil.class.getResourceAsStream("coldp/coldp-schema-non-name-usage.json");
    }

}
