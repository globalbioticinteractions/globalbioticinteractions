package org.eol.globi.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.service.TaxonUtil;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.util.MapDBUtil;
import org.mapdb.BTreeMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME;

public class DatasetImporterForEppo extends DatasetImporterWithListener {

    public static final Pattern EPPO_WEB_API_ENDPOINT_PATTERN = Pattern
            .compile(".*/taxons/taxon/(?<taxonId>[A-Z0-9]+)/(?<recordType>[a-z]+)$");

    public DatasetImporterForEppo(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }


    @Override
    public void importStudy() throws StudyImporterException {
        Dataset dataset = getDataset();
        JsonNode jsonNode = dataset.getConfig().get("url");
        if (jsonNode != null && jsonNode.isTextual()) {
            BTreeMap<String, Map<String, String>> taxonIdMap = MapDBUtil.createBigMap(getWorkDir());
            BTreeMap<String, String> taxonIdToAuthorityMap = MapDBUtil.createBigMap(getWorkDir());
            String url = jsonNode.asText();
            try {
                InputStream forTaxonomy = dataset.retrieve(URI.create(url));
                parseNamesAndTaxonomyOnly(forTaxonomy,
                        (Consumer<Taxon>) taxon -> {
                            taxonIdMap.putIfAbsent(taxon.getExternalId(), new TreeMap<>(TaxonUtil.taxonToMap(taxon)));
                            String authorship = taxon.getAuthorship();
                            if (StringUtils.isNotBlank(authorship)) {
                                taxonIdToAuthorityMap.putIfAbsent(taxon.getExternalId(), authorship);
                            }
                        }
                );

                InputStream forInteractionClaims = dataset.retrieve(URI.create(url));
                LineIterator lineIterator = getLineIterator(forInteractionClaims);
                while (lineIterator.hasNext()) {
                    String line = lineIterator.nextLine();
                    parseInteractionClaim(dataset, new ObjectMapper().readTree(line), new InteractionListener() {
                        @Override
                        public void on(Map<String, String> interaction) throws StudyImporterException {
                            final TreeMap<String, String> enrichedTaxonMap = new TreeMap<>();
                            String sourceTaxonId = interaction.get(TaxonUtil.SOURCE_TAXON_ID);
                            Map<String, String> taxonMap = taxonIdMap.get(sourceTaxonId);
                            if (sourceTaxonId != null && taxonMap != null) {
                                enrichedTaxonMap.put(TaxonUtil.SOURCE_TAXON_NAME, taxonMap.get(PropertyAndValueDictionary.NAME));
                                enrichedTaxonMap.put(TaxonUtil.SOURCE_TAXON_ID, taxonMap.get(PropertyAndValueDictionary.EXTERNAL_ID));
                                enrichedTaxonMap.put(TaxonUtil.SOURCE_TAXON_RANK, taxonMap.get(PropertyAndValueDictionary.RANK));
                                enrichedTaxonMap.put(TaxonUtil.SOURCE_TAXON_AUTHORSHIP, taxonMap.get(PropertyAndValueDictionary.AUTHORSHIP));
                                enrichedTaxonMap.put(TaxonUtil.SOURCE_TAXON_PATH, taxonMap.get(PropertyAndValueDictionary.PATH));
                                String pathIds = populateAuthorships(
                                        taxonMap,
                                        enrichedTaxonMap,
                                        TaxonUtil.SOURCE_TAXON_PATH_AUTHORSHIPS,
                                        taxonIdToAuthorityMap
                                );

                                enrichedTaxonMap.put(TaxonUtil.SOURCE_TAXON_PATH_IDS, pathIds);
                                enrichedTaxonMap.put(TaxonUtil.SOURCE_TAXON_PATH_NAMES, taxonMap.get(PropertyAndValueDictionary.PATH_NAMES));
                                enrichedTaxonMap.put(TaxonUtil.SOURCE_TAXON_COMMON_NAME, taxonMap.get(PropertyAndValueDictionary.COMMON_NAMES));
                            }
                            String targetTaxonId = interaction.get(TaxonUtil.TARGET_TAXON_ID);
                            taxonMap = taxonIdMap.get(targetTaxonId);
                            if (targetTaxonId != null && taxonMap != null) {
                                enrichedTaxonMap.put(TaxonUtil.TARGET_TAXON_NAME, taxonMap.get(PropertyAndValueDictionary.NAME));
                                enrichedTaxonMap.put(TaxonUtil.TARGET_TAXON_ID, taxonMap.get(PropertyAndValueDictionary.EXTERNAL_ID));
                                enrichedTaxonMap.put(TaxonUtil.TARGET_TAXON_RANK, taxonMap.get(PropertyAndValueDictionary.RANK));
                                enrichedTaxonMap.put(TaxonUtil.TARGET_TAXON_AUTHORSHIP, taxonMap.get(PropertyAndValueDictionary.AUTHORSHIP));
                                enrichedTaxonMap.put(TaxonUtil.TARGET_TAXON_PATH, taxonMap.get(PropertyAndValueDictionary.PATH));
                                enrichedTaxonMap.put(TaxonUtil.TARGET_TAXON_PATH_IDS, taxonMap.get(PropertyAndValueDictionary.PATH_IDS));
                                enrichedTaxonMap.put(TaxonUtil.TARGET_TAXON_PATH_NAMES, taxonMap.get(PropertyAndValueDictionary.PATH_NAMES));
                                populateAuthorships(
                                        taxonMap,
                                        enrichedTaxonMap,
                                        TaxonUtil.TARGET_TAXON_PATH_AUTHORSHIPS,
                                        taxonIdToAuthorityMap
                                );

                                enrichedTaxonMap.put(TaxonUtil.TARGET_TAXON_COMMON_NAME, taxonMap.get(PropertyAndValueDictionary.COMMON_NAMES));
                            }
                            getInteractionListener().on(new TreeMap<String, String>(interaction) {{
                                putAll(enrichedTaxonMap);
                            }});
                        }
                    });
                }

            } catch (IOException e) {
                throw new StudyImporterException("failed to retrieve resource at [" + url + "]", e);
            }
        }
    }

