package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.SelectorImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.util.CSVTSVUtil;
import org.mapdb.DBMaker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeMap;

public class StudyImporterForSaproxylic extends BaseStudyImporter {
    private static final String KINGDOM = "Kingdom";
    private static final String PHYLUM = "Phylum";
    private static final String CLASS = "Class";
    private static final String ORDER = "Order";
    private static final String SUB_ORDER = "SubOrder";
    private static final String FAMILY = "Family";
    private static final String GENUS = "Genus";
    private static final String SPECIES = "Species";

    public static final String CLASSIFIED_AS = "classifiedAs";

    public static final String IN_STAGE = "inStage";
    public static final String FOUND_AT = "foundAt";
    public static final String MENTIONED_BY = "mentioned_by";
    public static final String PARTICIPATES_IN = "participates_in";
    public static final String HAS_NAME = "hasName";
    public static final String HAS_RANK = "hasRank";
    public static final String HAS_PATH_IDS = "hasPathIds";
    public static final String HAS_PATH_NAMES = "hasPathNames";

    public StudyImporterForSaproxylic(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        try {
            final Model model = ModelFactory.createDefaultModel();

            NavigableSet<Triple<String, String, String>> triples = DBMaker.newTempTreeSet();
            Tripler add = triple -> {
                Resource resource = model.createResource("sx:" + triple.getLeft());
                Property property = model.createProperty("sx:" + triple.getMiddle());
                String obj = triple.getRight();
                if (StringUtils.startsWith(obj, "{")) {
                    Resource food = model.createResource("sx:" + obj);
                    model.add(resource, property, food);
                } else if (StringUtils.startsWith(obj, "http")) {
                    model.add(resource, property, model.createResource(obj));
                } else if (StringUtils.isNotBlank(obj)) {
                    model.add(resource, property, obj);
                }
            };
            parseReferences(add, getDataset().getResource("sx_txt/Reference.txt"));
            parseLocalities(add, getDataset().getResource("sx_txt/Locality.txt"));
            parseTaxa(add, getDataset().getResource("sx_txt/Taxon.txt"));
            parseTaxonRanks(add, getDataset().getResource("sx_txt/TaxonRank.txt"));
            parseOccurrences(add, getDataset().getResource("sx_txt/Occurrence.txt"));
            parseAssociations(add, getDataset().getResource("sx_txt/SX_Association.txt"));
            parseInteractionTypeMap(add, getDataset().getResource("interaction_type_map.tsv"));

            // associations -> occurrences, taxa, reference
            String queryString =
                            "SELECT ?sourceTaxonName ?sourceLifeStage ?interactionTypeId ?targetTaxonName ?targetLifeStage ?referenceCitation ?localityName ?studyTitle " +
                            "WHERE {" +
                            "      ?interaction <sx:mentioned_by> ?studyTitle . " +
                            "      ?studyTitle <sx:hasName> ?referenceCitation . " +
                            "      ?sourceSpecimen <sx:participates_in> ?interaction . " +
                            "      ?targetSpecimen <sx:participates_in> ?interaction . " +

                            "      ?sourceSpecimen ?inter ?targetSpecimen . " +
                            "      ?inter <sx:equivalentTo> ?interactionTypeId . " +

                            "      ?sourceSpecimen <sx:classifiedAs> ?sourceTaxon . " +
                            "      ?sourceTaxon <sx:hasName> ?sourceTaxonName . " +

                            "      ?targetSpecimen <sx:classifiedAs> ?targetTaxon . " +
                            "      ?targetTaxon <sx:hasName> ?targetTaxonName . " +

                            "      ?sourceSpecimen <sx:inStage> ?sourceLifeStage . " +
                            "      ?targetSpecimen <sx:inStage> ?targetLifeStage . " +

                            "      ?targetSpecimen <sx:foundAt> ?locality . " +
                            "      ?locality <sx:hasName> ?localityName . " +
                            "}";

            Query query = QueryFactory.create(queryString);
            QueryExecution qe = QueryExecutionFactory.create(query, model);
            ResultSet results = qe.execSelect();

            toInteractions(results);
            qe.close();
        } catch (IOException e) {
            throw new StudyImporterException("failed to access resource", e);
        }
    }

    public void toInteractions(ResultSet results) throws StudyImporterException {
        final InteractionListener listener = new InteractionListenerImpl(nodeFactory, getGeoNamesService(), getLogger());
        while (results.hasNext()) {
            QuerySolution next = results.next();
            Iterator<String> nameIter = next.varNames();
            Map<String, String> props = new TreeMap<>();
            while (nameIter.hasNext()) {
                String key = nameIter.next();
                RDFNode rdfNode = next.get(key);
                if (rdfNode.isURIResource()) {
                    props.put(key, next.getResource(key).getURI());
                } else {
                    props.put(key, next.getLiteral(key).getString());
                }
            }
            props.put(StudyImporterForTSV.STUDY_SOURCE_CITATION, getDataset().getCitation());
            listener.newLink(props);
        }
    }


    private static void handleLines(LineListener listener, InputStream is) throws IOException, StudyImporterException {
        LabeledCSVParser parser = CSVTSVUtil.createLabeledTSVParser(is);
        while (parser.getLine() != null) {
            listener.onLine(parser);
        }
    }

    static ImmutableTriple<String, String, String> asTriple(String subj, String verb, String obj) {
        return new ImmutableTriple<>(subj, verb, obj);
    }

