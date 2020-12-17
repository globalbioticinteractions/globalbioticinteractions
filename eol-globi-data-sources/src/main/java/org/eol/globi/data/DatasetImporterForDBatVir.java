package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.util.JSONUtil;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.LOCALITY_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_URL;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_PATH;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_PATH_IDS;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_PATH_NAMES;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_PATH;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_PATH_IDS;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_PATH_NAMES;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_RANK;
import static org.eol.globi.util.JSONUtil.textValueOrEmpty;

public class DatasetImporterForDBatVir extends DatasetImporterWithListener {

    private static final int BATCH_SIZE_DEFAULT = -1;

    public DatasetImporterForDBatVir(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    static String extractFirstPubMedReferenceId(String pubMed) {
        Pattern compile = Pattern.compile("(.*)(nih.gov/pubmed/)(\\d+)(.*)");
        Matcher matcher = compile.matcher(pubMed);
        String referenceId = "";
        if (matcher.find()) {
            String pubmedId = matcher.group(3);
            referenceId = "http://www.ncbi.nlm.nih.gov/pubmed/" + pubmedId;
        }
        return referenceId;
    }

    public static String scrubReferenceCitation(String refs) {
        String citation = Jsoup.clean(refs, new Whitelist());
        return StringUtils.replace(citation, "&nbsp;", "");
    }

    @Override
    public void importStudy() throws StudyImporterException {
        int totalCount = getTotalCount();

        String batchSizeValue = getDataset()
                .getOrDefault("batchSize", Integer.toString(BATCH_SIZE_DEFAULT));

        int batchSize = totalCount;
        try {
            int batchSizeParsed = Integer.parseInt(batchSizeValue);
            batchSize = batchSizeParsed > 0 ? batchSizeParsed : totalCount;
        } catch (NumberFormatException ex) {
            if (getLogger() != null) {
                getLogger().warn(null, "invalid batchSize value [" + batchSizeValue + "]");
            }
        }

        for (int offset = 0; offset < totalCount; offset += batchSize) {
            URI pageURL1 = getPageURL(offset, batchSize);
            try {
                InputStream interactionsPage = getDataset().retrieve(pageURL1);
                parseInteractions(interactionsPage, getInteractionListener());
            } catch (IOException e) {
                getLogger().warn(null, "failed to retrieve page [" + pageURL1.toString() + "], because of: [" + e.getMessage() + "]");
            }
        }

    }

    public int getTotalCount() throws StudyImporterException {
        int totalCount = 0;
        try {
            InputStream firstInteraction = getDataset().retrieve(getPageURL(0, 1));
            JsonNode jsonNode = getLenientObjectMapper().readTree(firstInteraction);
            if (jsonNode.has("totalCount")) {
                JsonNode totalCountNode = jsonNode.get("totalCount");
                if (totalCountNode.isInt()) {
                    totalCount = totalCountNode.asInt();
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to fetch first interaction record", e);
        }
        return totalCount;
    }

    private URI getPageURL(int offset, int limit) {
        String baseUrl = getDataset().getOrDefault("url", "http://www.mgc.ac.cn/cgi-bin/DBatVir/json_data.pl");
        return URI.create(baseUrl + "?start=" + offset + "&limit=" + limit);
    }

    public static void parseInteractions(InputStream pageStream, InteractionListener interactionListener) throws IOException, StudyImporterException {
        ObjectMapper objectMapper = getLenientObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(new InputStreamReader(pageStream, StandardCharsets.ISO_8859_1));
        if (jsonNode.has("data")) {
            JsonNode data = jsonNode.get("data");
            Map<String, String> link = new TreeMap<>();
            for (JsonNode interactionNode : data) {
                String strainName = textValueOrEmpty(interactionNode, "StrainName");
                String virusName = textValueOrEmpty(interactionNode, "Virus");
                if (StringUtils.isNotBlank(strainName)) {
                    link.put(TARGET_TAXON_NAME, strainName);
                    link.put(TARGET_TAXON_RANK, strainName);
                } else if (StringUtils.isNotBlank(virusName)) {
                    if (StringUtils.isNotBlank(virusName)) {
                        link.put(TARGET_TAXON_NAME, virusName);
                        link.put(TARGET_TAXON_RANK, virusName);
                    }
                }

                String targetTaxonId = getTaxonId(interactionNode, "v_tax");
                if (StringUtils.isNotBlank(targetTaxonId)) {
                    link.put(TARGET_TAXON_ID, targetTaxonId);
                }


                String targetTaxonPath = Stream.of(
                        textValueOrEmpty(interactionNode, "v_family"),
                        virusName,
                        strainName)
                        .map(StringUtils::trim)
                        .collect(Collectors.joining(CharsetConstant.SEPARATOR));
                link.put(TARGET_TAXON_PATH, targetTaxonPath);

                String targetTaxonPathNames = Stream.of(
                        "family",
                        "species",
                        "strain")
                        .map(StringUtils::trim)
                        .collect(Collectors.joining(CharsetConstant.SEPARATOR));

                link.put(TARGET_TAXON_PATH_NAMES, targetTaxonPathNames);

                String targetTaxonPathIds = Stream.of(
                        "",
                        targetTaxonId,
                        "")
                        .map(StringUtils::trim)
                        .collect(Collectors.joining(CharsetConstant.SEPARATOR));

                link.put(TARGET_TAXON_PATH_IDS, targetTaxonPathIds);


                setPropertyIfNotBlank(link, interactionNode, SOURCE_TAXON_NAME, "Bat");
                String sourceTaxonId = getTaxonId(interactionNode, "b_tax");
                if (StringUtils.isNotBlank(sourceTaxonId)) {
                    link.put(SOURCE_TAXON_ID, sourceTaxonId);
                }

                String sourceTaxonPath = Stream.of(
                        textValueOrEmpty(interactionNode, "b_family"),
                        textValueOrEmpty(interactionNode, "Bat"))
                        .map(StringUtils::trim)
                        .collect(Collectors.joining(CharsetConstant.SEPARATOR));
                link.put(SOURCE_TAXON_PATH, sourceTaxonPath);

                String sourceTaxonPathNames = Stream.of(
                        "family",
                        "species")
                        .map(StringUtils::trim)
                        .collect(Collectors.joining(CharsetConstant.SEPARATOR));

                link.put(SOURCE_TAXON_PATH_NAMES, sourceTaxonPathNames);

                String sourceTaxonPathIds = Stream.of(
                        "",
                        sourceTaxonId)
                        .map(StringUtils::trim)
                        .collect(Collectors.joining(CharsetConstant.SEPARATOR));

                link.put(SOURCE_TAXON_PATH_IDS, sourceTaxonPathIds);


                String value = JSONUtil.textValueOrNull(interactionNode, "CollectionDate");
                if (StringUtils.isNotBlank(value)) {
                    // account for usage of ~ instead of / to annotate date range
                    String replace = StringUtils.replace(value, "~", "/");
                    link.put(DatasetImporterForMetaTable.EVENT_DATE, replace);
                }


                String localityName = Stream.of(JSONUtil.textValueOrNull(interactionNode, "Country"),
                        JSONUtil.textValueOrNull(interactionNode, "Location"))
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.joining(" "));

                if (StringUtils.isNotBlank(localityName)) {
                    link.put(LOCALITY_NAME, localityName);
                }

                String refs = JSONUtil.textValueOrNull(interactionNode, "Refs");
                if (StringUtils.isNotBlank(refs)) {
                    link.put(REFERENCE_CITATION, scrubReferenceCitation(refs));
                }

                String pubMedString = JSONUtil.textValueOrNull(interactionNode, "PubMed");
                if (StringUtils.isNotBlank(pubMedString)) {
                    String referenceId = extractFirstPubMedReferenceId(pubMedString);
                    if (StringUtils.isNotBlank(referenceId)) {
                        link.put(REFERENCE_ID, referenceId);
                        link.put(REFERENCE_URL, referenceId);

                    }
                }

                link.put(INTERACTION_TYPE_ID, InteractType.HOST_OF.getIRI());
                link.put(INTERACTION_TYPE_NAME, InteractType.HOST_OF.getLabel());

                interactionListener.on(link);
            }
        }
    }

    public static ObjectMapper getLenientObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        return objectMapper;
    }

    private static String getTaxonId(JsonNode interactionNode, String v_tax) {
        String ncbiTaxonId = JSONUtil.textValueOrNull(interactionNode, v_tax);
        String taxonId = "";
        if (StringUtils.isNotBlank(ncbiTaxonId)) {
            taxonId = TaxonomyProvider.NCBI.getIdPrefix() + StringUtils.trim(ncbiTaxonId);
        }
        return taxonId;

    }

    private static void setPropertyIfNotBlank(Map<String, String> link, JsonNode interactionNode, String propertyLabel, String columnName) {
        String value = JSONUtil.textValueOrNull(interactionNode, columnName);
        if (StringUtils.isNotBlank(value)) {
            link.put(propertyLabel, value);
        }
    }


}