    private static String populateAuthorships(Map<String, String> taxonMap, TreeMap<String, String> enrichedTaxonMap, String sourceTaxonPathNameAuthorships, BTreeMap<String, String> taxonIdToAuthorityMap) {
        String pathIds = taxonMap.get(PropertyAndValueDictionary.PATH_IDS);
        String nameAuthorships = generateAuthorshipPath(pathIds, taxonIdToAuthorityMap);
        enrichedTaxonMap.put(sourceTaxonPathNameAuthorships, nameAuthorships);
        return pathIds;
    }

    private static String generateAuthorshipPath(String pathIds, BTreeMap<String, String> taxonIdToAuthorityMap) {
        String[] ids = StringUtils.splitByWholeSeparator(pathIds, CharsetConstant.SEPARATOR);
        List<String> authorshipList = new ArrayList<String>();
        for (String id : ids) {
            String s = taxonIdToAuthorityMap.get(id);
            authorshipList.add(StringUtils.defaultIfBlank(s, ""));
        }
        return StringUtils.join(authorshipList, CharsetConstant.SEPARATOR);
    }

    static void parseNamesAndTaxonomyOnly(InputStream inputStream, Consumer<Taxon> listener) throws JsonProcessingException {
        LineIterator lineIterator = getLineIterator(inputStream);
        String currentFocalTaxonId = null;
        String currentAuthor = null;
        List<String> currentTaxonNames = new ArrayList<>();
        List<String> currentTaxonCommonNames = new ArrayList<>();
        List<String> currentTaxonIds = new ArrayList<>();
        List<String> currentTaxonRanks = new ArrayList<>();
        while (lineIterator.hasNext()) {
            String line = lineIterator.next();
            final JsonNode record = new ObjectMapper().readTree(line);
            String recordType = getRecordType(record);
            if (Arrays.asList("taxonomy", "names").contains(recordType)) {
                String focalTaxonId = getFocalTaxon(record);
                if (currentFocalTaxonId == null) {
                    currentFocalTaxonId = focalTaxonId;
                } else if (!StringUtils.equals(currentFocalTaxonId, focalTaxonId)) {
                    String currentFocalTaxonId1;
                    currentTaxonNames.clear();
                    currentTaxonIds.clear();
                    currentTaxonRanks.clear();
                    currentTaxonCommonNames.clear();
                    currentAuthor = null;
                    currentFocalTaxonId1 = focalTaxonId;
                    currentFocalTaxonId = currentFocalTaxonId1;
                }
                if (record.has("type") && record.has("prefname")) {
                    String name = record.get("prefname").asText();
                    currentTaxonNames.add(name);
                    String taxonId = record.get("eppocode").asText();
                    currentTaxonIds.add(TaxonomyProvider.EPPO.getIdPrefix() + taxonId);
                    String rank = StringUtils.lowerCase(record.get("type").asText());
                    currentTaxonRanks.add(rank);
                    if (endOfHierarchyChain(currentFocalTaxonId, taxonId)) {
                        TaxonImpl t = new TaxonImpl();
                        t.setName(name);
                        t.setRank(rank);
                        t.setExternalId(TaxonomyProvider.EPPO.getIdPrefix() + taxonId);
                        t.setAuthorship(StringUtils.isBlank(currentAuthor) ? null : currentAuthor);
                        t.setPath(StringUtils.join(currentTaxonNames, CharsetConstant.SEPARATOR));
                        t.setPathIds(StringUtils.join(currentTaxonIds, CharsetConstant.SEPARATOR));
                        t.setPathNames(StringUtils.join(currentTaxonRanks, CharsetConstant.SEPARATOR));
                        t.setCommonNames(StringUtils.join(currentTaxonCommonNames, CharsetConstant.SEPARATOR));
                        listener.accept(t);
                    }
                } else if (record.has("lang_iso") && record.has("fullname")) {
                    String language = record.get("lang_iso").asText();
                    String name = record.get("fullname").asText();
                    currentTaxonCommonNames.add(name + " @" + language);
                    if (record.hasNonNull("author")
                            && record.has("preferred")
                            && StringUtils.equals("true",record.get("preferred").asText())) {
                        String text = record.get("author").asText();
                        if (!StringUtils.equals(text, "null")) {
                            currentAuthor = text;
                        }
                    }
                }
            }

        }
    }