    static void parseTaxa(Tripler tripler, InputStream is) throws IOException, StudyImporterException {
        handleLines(parser -> {
            String taxonId = parser.getValueByLabel("Oid");
            String kingdomId = parser.getValueByLabel(KINGDOM);
            String phylumId = parser.getValueByLabel(PHYLUM);
            String classId = parser.getValueByLabel(CLASS);
            String orderId = parser.getValueByLabel(ORDER);
            String subOrderId = parser.getValueByLabel(SUB_ORDER);
            String familyId = parser.getValueByLabel(FAMILY);
            String genusId = parser.getValueByLabel(GENUS);
            String speciesId = parser.getValueByLabel(SPECIES);
            String rankId = parser.getValueByLabel("Rank");
            String nameString = parser.getValueByLabel("FullName");

            List<String> ts = Arrays.asList(kingdomId, phylumId, classId, orderId, subOrderId, familyId, genusId, speciesId);
            String pathIds = StringUtils.join(ts, CharsetConstant.SEPARATOR);
            List<String> pathNames = Arrays.asList(KINGDOM, PHYLUM, CLASS, ORDER, SUB_ORDER, FAMILY, GENUS, SPECIES);
            String pathNamesJoined = StringUtils.join(pathNames, CharsetConstant.SEPARATOR);
            tripler.on(asTriple(taxonId, HAS_NAME, nameString));
            tripler.on(asTriple(taxonId, HAS_RANK, rankId));
            tripler.on(asTriple(taxonId, HAS_PATH_IDS, pathIds));
            tripler.on(asTriple(taxonId, HAS_PATH_NAMES, pathNamesJoined));
        }, is);
    }

    static void parseLocalities(Tripler tripler, InputStream is) throws IOException, StudyImporterException {
        handleLines(parser -> {
            String localityId = parser.getValueByLabel("Oid");
            String name = parser.getValueByLabel("Name");
            tripler.on(asTriple(localityId, HAS_NAME, name));
        }, is);
    }

    static void parseReferences(Tripler tripler, InputStream is) throws IOException, StudyImporterException {
        handleLines(parser -> {
            String referenceId = parser.getValueByLabel("Oid");
            List<String> citation = new ArrayList<>();

            appendIfNotBlank(parser, citation, "", "Authors");
            appendIfNotBlank(parser, citation, "", "Year");
            appendIfNotBlank(parser, citation, "", "Title");
            appendIfNotBlank(parser, citation, "vol ", "Volume");
            appendIfNotBlank(parser, citation, "issue ", "Issue");
            appendIfNotBlank(parser, citation, "p ", "Page1");
            appendIfNotBlank(parser, citation, "", "Jourfull");
            String citationJoined = StringUtils.join(citation, ". ");
            tripler.on(asTriple(referenceId, HAS_NAME, citationJoined));
        }, is);
    }

    private static void appendIfNotBlank(LabeledCSVParser parser, List<String> citation, String prefix, String label) {
        String value = parser.getValueByLabel(label);
        if (StringUtils.isNotBlank(value)) {
            citation.add(prefix + value);
        }
    }

    static void parseTaxonRanks(Tripler tripler, InputStream is) throws IOException, StudyImporterException {
        LineListener lineListener = parser -> {
            String rankId = parser.getValueByLabel("Oid");
            String rankName = parser.getValueByLabel("Name");
            tripler.on(new ImmutableTriple<>(rankId, "hasName", rankName));
        };
        handleLines(lineListener, is);
    }

    static void parseInteractionTypeMap(Tripler tripler, InputStream is) throws IOException, StudyImporterException {
        handleLines(parser -> {
            String sourceInteractionId = parser.getValueByLabel("sourceInteractionId");
            String targetInteractionId = parser.getValueByLabel("targetInteractionId");
            tripler.on(asTriple(sourceInteractionId, "equivalentTo", targetInteractionId));
        }, is);
    }

    static void parseAssociations(Tripler tripler, InputStream is) throws IOException, StudyImporterException {
        LineListener listener = parser -> {
            String sourceOccurrenceId = parser.getValueByLabel("OccurrenceA");
            String targetOccurrenceId = parser.getValueByLabel("OccurrenceB");
            String interactionTypeIdAB = parser.getValueByLabel("RoleA");
            String interactionTypeIdBA = parser.getValueByLabel("RoleB");
            tripler.on(asTriple(sourceOccurrenceId, interactionTypeIdAB, targetOccurrenceId));
            tripler.on(asTriple(targetOccurrenceId, interactionTypeIdBA, sourceOccurrenceId));
            String interactionId = parser.getValueByLabel("Oid");
            tripler.on(asTriple(targetOccurrenceId, PARTICIPATES_IN, interactionId));
            tripler.on(asTriple(sourceOccurrenceId, PARTICIPATES_IN, interactionId));
            String referenceId = parser.getValueByLabel("Reference");
            tripler.on(asTriple(interactionId, MENTIONED_BY, referenceId));
        };
        handleLines(listener, is);
    }

    static void parseOccurrences(Tripler tripler, InputStream is) throws IOException, StudyImporterException {
        handleLines(parser -> {
            String occId = parser.getValueByLabel("Oid");
            tripler.on(asTriple(occId, CLASSIFIED_AS, parser.getValueByLabel("ExpertTaxon")));
            tripler.on(asTriple(occId, IN_STAGE, parser.getValueByLabel("Stage")));
            tripler.on(asTriple(occId, FOUND_AT, parser.getValueByLabel("Locality")));
        }, is);
    }


    interface LineListener {
        void onLine(LabeledCSVParser parser) throws StudyImporterException;
    }

    interface Tripler {
        void on(Triple<String, String, String> triple);
    }
}
