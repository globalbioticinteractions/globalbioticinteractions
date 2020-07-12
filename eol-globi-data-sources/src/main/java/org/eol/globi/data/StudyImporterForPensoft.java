package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.CSVTSVUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class StudyImporterForPensoft extends StudyImporterWithListener {
    private static final Log LOG = LogFactory.getLog(StudyImporterForPensoft.class);

    public StudyImporterForPensoft(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    public static void parseRowsAndEnrich(JsonNode biodivTable,
                                          InteractionListener listener,
                                          ImportLogger logger,
                                          ResourceService resourceService) throws StudyImporterException {
        final Elements tables = getTables(biodivTable);
        Elements rows = tables.get(0).select("tr");
        final List<String> columnNames = getColumnNames(tables);

        final Map<String, String> tableReferences;
        try {
            tableReferences = parseTableReferences(biodivTable, resourceService);
        } catch (IOException e) {
            throw new StudyImporterException("failed to retrieve reference", e);
        }

        for (Element row : rows) {
            Elements rowColumns = row.select("td");

            // expand spanned rows
            expandSpannedRows(row, rowColumns);


            final Map<String, String> rowValue = new TreeMap<>();
            final List<Pair<String, Term>> rowTerms = parseRowValues(columnNames, rowValue, rowColumns);

            if (!rowValue.isEmpty()) {
                final List<Map<String, Term>> distinctPermutations = new ArrayList<>();
                expandRows(rowTerms, new PermutationListener() {
                    @Override
                    public void on(Map<String, Term> link) {
                        if (!distinctPermutations.contains(link) && !link.isEmpty()) {
                            distinctPermutations.add(link);

                        }
                    }
                }, distinctKeys(rowTerms));

                final TreeMap<String, String> link = new TreeMap<String, String>(rowValue) {{
                    putAll(tableReferences);
                }};

                if (rowColumns.size() != columnNames.size()) {
                    logger.warn(LogUtil.contextFor(link), "inconsistent column usage: found [" + rowColumns.size() + "] data columns, but [" + columnNames.size() + "] column definitions");
                }

                if (distinctPermutations.isEmpty()) {
                    listener.newLink(link);
                } else {
                    for (Map<String, Term> distinctPermutation : distinctPermutations) {
                        listener.newLink(new TreeMap<String, String>(link) {{
                            List<Taxon> collectTaxa = new ArrayList<>();
                            for (Map.Entry<String, Term> taxonNames : distinctPermutation.entrySet()) {
                                put(taxonNames.getKey() + "_taxon_name", taxonNames.getValue().getName());
                                final String id = taxonNames.getValue().getId();
                                put(taxonNames.getKey() + "_taxon_id", id);
                                try {
                                    final Taxon taxon = retrieveTaxonHierarchyById(id, resourceService);
                                    if (taxon != null) {
                                        collectTaxa.add(taxon);
                                        final Map<String, String> taxonMap = TaxonUtil.taxonToMap(taxon, taxonNames.getKey() + "_taxon_");
                                        for (String s : taxonMap.keySet()) {
                                            putIfAbsent(s, taxonMap.get(s));
                                        }
                                    }
                                } catch (IOException e) {
                                    // ignore
                                }
                            }

                            final List<Taxon> taxons = TaxonUtil.determineNonOverlappingTaxa(collectTaxa);
                            if (taxons.size() == 2) {
                                put(StudyImporterForTSV.INTERACTION_TYPE_NAME, InteractType.INTERACTS_WITH.getLabel());
                                put(StudyImporterForTSV.INTERACTION_TYPE_ID, InteractType.INTERACTS_WITH.getIRI());

                                final Taxon sourceTaxon = taxons.get(0);
                                put(TaxonUtil.SOURCE_TAXON_NAME, sourceTaxon.getName());
                                put(TaxonUtil.SOURCE_TAXON_ID, sourceTaxon.getExternalId());
                                put(TaxonUtil.SOURCE_TAXON_RANK, sourceTaxon.getRank());
                                put(TaxonUtil.SOURCE_TAXON_PATH, sourceTaxon.getPath());
                                put(TaxonUtil.SOURCE_TAXON_PATH_NAMES, sourceTaxon.getPathNames());
                                put(TaxonUtil.SOURCE_TAXON_PATH_IDS, sourceTaxon.getPathIds());

                                final Taxon targetTaxon = taxons.get(1);
                                put(TaxonUtil.TARGET_TAXON_NAME, targetTaxon.getName());
                                put(TaxonUtil.TARGET_TAXON_ID, targetTaxon.getExternalId());
                                put(TaxonUtil.TARGET_TAXON_RANK, targetTaxon.getRank());
                                put(TaxonUtil.TARGET_TAXON_PATH, targetTaxon.getPath());
                                put(TaxonUtil.TARGET_TAXON_PATH_NAMES, targetTaxon.getPathNames());
                                put(TaxonUtil.TARGET_TAXON_PATH_IDS, targetTaxon.getPathIds());
                            }
                        }

                        });
                    }
                }
            }
        }
    }

    public static void expandSpannedRows(Element row, Elements rowColumns) {
        for (Element rowColumn : rowColumns) {
            final String attr = rowColumn.attr("rowspan");
            final int rowSpan = NumberUtils.toInt(attr, 1);
            if (rowSpan > 1) {
                // found spanned column
                final Element next = row.nextElementSibling();
                if (next != null) {
                    final Element clone = rowColumn.clone();
                    clone.attr("rowspan", Integer.toString(rowSpan - 1));
                    next.insertChildren(rowColumns.indexOf(rowColumn), Collections.singleton(clone));
                }
            }
        }
    }

    public static List<Pair<String, Term>> parseRowValues(List<String> columnNames, Map<String, String> rowValue, Elements cols) {
        final List<Pair<String, Term>> rowTerms = new ArrayList<>();

        for (int j = 0; j < cols.size(); j++) {
            final Element element = cols.get(j);
            final String headerName = j < columnNames.size() ? columnNames.get(j) : ("column" + j);
            rowValue.put(headerName, element.text());

            final Elements names = element.select(new Evaluator.TagEndsWith("tp:taxon-name"));
            for (Element name : names) {
                final String id = TaxonomyProvider.OPEN_BIODIV.getIdPrefix() + names.attr("obkms_id");
                final Term term = asTerm(id, name.text());
                rowTerms.add(Pair.of(headerName, term));
            }
        }
        return rowTerms;
    }

    public static List<String> getColumnNames(Elements tables) {
        List<String> headerNames = new ArrayList<>();
        if (tables.size() > 0) {
            Element table = tables.get(0);
            Elements headers = table.select("th");
            for (int h = 0; h < headers.size(); h++) {
                headerNames.add(StringUtils.defaultIfBlank(headers.get(h).text(), "header-" + h));
            }
        }
        return headerNames;
    }

    public static TermImpl asTerm(String id, String name) {
        return new TermImpl(id, name);
    }

    public static String findCitationByDoi(String doi, ResourceService resourceService) throws IOException {
        String sparql = "PREFIX fabio: <http://purl.org/spar/fabio/>\n" +
                "PREFIX prism: <http://prismstandard.org/namespaces/basic/2.0/>\n" +
                "PREFIX doco: <http://purl.org/spar/doco/>\n" +
                "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                "select ?article ?title ?doi where { \n" +
                "    BIND(\"" + doi + "\" AS ?doi). \n" +
                "    ?article a fabio:JournalArticle.\n" +
                "    ?article prism:doi ?doi.\n" +
                "    ?article dc:title ?title.\n" +
                "   \n" +
                "} limit 1";

        try {
            final LabeledCSVParser parser = query(sparql, resourceService);
            parser.getLine();
            return StringUtils.join(Arrays.asList(parser.getValueByLabel("title"), parser.getValueByLabel("article"), parser.getValueByLabel("doi")), ". ");
        } catch (URISyntaxException e) {
            throw new IOException("marlformed uri", e);
        }
    }

    public static LabeledCSVParser query(String sparql, ResourceService resourceService) throws URISyntaxException, IOException {
        URI url = createSparqlURI(sparql);
        return CSVTSVUtil.createLabeledCSVParser(resourceService.retrieve(url));
    }

    public static URI createSparqlURI(String sparql) throws URISyntaxException {
        final URI endpoint = URI.create("http://graph.openbiodiv.net/repositories/OpenBiodiv2020");
        return new URI(endpoint.getScheme(), endpoint.getHost(), endpoint.getPath(), "query=" + sparql, null);
    }

    public static Taxon retrieveTaxonHierarchyById(String taxonId, ResourceService resourceService) throws IOException {
        final String normalizedTaxonId = StringUtils.replace(taxonId, TaxonomyProvider.OPEN_BIODIV.getIdPrefix(), "");
        String sparql = "PREFIX fabio: <http://purl.org/spar/fabio/>\n" +
                "PREFIX prism: <http://prismstandard.org/namespaces/basic/2.0/>\n" +
                "PREFIX doco: <http://purl.org/spar/doco/>\n" +
                "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                "select ?name ?rank ?id ?kingdom ?phylum ?class ?order ?family ?genus ?specificEpithet " +
                "where { {\n" +
                "    BIND(<http://openbiodiv.net/" + normalizedTaxonId + "> AS ?id). \n" +
                "      OPTIONAL { ?taxon <http://rs.tdwg.org/dwc/terms/specificEpithet> ?specificEpithet.}\n" +
                "      OPTIONAL { ?taxon <http://rs.tdwg.org/dwc/terms/genus> ?genus.}\n" +
                "      OPTIONAL { ?taxon <http://rs.tdwg.org/dwc/terms/family> ?family.}\n" +
                "      OPTIONAL { ?taxon <http://rs.tdwg.org/dwc/terms/order> ?order. }\n" +
                "      OPTIONAL { ?taxon <http://rs.tdwg.org/dwc/terms/class> ?class. }\n" +
                "      OPTIONAL { ?taxon <http://rs.tdwg.org/dwc/terms/phylum> ?phylum.}\n" +
                "      OPTIONAL { ?taxon <http://rs.tdwg.org/dwc/terms/kingdom> ?kingdom.}\n" +
                "    { ?id <http://proton.semanticweb.org/protonkm#mentions> ?taxon.\n" +
                "      ?taxon <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://openbiodiv.net/ScientificName>.\n" +
                "      OPTIONAL { ?taxon <http://rs.tdwg.org/dwc/terms/verbatimTaxonRank> ?rank.}\n" +
                "      ?taxon <http://www.w3.org/2000/01/rdf-schema#label> ?name.\n" +
                "   } " +
                "   UNION\n" +
                "    { ?id <http://proton.semanticweb.org/protonkm#mentions> ?btaxon.\n" +
                "      ?btaxon <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://openbiodiv.net/ScientificName>.\n" +
                "      ?btaxon <http://openbiodiv.net/hasGbifTaxon> ?taxon.\n" +
                "      OPTIONAL { ?taxon <http://rs.tdwg.org/dwc/terms/taxonRank> ?rank.}\n" +
                "      ?btaxon <http://www.w3.org/2000/01/rdf-schema#label> ?name.\n" +
                "   }" +
                "  } " +
                "} limit 2";

        try {
            final LabeledCSVParser parser = query(sparql, resourceService);
            Taxon taxon = null;
            while ((taxon == null
                    || StringUtils.isBlank(taxon.getPathNames()))
                    && parser.getLine() != null) {
                taxon = parseTaxon(parser);
            }
            return taxon;
        } catch (URISyntaxException e) {
            throw new IOException("marlformed uri", e);
        }
    }

    public static TaxonImpl parseTaxon(LabeledCSVParser parser) {
        final String name = parser.getValueByLabel("name");
        final String rank = StringUtils.defaultIfBlank(parser.getValueByLabel("rank"), null);
        final String id = parser.getValueByLabel("id");
        final TaxonImpl taxon = new TaxonImpl(name, id);
        taxon.setRank(rank);

        Map<String, String> nameMap = new LinkedHashMap<>();
        nameMap.put(rank, name);
        Arrays.asList("specificEpithet", "genus", "family", "order", "class", "phylum", "kingdom")
                .forEach(x -> add(parser, nameMap, x));

        final String path = TaxonUtil.generateTaxonPath(nameMap);
        taxon.setPath(StringUtils.defaultIfBlank(path, name));
        taxon.setPathNames(TaxonUtil.generateTaxonPathNames(nameMap));
        return taxon;
    }

    public static void add(LabeledCSVParser parser, Map<String, String> nameMap, String rankName) {
        final String valueByLabel = parser.getValueByLabel(rankName);
        if (StringUtils.isNotBlank(valueByLabel)) {
            nameMap.put(rankName, valueByLabel);
        }
    }

    @Override
    public void importStudy() throws StudyImporterException {
        try {
            final String url = getDataset().getOrDefault("url", null);
            if (StringUtils.isBlank(url)) {
                throw new StudyImporterException("please specify [url] in config");
            }
            final InputStream is = getDataset().retrieve(URI.create(url));
            BufferedReader reader = IOUtils.toBufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                final JsonNode biodivTable = new ObjectMapper().readTree(line);
                parseRowsAndEnrich(biodivTable, getInteractionListener(), getLogger(), getDataset());
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to retrieve resource", e);
        }
    }

    private static Map<String, String> parseTableReferences(final JsonNode biodivTable, ResourceService resourceService) throws IOException {
        final String table_id = biodivTable.has("table_id") ? biodivTable.get("table_id").asText() : "";
        final String referenceUrl = StringUtils.replaceAll(table_id, "[<>]", "");
        final String doi = biodivTable.has("article_doi") ? biodivTable.get("article_doi").asText() : "";
        final String citation = findCitationByDoi(doi, resourceService);
        return new TreeMap<String, String>() {
            {
                put(StudyImporterForTSV.REFERENCE_ID, referenceUrl);
                put(StudyImporterForTSV.REFERENCE_URL, referenceUrl);
                put(StudyImporterForTSV.REFERENCE_DOI, doi);
                put(StudyImporterForTSV.REFERENCE_CITATION, citation);
                put("tableCaption", biodivTable.has("caption") ? biodivTable.get("caption").asText() : "");
            }
        };
    }


    public static Elements getTables(JsonNode jsonNode) {
        final JsonNode table_content = jsonNode.get("table_content");
        final String html = table_content.asText();
        final Document doc = Jsoup.parse(html);

        return doc.select("table");
    }

    public static void expandRows(List<Pair<String, Term>> remaining, PermutationListener listener, long distinctHeaders) throws StudyImporterException {
        if (!remaining.isEmpty()) {
            permutation(Collections.emptyList(), remaining, listener, distinctHeaders);
        }
    }

    private static void permutation(List<Pair<String, Term>> combination, List<Pair<String, Term>> remaining, PermutationListener listener, long distinctHeaders) throws StudyImporterException {
        int n = remaining.size();

        if (n == 0) {
            listener.on(new TreeMap<String, Term>() {{
                final long uniqueHeaders = distinctKeys(combination);
                if (distinctHeaders == uniqueHeaders) {
                    for (Pair<String, Term> pair : combination) {
                        put(pair.getKey(), pair.getValue());
                    }
                }
            }});
        } else {
            for (int i = 0; i < n; i++) {
                final Pair<String, Term> selected = remaining.get(i);
                List<Pair<String, Term>> newRemaining = new ArrayList<>();
                newRemaining.addAll(remaining.subList(0, i));
                newRemaining.addAll(remaining.subList(i + 1, n));

                final List<Pair<String, Term>> newRemainingWithSelectedKeyValues = newRemaining.stream()
                        .filter(x -> !StringUtils.equals(selected.getKey(), x.getKey()))
                        .collect(Collectors.toList());

                List<Pair<String, Term>> newCombination = new ArrayList<>(combination);
                newCombination.add(selected);
                permutation(newCombination, newRemainingWithSelectedKeyValues, listener, distinctHeaders);
            }
        }
    }

    static long distinctKeys(List<Pair<String, Term>> combination) {
        return combination.stream().map(Pair::getKey).distinct().count();
    }


    interface PermutationListener {
        void on(Map<String, Term> permutation);
    }
}