    private static LineIterator getLineIterator(InputStream inputStream) {
        LineIterator lineIterator = new LineIterator(IOUtils.toBufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        ));
        return lineIterator;
    }

    private static boolean endOfHierarchyChain(String currentFocalTaxonId, String taxonId) {
        return StringUtils.equals(currentFocalTaxonId, taxonId);
    }

    public static void parseInteractionClaim(Dataset dataset, JsonNode record, InteractionListener listener) throws StudyImporterException {
        String recordType = getRecordType(record);
        String sourceTaxonId = getFocalTaxon(record);

        JsonNode targetTaxonId = record.get("eppocode");
        JsonNode targetTaxonName = record.get("prefname");
        String interactionTypeId = null;
        String interactionTypeName = null;
        if ("pests".equals(recordType)) {
            interactionTypeId = record.get("class_id").asText("");
            interactionTypeName = record.get("class_label").asText("");
        } else if ("bca".equals(recordType)) {
            interactionTypeId = InteractType.KILLED_BY.getIRI();
            interactionTypeName = InteractType.KILLED_BY.getLabel();
        } else if ("vectors".equals(recordType)) {
            interactionTypeId = record.get("vectorclass_id").asText("");
            interactionTypeName = record.get("vectorclass_label").asText("");
        }
        if (likelyInteractionClaim(recordType)) {
            JsonNode referenceCitations = record.get("bibref");
            String text = referenceCitations.asText();
            String[] references = StringUtils.split(StringUtils.replace(text, "*", ""), "\n");

            Iterator<String> refs = Stream.of(references)
                    .map(StringUtils::trim)
                    .filter(r -> !StringUtils.startsWith(r, "INTERNET"))
                    .filter(r -> !StringUtils.startsWith(r, "-------"))
                    .iterator();

            while (refs.hasNext()) {
                String reference = refs.next();
                Map<String, String> interaction = new TreeMap<>();
                interaction.put(TaxonUtil.SOURCE_TAXON_ID, TaxonomyProvider.EPPO.getIdPrefix() + sourceTaxonId);
                interaction.put(INTERACTION_TYPE_NAME, interactionTypeName);
                interaction.put(INTERACTION_TYPE_ID, interactionTypeId);
                interaction.put(TaxonUtil.TARGET_TAXON_ID, TaxonomyProvider.EPPO.getIdPrefix() + targetTaxonId.asText());
                interaction.put(TaxonUtil.TARGET_TAXON_NAME, targetTaxonName.asText());
                interaction.put(DatasetImporterForTSV.REFERENCE_CITATION, reference);
                interaction.put(DatasetImporterForTSV.REFERENCE_ID, dataset.getNamespace() + reference);
                interaction.put(DatasetImporterForTSV.REFERENCE_URL, getHtmlUrl(record));
                interaction.put("recordType", recordType);
                listener.on(interaction);
            }
        }
    }

    private static String getFocalTaxon(JsonNode record) {
        Matcher matcher = getApiEndpointMatcher(record);
        String taxonId = null;
        if (matcher != null && matcher.matches()) {
            taxonId = matcher.group("taxonId");
        }
        return taxonId;
    }

    private static Matcher getApiEndpointMatcher(JsonNode record) {
        JsonNode revisionOf = record.get("http://www.w3.org/ns/prov#wasRevisionOf");
        String url = revisionOf == null ? null : revisionOf.asText();
        return url == null ? null : EPPO_WEB_API_ENDPOINT_PATTERN.matcher(url);
    }

    private static String getRecordType(JsonNode record) {
        Matcher matcher = getApiEndpointMatcher(record);
        String recordType = null;
        if (matcher != null && matcher.matches()) {
            recordType = matcher.group("recordType");
        }
        return recordType;
    }

    private static String getHtmlUrl(JsonNode record) {
        Matcher matcher = getApiEndpointMatcher(record);
        String htmlUrl = null;
        if (matcher != null && matcher.matches()) {
            String recordType = matcher.group("recordType");
            String taxonId = matcher.group("taxonId");
            htmlUrl = "https://gd.eppo.int/taxon/" + taxonId + "/" + recordType;
        }
        return htmlUrl;
    }

    private static boolean likelyInteractionClaim(String recordType) {
        return Arrays.asList("pests", "bca", "vectors").contains(recordType);
    }

}
