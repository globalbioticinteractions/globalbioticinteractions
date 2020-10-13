package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.TaxonUtil;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;
import org.globalbioticinteractions.pensoft.AddColumnFromCaption;
import org.globalbioticinteractions.pensoft.ExpandColumnSpans;
import org.globalbioticinteractions.pensoft.ExpandRowSpans;
import org.globalbioticinteractions.pensoft.AddColumnsForOpenBiodivTerms;
import org.globalbioticinteractions.pensoft.TablePreprocessor;
import org.globalbioticinteractions.pensoft.TableRectifier;
import org.globalbioticinteractions.pensoft.TableUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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

public class DatasetImporterForPensoft extends DatasetImporterWithListener {
    private SparqlClientFactory sparqlClientFactory = new SparqlClientCachingFactory();

    public DatasetImporterForPensoft(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    public static ObjectNode createColumnSchema(List<String> columnNames) {
        final List<ObjectNode> columns = columnNames
                .stream()
                .map(x -> {
                    final ObjectMapper objectMapper = new ObjectMapper();
                    final ObjectNode column = objectMapper.createObjectNode();
                    column.put("name", x);
                    column.put("titles", x);
                    column.put("datatype", "string");
                    return column;
                }).collect(Collectors.toList());

        final ObjectMapper objectMapper = new ObjectMapper();
        final ObjectNode obj = objectMapper.createObjectNode();
        final ArrayNode arrayNode = objectMapper.createArrayNode();
        columns.forEach(arrayNode::add);
        obj.put("columns", arrayNode);
        return obj;
    }

    public static String parseInteractionType(JsonNode jsonNode) throws IOException {
        String interactionType = null;
        if (jsonNode.has("annotations")) {
            JsonNode annotations = jsonNode.get("annotations");
            for (JsonNode annotation : annotations) {
                if (annotation.has("id")) {
                    for (JsonNode id : annotation.get("id")) {
                        if (id.isTextual()) {
                            interactionType = id.asText();
                        }
                    }
                }
            }
        }
        return interactionType;
    }


    @Override
    public void importStudy() throws StudyImporterException {
        try (SparqlClient sparqlClient = getSparqlClientFactory().create(getDataset())) {
            final String url = getDataset().getOrDefault("url", null);
            if (StringUtils.isBlank(url)) {
                throw new StudyImporterException("please specify [url] in config");
            }
            final InputStream is = getDataset().retrieve(URI.create(url));
            BufferedReader reader = IOUtils.toBufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                final JsonNode biodivTable = new ObjectMapper().readTree(line);
                parseRowsAndEnrich(biodivTable, getInteractionListener(), getLogger(), sparqlClient);
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to retrieve resource", e);
        }
    }


    void parseRowsAndEnrich(JsonNode biodivTable,
                            InteractionListener listener,
                            ImportLogger logger,
                            final SparqlClient sparqlClient) throws StudyImporterException {
        final Map<String, String> tableReferences;
        try {
            tableReferences = parseTableReferences(biodivTable, sparqlClient);
        } catch (IOException e) {
            throw new StudyImporterException("failed to retrieve reference", e);
        }

        final JsonNode tableContent = biodivTable.get("table_content");
        final String htmlString = tableContent.asText();
        //tableReferences.put("table_content", htmlString);

        String tableUUID = getTableUUID(biodivTable);

        if (StringUtils.isBlank(tableUUID)) {
            logger.warn(LogUtil.contextFor(tableReferences), "no table uuid found");
        } else {
            tableReferences.put("uuid", tableUUID);
        }

        JsonNode caption_xml = biodivTable.get("caption_xml");
        TableRectifier tableRectifier = new TableRectifier();
        if (caption_xml != null && caption_xml.isTextual()) {
            AddColumnFromCaption captionProcessor = new AddColumnFromCaption(caption_xml.asText());

            tableRectifier = new TableRectifier(
                    captionProcessor,
                    new TablePreprocessor(),
                    new ExpandColumnSpans(),
                    new ExpandRowSpans(),
                    new AddColumnsForOpenBiodivTerms()
            );
        }

        if (TableUtil.isRectangularTable(Jsoup.parse(htmlString))) {
            logger.info(LogUtil.contextFor(tableReferences), "original Pensoft table is rectangular.");
        } else {
            logger.warn(LogUtil.contextFor(tableReferences), "original Pensoft table is not rectangular.");
        }

        String rectifiedTable = tableRectifier.process(htmlString);
        //tableReferences.put("tableContentRectified", rectifiedTable);

        final Document doc = Jsoup.parse(rectifiedTable);
        if (TableUtil.isRectangularTable(doc)) {
            logger.info(LogUtil.contextFor(tableReferences), "pre-processed Pensoft table is rectangular.");
        } else {
            logger.warn(LogUtil.contextFor(tableReferences), "pre-processed Pensoft table is not rectangular: [" + htmlString + "]");
        }

        JsonNode columnSchema = null;
        if (StringUtils.isNotBlank(tableUUID)) {
            try {
                InputStream retrieve = getDataset() == null ? null : getDataset().retrieve(URI.create(tableUUID + "-schema.json"));
                if (retrieve == null) {
                    logger.info(LogUtil.contextFor(tableReferences), "no schema found for openbiodiv table [" + tableUUID + "]");
                } else {
                    logger.info(LogUtil.contextFor(tableReferences), "found custom schema for openbiodiv table [" + tableUUID + "]");
                    columnSchema = new ObjectMapper().readTree(retrieve);
                }
            } catch (IOException ex) {
                logger.warn(LogUtil.contextFor(tableReferences), "failed to read schema for openbiodiv table [" + tableUUID + "]");
            }
        }

        if (columnSchema == null) {
            logger.info(LogUtil.contextFor(tableReferences), "using generated schema for openbiodiv table [" + tableUUID + "]");
            columnSchema = generateColumnSchema(doc);
        }

        final Elements table = doc.select("table");

        if (columnSchema == null) {
            throw new StudyImporterException("failed to generate proposed table schema");
        } else {
            try {
                tableReferences.put("tableSchema", new ObjectMapper().writeValueAsString(columnSchema));
            } catch (IOException e) {
                throw new StudyImporterException("failed to write proposed table schema");
            }
        }

        Elements rows = table.get(0).select("tr");
        List<DatasetImporterForMetaTable.Column> columns = DatasetImporterForMetaTable.columnNamesForSchema(columnSchema);

        for (Element row : rows) {
            Elements rowColumns = row.select("td");

            if (columns.size() != rowColumns.size()) {
                logger.warn(LogUtil.contextFor(tableReferences), "found [" + columns.size() + "] column definitions, but [" + rowColumns.size() + "] data values");
            } else {
                final TreeMap<String, String> link = new TreeMap<String, String>() {{
                    try {
                        put(DatasetImporterForTSV.INTERACTION_TYPE_ID, parseInteractionType((biodivTable)));
                    } catch (IOException e) {
                        logger.warn(LogUtil.contextFor(tableReferences), "failed to extract interactionType from table context, using default instead");
                        put(DatasetImporterForTSV.INTERACTION_TYPE_ID, InteractType.INTERACTS_WITH.getIRI());
                    }
                    putAll(tableReferences);
                }};

                for (int i = 0; i < rowColumns.size(); i++) {
                    link.put(columns.get(i).getName(), rowColumns.get(i).text());
                }
                listener.newLink(link);

            }
        }
    }

    private String getTableUUID(JsonNode biodivTable) {
        JsonNode table_id = biodivTable.get("table_id");
        String tableUUID = null;
        if (table_id != null && table_id.isTextual()) {
            tableUUID = StringUtils.replacePattern(table_id.asText(), "(<)|(>)|(http://openbiodiv.net/)", "");
        }
        return tableUUID;
    }

    private ObjectNode generateColumnSchema(Document doc) {
        final Elements table = doc.select("table");
        return createColumnSchema(getColumnNames(table));
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
                    int index = rowColumns.indexOf(rowColumn);
                    if (index < next.children().size()) {
                        next.insertChildren(index, Collections.singleton(clone));
                    }
                }
            }
        }
        for (Element rowColumn : rowColumns) {
            final String attr = rowColumn.attr("rowspan");
            final int rowSpan = NumberUtils.toInt(attr, 1);
            if (rowSpan > 1) {
                rowColumn.attr("rowspan", "1");
            }
        }
    }

    static List<Pair<String, Term>> extractTermsForRowValue(List<String> columnNames, Map<String, String> rowValue, Elements cols) {
        return new ArrayList<>();
    }

    static List<String> getColumnNames(Elements tables) {
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

    private static TermImpl asTerm(String id, String name) {
        return new TermImpl(id, name);
    }

    static String findCitationByDoi(String doi, SparqlClient openBiodivClient) throws IOException {
        String sparql = "PREFIX fabio: <http://purl.org/spar/fabio/>\n" +
                "PREFIX prism: <http://prismstandard.org/namespaces/basic/2.0/>\n" +
                "PREFIX doco: <http://purl.org/spar/doco/>\n" +
                "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                "SELECT " +
                "   ?article " +
                "   ?title " +
                "   ?doi " +
                "   (group_concat(distinct ?authorName; separator=\", \") as ?authorsList)  " +
                "   ( REPLACE(str(?pubDate), \"(\\\\d*)-.*\", \"$1\") as ?pubYear) " +
                "   ?journalName " +
                "WHERE { \n" +
                "    BIND(\"" + doi + "\" AS ?doi). \n" +
                "    ?article a fabio:JournalArticle.\n" +
                "    ?article prism:doi ?doi.\n" +
                "    ?article dc:title ?title.\n" +
                "    ?article prism:publicationDate ?pubDate.\n" +
                "    ?article <http://purl.org/vocab/frbr/core#realizationOf> ?paper.\n" +
                "    ?paper dc:creator ?author.\n" +
                "    ?journal <http://purl.org/vocab/frbr/core#part> ?article.\n" +
                "    ?journal a fabio:Journal.\n" +
                "    ?journal <http://www.w3.org/2004/02/skos/core#prefLabel> ?journalName.\n" +
                "    ?author <http://www.w3.org/2000/01/rdf-schema#label> ?authorName.\n" +
                "}   GROUP BY ?article ?title ?doi ?pubDate ?journalName \n" +
                " LIMIT 1";

        try {
            final LabeledCSVParser parser = openBiodivClient.query(sparql);
            parser.getLine();
            final String doiURIString = DOI.create(doi).toURI().toString();
            return StringUtils.join(Arrays.asList(
                    parser.getValueByLabel("authorsList"),
                    parser.getValueByLabel("pubYear"),
                    parser.getValueByLabel("title"),
                    parser.getValueByLabel("journalName"),
                    doiURIString), ". ");
        } catch (MalformedDOIException e) {
            throw new IOException("marlformed uri", e);
        }
    }

    public static URI createSparqlURI(String sparql) throws URISyntaxException {
        final URI endpoint = URI.create("http://graph.openbiodiv.net/repositories/OpenBiodiv2020");
        return new URI(endpoint.getScheme(), endpoint.getHost(), endpoint.getPath(), "query=" + sparql, null);
    }

    static Taxon retrieveTaxonHierarchyById(String taxonId, SparqlClient sparqlClient) throws IOException {
        final String normalizedTaxonId = StringUtils.replace(taxonId, TaxonomyProvider.OPEN_BIODIV.getIdPrefix(), "");
        String sparql = "PREFIX fabio: <http://purl.org/spar/fabio/>\n" +
                "PREFIX prism: <http://prismstandard.org/namespaces/basic/2.0/>\n" +
                "PREFIX doco: <http://purl.org/spar/doco/>\n" +
                "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                "select ?name ?rank ?id ?kingdom ?phylum ?class ?order ?family ?genus ?specificEpithet " +
                "where { {\n" +
                "    BIND(<http://openbiodiv.net/" + normalizedTaxonId + "> AS ?id). \n" +
                "     ?id <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://openbiodiv.net/TaxonomicNameUsage>.\n" +
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
                "}";

        final LabeledCSVParser parser = sparqlClient.query(sparql);
        Taxon taxon = null;
        while ((taxon == null
                || StringUtils.isBlank(taxon.getPathNames()))
                && parser.getLine() != null) {
            taxon = parseTaxon(parser);
        }
        return taxon;
    }

    private static TaxonImpl parseTaxon(LabeledCSVParser parser) {
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

    private static Map<String, String> parseTableReferences(final JsonNode biodivTable, SparqlClient sparqlClient) throws IOException {
        final String table_id = biodivTable.has("table_id") ? biodivTable.get("table_id").asText() : "";
        final String referenceUrl = StringUtils.replaceAll(table_id, "[<>]", "");
        final String doi = biodivTable.has("article_doi") ? biodivTable.get("article_doi").asText() : "";
        final String citation = findCitationByDoi(doi, sparqlClient);
        return new TreeMap<String, String>() {
            {
                put(DatasetImporterForTSV.REFERENCE_ID, referenceUrl);
                put(DatasetImporterForTSV.REFERENCE_URL, referenceUrl);
                put(DatasetImporterForTSV.REFERENCE_DOI, doi);
                put(DatasetImporterForTSV.REFERENCE_CITATION, citation);
                put("tableCaption", biodivTable.has("caption") ? biodivTable.get("caption").asText() : "");
            }
        };
    }


    public SparqlClientFactory getSparqlClientFactory() {
        return sparqlClientFactory;
    }

    public void setSparqlClientFactory(SparqlClientFactory sparqlClientFactory) {
        this.sparqlClientFactory = sparqlClientFactory;
    }

}
