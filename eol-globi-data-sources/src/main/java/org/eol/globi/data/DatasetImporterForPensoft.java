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
import org.eol.globi.domain.Term;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;
import org.globalbioticinteractions.pensoft.AddColumnFromCaption;
import org.globalbioticinteractions.pensoft.AddColumnsForOpenBiodivTerms;
import org.globalbioticinteractions.pensoft.ExpandColumnSpans;
import org.globalbioticinteractions.pensoft.ExpandRowSpans;
import org.globalbioticinteractions.pensoft.TablePreprocessor;
import org.globalbioticinteractions.pensoft.TableRectifier;
import org.globalbioticinteractions.pensoft.TableUtil;
import org.globalbioticinteractions.util.SparqlClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
            logger.warn(LogUtil.contextFor(tableReferences), "pre-processed Pensoft table is not rectangular.");
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
                listener.on(link);

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

    static String findCitationByDoi(String doi, SparqlClient openBiodivClient) throws IOException {
        String bindStatement = "    BIND(\"" + doi + "\" AS ?doi). \n";
        return findCitation(openBiodivClient, bindStatement);
    }

    static String findCitationById(String articleId, SparqlClient openBiodivClient) throws IOException {
        String articleURI = StringUtils.replacePattern(articleId, "[<>]", "");
        String bindStatement = "    BIND(<" + articleURI + "> AS ?article). \n";
        return findCitation(openBiodivClient, bindStatement);
    }

    private static String findCitation(SparqlClient openBiodivClient, String bindStatement) throws IOException {
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
                bindStatement +
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
            String citation = null;
            final LabeledCSVParser parser = openBiodivClient.query(sparql);
            parser.getLine();
            String doi = parser.getValueByLabel("doi");
            if (StringUtils.isNotBlank(doi)) {
                final String doiURIString = DOI.create(doi).toURI().toString();
                citation = StringUtils.join(Arrays.asList(
                        parser.getValueByLabel("authorsList"),
                        parser.getValueByLabel("pubYear"),
                        parser.getValueByLabel("title"),
                        parser.getValueByLabel("journalName"),
                        doiURIString), ". ");
            }
            return citation;
        } catch (MalformedDOIException e) {
            throw new IOException("marlformed uri", e);
        }
    }


    private static Map<String, String> parseTableReferences(final JsonNode biodivTable, SparqlClient sparqlClient) throws IOException {
        final String tableURI = biodivTable.has("table_id") ? biodivTable.get("table_id").asText() : "";
        final String referenceUrl = StringUtils.replaceAll(tableURI, "[<>]", "");
        final String doiString = biodivTable.has("article_doi") ? biodivTable.get("article_doi").asText() : "";
        final String articleURI = biodivTable.has("article_id") ? biodivTable.get("article_id").asText() : "";
        if (StringUtils.isBlank(articleURI)) {
            throw new IOException("missing mandatory articleURI for table with id [" + tableURI + "]");
        }
        String citation = findCitationById(articleURI, sparqlClient);
        if (StringUtils.isBlank(citation)) {
            citation = findCitationByDoi(doiString, sparqlClient);
        }
        final String finalCitation = citation;

        TreeMap<String, String> references = new TreeMap<String, String>() {
            {
                put(DatasetImporterForTSV.REFERENCE_ID, tableURI);
                put(DatasetImporterForTSV.REFERENCE_URL, tableURI);
                put(DatasetImporterForTSV.REFERENCE_CITATION, StringUtils.isBlank(finalCitation) ? referenceUrl : finalCitation);
                put("tableCaption", biodivTable.has("caption") ? biodivTable.get("caption").asText() : "");
            }
        };

        addDOIReferenceIfAvailable(doiString, references);

        return references;
    }

    private static void addDOIReferenceIfAvailable(String doiString, TreeMap<String, String> references) {
        try {
            final DOI doiObj = DOI.create(doiString);
            references.put(DatasetImporterForTSV.REFERENCE_DOI, doiString);
            references.put(DatasetImporterForTSV.REFERENCE_URL, doiObj.toURI().toString());
        } catch (MalformedDOIException e) {
            // ignore
        }
    }


    public SparqlClientFactory getSparqlClientFactory() {
        return sparqlClientFactory;
    }

    public void setSparqlClientFactory(SparqlClientFactory sparqlClientFactory) {
        this.sparqlClientFactory = sparqlClientFactory;
    }

}
