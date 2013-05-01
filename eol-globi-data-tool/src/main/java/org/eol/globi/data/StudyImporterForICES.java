package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.Relationship;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;

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

        Study study = nodeFactory.createStudy("ICES",
                "<a href=\"http://ecosystemdata.ices.dk/stomachdata/\">ICES Stomach Dataset, ICES, Copenhagen</a>",
                "International Council for the Exploration of the Sea (ICES); Institute for Marine Resources & Ecosystem Studies (IMARES)",
                "1980- 1991",
                "<a href=\"http://www.ices.dk/products/cooperative.asp\">ICES Cooperative Research Report No. 164</a>; <a href=\"http://ices.dk/products/cooperative.asp\">ICES Cooperative Research Report No. 219</a>", null);
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
        predatorSpecimen = nodeFactory.createSpecimen(parser.getValueByLabel("Predator"));
        predatorSpecimen.setLengthInMm(parseDoubleField(parser, "Predator (mean) Lengh"));

        addLocation(parser, predatorSpecimen);
        addCollectedAt(parser, study, predatorSpecimen);
        return predatorSpecimen;
    }

    private LabeledCSVParser createParser() throws StudyImporterException {
        LabeledCSVParser parser = null;
        try {
            parser = parserFactory.createParser("ices/StomachDataSet.csv.gz", CharsetConstant.UTF8);
        } catch (IOException e) {
            throw new StudyImporterException("failed to access datasource", e);
        }
        return parser;
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
        Specimen preySpecimen = nodeFactory.createSpecimen(preyName);
        predatorSpecimen.ate(preySpecimen);
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
