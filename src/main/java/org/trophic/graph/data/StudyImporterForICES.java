package org.trophic.graph.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.Relationship;
import org.trophic.graph.domain.Location;
import org.trophic.graph.domain.Specimen;
import org.trophic.graph.domain.Study;
import org.trophic.graph.domain.Taxon;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class StudyImporterForICES extends BaseStudyImporter {

    public StudyImporterForICES(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        LabeledCSVParser parser = createParser();

        Study study = nodeFactory.createStudy(StudyLibrary.Study.ICES.toString());
        try {
            Specimen predatorSpecimen = null;
            String lastStomachId = null;
            while ((parser.getLine()) != null) {
                if (importFilter.shouldImportRecord((long) parser.getLastLineNumber())) {
                    String currentStomachId = parser.getValueByLabel("ICES StomachID");
                    if (lastStomachId == null || !lastStomachId.equals(currentStomachId)) {
                        predatorSpecimen = addPredator(parser, study);
                    }

                    addPrey(parser, predatorSpecimen);
                    lastStomachId = currentStomachId;
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("problem parsing datasource", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("problem parsing datasource", e);
        }

        return study;
    }

    private void addPrey(LabeledCSVParser parser, Specimen predatorSpecimen) throws NodeFactoryException {
        String preyName = parser.getValueByLabel("Prey Species Name");
        if (StringUtils.isNotBlank(preyName)) {
            atePrey(predatorSpecimen, preyName);
        }
    }

    private Specimen addPredator(LabeledCSVParser parser, Study study) throws NodeFactoryException, StudyImporterException {
        Specimen predatorSpecimen;
        predatorSpecimen = nodeFactory.createSpecimen();
        predatorSpecimen.setLengthInMm(parseDoubleField(parser, "Predator (mean) Lengh"));

        addLocation(parser, predatorSpecimen);
        classifyPredator(parser, predatorSpecimen);
        addCollectedAt(parser, study, predatorSpecimen);
        return predatorSpecimen;
    }

    private LabeledCSVParser createParser() throws StudyImporterException {
        LabeledCSVParser parser = null;
        try {
            parser = parserFactory.createParser("ices/StomachDataSet.csv.gz");
        } catch (IOException e) {
            throw new StudyImporterException("failed to access datasource", e);
        }
        return parser;
    }

    private void classifyPredator(LabeledCSVParser parser, Specimen predatorSpecimen) throws NodeFactoryException {
        String predatorName = parser.getValueByLabel("Predator");
        Taxon predatorTaxon = nodeFactory.getOrCreateTaxon(predatorName);
        predatorSpecimen.classifyAs(predatorTaxon);
    }

    private void addCollectedAt(LabeledCSVParser parser, Study study, Specimen predatorSpecimen) throws StudyImporterException {
        Relationship collected = study.collected(predatorSpecimen);
        String dateTime = parser.getValueByLabel("Date/Time");
        try {
            nodeFactory.setUnixEpochProperty(collected, new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse(dateTime));
        } catch (ParseException e) {
            throw new StudyImporterException("missing or invalid date value [" + dateTime + "]", e);
        }
    }

    private void addLocation(LabeledCSVParser parser, Specimen predatorSpecimen) {
        Double lat = parseDoubleField(parser, "Latitude");
        Double lon = parseDoubleField(parser, "Longitude");
        Double depth = parseDoubleField(parser, "Depth");
        Location loc = nodeFactory.getOrCreateLocation(lat, lon, depth == null ? null : -depth);
        predatorSpecimen.caughtIn(loc);
    }

    private void atePrey(Specimen predatorSpecimen, String preyName) throws NodeFactoryException {
        Taxon preyTaxon = nodeFactory.getOrCreateTaxon(preyName);
        if (preyTaxon != null) {
            Specimen preySpecimen = nodeFactory.createSpecimen();
            preySpecimen.classifyAs(preyTaxon);
            predatorSpecimen.ate(preySpecimen);
        }
    }

    private Double parseDoubleField(LabeledCSVParser parser, String name) {
        String LatString = parser.getValueByLabel(name);
        Double aDouble = null;
        if (StringUtils.isNotBlank(LatString)) {
            aDouble = Double.parseDouble(LatString);
        }
        return aDouble;
    }
}
