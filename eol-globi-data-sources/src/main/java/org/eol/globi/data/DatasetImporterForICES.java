package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.util.DateUtil;

import java.io.IOException;
import java.net.URI;
import java.util.Date;

public class DatasetImporterForICES extends NodeBasedImporter {

    public DatasetImporterForICES(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        LabeledCSVParser parser = createParser();


        Study study = getNodeFactory().getOrCreateStudy(
                new StudyImpl("ICES", null, "Cooperative Research Report No. 164; Cooperative Research Report No. 219, ICES Stomach DatasetImpl, ICES"));
        study.setExternalId("http://ecosystemdata.ices.dk/stomachdata/");
        try {
            Specimen predator = null;
            String lastStomachId = null;
            while ((parser.getLine()) != null) {
                setCurrentLine(parser.getLastLineNumber());
                if (importFilter.shouldImportRecord((long) parser.getLastLineNumber())) {
                    Date date = parseDate(parser);
                    Location location = parseLocation(parser);

                    String currentStomachId = parser.getValueByLabel("ICES StomachID");
                    if (lastStomachId == null || !lastStomachId.equals(currentStomachId)) {
                        predator = addPredator(parser, study);
                        getNodeFactory().setUnixEpochProperty(predator, date);
                        predator.caughtIn(location);
                    }

                    String preyName = parser.getValueByLabel("Prey Species Name");
                    Specimen prey = null;
                    if (StringUtils.isNotBlank(preyName)) {
                        prey = getNodeFactory().createSpecimen(study, new TaxonImpl(preyName, null));
                    }
                    if (prey != null) {
                        getNodeFactory().setUnixEpochProperty(prey, date);
                        prey.caughtIn(location);
                        predator.ate(prey);
                    }
                    lastStomachId = currentStomachId;
                }
            }
        } catch (IOException | NodeFactoryException e) {
            throw new StudyImporterException("problem parsing datasource", e);
        }

    }

    private Specimen addPredator(LabeledCSVParser parser, Study study) throws StudyImporterException {
        Specimen predatorSpecimen;
        predatorSpecimen = getNodeFactory().createSpecimen(study, new TaxonImpl(parser.getValueByLabel("Predator"), null));
        predatorSpecimen.setLengthInMm(parseDoubleField(parser, "Predator (mean) Lengh"));
        return predatorSpecimen;
    }

    private LabeledCSVParser createParser() throws StudyImporterException {
        LabeledCSVParser parser;
        try {
            URI resource = URI.create("ices/StomachDataSet.csv.gz");
            setCurrentResource(resource);
            parser = getParserFactory().createParser(resource, CharsetConstant.UTF8);
        } catch (IOException e) {
            throw new StudyImporterException("failed to access datasource", e);
        }
        return parser;
    }

    private Date parseDate(LabeledCSVParser parser) throws StudyImporterException {
        String dateTime = parser.getValueByLabel("Date/Time");
        Date date;
        try {
            date = DateUtil.parsePatternUTC(dateTime, "dd/MM/yyyy HH:mm:ss").toDate();
        } catch (IllegalArgumentException e) {
            throw new StudyImporterException("missing or invalid date value [" + dateTime + "]", e);
        }

        return date;
    }

    private Location parseLocation(LabeledCSVParser parser) throws StudyImporterException {
        Double lat = parseDoubleField(parser, "Latitude");
        Double lon = parseDoubleField(parser, "Longitude");
        Double depth = parseDoubleField(parser, "Depth");
        try {
            return getNodeFactory().getOrCreateLocation(new LocationImpl(lat, lon, depth == null ? null : -depth, null));
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create location", e);
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
