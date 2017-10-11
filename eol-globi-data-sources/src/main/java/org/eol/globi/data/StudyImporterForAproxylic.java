package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.eol.globi.util.CSVTSVUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StudyImporterForAproxylic extends BaseStudyImporter {


    private static final String KINGDOM = "Kingdom";
    private static final String PHYLUM = "Phylum";
    private static final String CLASS = "Class";
    private static final String ORDER = "Order";
    private static final String SUB_ORDER = "SubOrder";
    private static final String FAMILY = "Family";
    private static final String GENUS = "Genus";
    private static final String SPECIES = "Species";

    public StudyImporterForAproxylic(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    static void handleLines(LineListener listener, InputStream is) throws IOException, StudyImporterException {
        LabeledCSVParser parser = CSVTSVUtil.createLabeledTSVParser(is);
        while (parser.getLine() != null) {
            listener.onLine(parser);
        }
    }

    static ImmutableTriple<String, String, String> asTriple(String subj, String verb, String obj) {
        return new ImmutableTriple<>(subj, verb, obj);
    }

    static ImmutableTriple<String, String, String> triple(LabeledCSVParser parser, String occId) {
        return asTriple(occId, "foundAt", parser.getValueByLabel("Locality"));
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
            tripler.on(asTriple(taxonId, "hasName", nameString));
            tripler.on(asTriple(taxonId, "hasRank", rankId));
            tripler.on(asTriple(taxonId, "hasPathIds", pathIds));
            tripler.on(asTriple(taxonId, "hasPathNames", pathNamesJoined));
        }, is);
    }

    static void parseLocalities(Tripler tripler, InputStream is) throws IOException, StudyImporterException {
        handleLines(parser -> {
            String localityId = parser.getValueByLabel("Oid");
            String name = parser.getValueByLabel("Name");
            tripler.on(asTriple(localityId, "hasName", name));
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
            tripler.on(asTriple(referenceId, "hasName", citationJoined));
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

    @Override
    public void importStudy() throws StudyImporterException {
        try {
            getDataset().getResource("Reference.txt");
            getDataset().getResource("SX_Association.txt");
            getDataset().getResource("Taxon.txt");
            getDataset().getResource("TaxonRank.txt");
            getDataset().getResource("Occurrence.txt");
            getDataset().getResource("Locality.txt");
        } catch (IOException e) {
            throw new StudyImporterException("failed to access resource", e);
        }

    }

    interface LineListener {
        void onLine(LabeledCSVParser parser) throws StudyImporterException;
    }

    interface Tripler {
        void on(Triple<String, String, String> triple);
    }
}
