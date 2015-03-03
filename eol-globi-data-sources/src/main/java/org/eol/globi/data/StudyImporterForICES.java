package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.util.ExternalIdUtil;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StudyImporterForICES extends BaseStudyImporter {

    public StudyImporterForICES(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        LabeledCSVParser parser = createParser();

        Study study = nodeFactory.getOrCreateStudy("ICES",
                "International Council for the Exploration of the Sea. Available at http://www.ices.dk/products/cooperative.asp .", ExternalIdUtil.toCitation("<a href=\"http://ecosystemdata.ices.dk/stomachdata/\">ICES Stomach Dataset, ICES, Copenhagen</a>", "<a href=\"http://www.ices.dk/products/cooperative.asp\">ICES Cooperative Research Report No. 164</a>; <a href=\"http://ices.dk/products/cooperative.asp\">ICES Cooperative Research Report No. 219</a>", null));
        study.setExternalId("http://ecosystemdata.ices.dk/stomachdata/");
        try {
            Specimen predator = null;
            String lastStomachId = null;
            while ((parser.getLine()) != null) {
                if (importFilter.shouldImportRecord((long) parser.getLastLineNumber())) {
                    Date date = parseDate(parser);
                    Location location = parseLocation(parser);

                    String currentStomachId = parser.getValueByLabel("ICES StomachID");
                    if (lastStomachId == null || !lastStomachId.equals(currentStomachId)) {
                        predator = addPredator(parser, study);
                        nodeFactory.setUnixEpochProperty(predator, date);
                        predator.caughtIn(location);
                    }

                    Specimen prey = addPrey(parser, predator, study);
                    if (prey != null) {
                        nodeFactory.setUnixEpochProperty(prey, date);
                        prey.caughtIn(location);
                    }
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

    private Specimen addPrey(LabeledCSVParser parser, Specimen predatorSpecimen, Study study) throws NodeFactoryException {
        String preyName = parser.getValueByLabel("Prey Species Name");
        Specimen specimen = null;
        if (StringUtils.isNotBlank(preyName)) {
            specimen = atePrey(predatorSpecimen, preyName, study);
        }
        return specimen;
    }

    private Specimen addPredator(LabeledCSVParser parser, Study study) throws NodeFactoryException, StudyImporterException {
        Specimen predatorSpecimen;
        predatorSpecimen = nodeFactory.createSpecimen(study, parser.getValueByLabel("Predator"));
        predatorSpecimen.setLengthInMm(parseDoubleField(parser, "Predator (mean) Lengh"));
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

    private Date parseDate(LabeledCSVParser parser) throws StudyImporterException {
        String dateTime = parser.getValueByLabel("Date/Time");
        Date date;
        try {
            date = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse(dateTime);
        } catch (ParseException e) {
            throw new StudyImporterException("missing or invalid date value [" + dateTime + "]", e);
        }

        return date;
    }

    private Location parseLocation(LabeledCSVParser parser) throws StudyImporterException {
        Double lat = parseDoubleField(parser, "Latitude");
        Double lon = parseDoubleField(parser, "Longitude");
        Double depth = parseDoubleField(parser, "Depth");
        try {
            return nodeFactory.getOrCreateLocation(lat, lon, depth == null ? null : -depth);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create location", e);
        }
    }

    private Specimen atePrey(Specimen predatorSpecimen, String preyName, Study study) throws NodeFactoryException {
        Specimen preySpecimen = nodeFactory.createSpecimen(study, preyName);
        predatorSpecimen.ate(preySpecimen);
        return preySpecimen;
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
