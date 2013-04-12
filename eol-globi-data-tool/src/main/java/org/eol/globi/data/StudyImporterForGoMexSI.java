package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StudyImporterForGoMexSI extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForGoMexSI.class);

    public StudyImporterForGoMexSI(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {

        Study study = nodeFactory.createStudy(StudyImporterFactory.Study.GOMEXSI.toString(),
                "James D. Simons",
                "Center for Coastal Studies, Texas A&M University - Corpus Christi, United States",
                "",
                "<a href=\"http://www.ingentaconnect.com/content/umrsmas/bullmar/2013/00000089/00000001/art00009\">Building a Fisheries Trophic Interaction Database for Management and Modeling Research in the Gulf of Mexico Large Marine Ecosystem.</a>");

        Map<String, Specimen> predatorIdToPredatorSpecimen = new HashMap<String, Specimen>();
        Map<String, Specimen> predatorIdToPreySpecimen = new HashMap<String, Specimen>();
        addSpecimen(predatorIdToPredatorSpecimen, "gomexsi/Predators.csv", "PRED_SCI_NAME");
        addSpecimen(predatorIdToPreySpecimen, "gomexsi/Prey.csv", "DATABASE_PREY_NAME");

        for (Map.Entry<String, Specimen> predatorIdPredatorSpecimen : predatorIdToPredatorSpecimen.entrySet()) {
            Specimen predatorSpecimen = predatorIdPredatorSpecimen.getValue();
            study.collected(predatorSpecimen);
            Specimen specimen = predatorIdToPreySpecimen.get(predatorIdPredatorSpecimen.getKey());
            if (specimen != null) {
                predatorSpecimen.ate(specimen);
            }
        }

        addLocations(predatorIdToPredatorSpecimen);

        return study;
    }

    private void addLocations(Map<String, Specimen> predatorIdToPredatorSpecimen) throws StudyImporterException {
        String locationResource = "gomexsi/Locations.csv";
        try {
            LabeledCSVParser parser = parserFactory.createParser(locationResource, CharsetConstant.UTF8);
            while (parser.getLine() != null) {
                String refId = getMandatoryValue(locationResource, parser, "REF_ID");
                String specimenId = getMandatoryValue(locationResource, parser, "PRED_ID");
                Double latitude = getMandatoryDoubleValue(locationResource, parser, "LOC_CENTR_LAT");
                Double longitude = getMandatoryDoubleValue(locationResource, parser, "LOC_CENTR_LONG");
                Double depth = getMandatoryDoubleValue(locationResource, parser, "MN_DEP_SAMP");
                Location location = nodeFactory.getOrCreateLocation(latitude, longitude, depth == null ? null : -depth);
                Specimen specimen = predatorIdToPredatorSpecimen.get(refId + specimenId);
                if (specimen == null) {
                    LOG.warn("failed to lookup location for predator [" + refId + ":" + specimenId + "] from [" + locationResource + ":" + parser.getLastLineNumber() + "]");
                } else {
                    specimen.caughtIn(location);
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to open resource [" + locationResource + "]", e);
        }
    }

    private Double getMandatoryDoubleValue(String locationResource, LabeledCSVParser parser, String label) throws StudyImporterException {
        String lat = getMandatoryValue(locationResource, parser, label);
        try {
            return "NA".equals(lat) ? null : Double.parseDouble(lat);
        } catch (NumberFormatException ex) {
            throw new StudyImporterException("failed to parse [" + label + "]", ex);
        }
    }

    private void addSpecimen(Map<String, Specimen> specimenMap, String datafile, String scientificNameLabel) throws StudyImporterException {
        try {
            LabeledCSVParser parser = parserFactory.createParser(datafile, CharsetConstant.UTF8);
            while (parser.getLine() != null) {
                String refId = getMandatoryValue(datafile, parser, "REF_ID");
                String specimenId = getMandatoryValue(datafile, parser, "PRED_ID");
                String scientificName = getMandatoryValue(datafile, parser, scientificNameLabel);
                Specimen specimen = nodeFactory.createSpecimen(scientificName);
                specimenMap.put(refId + specimenId, specimen);
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to open resource [" + datafile + "]", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create specimen", e);
        }
    }

    private String getMandatoryValue(String datafile, LabeledCSVParser parser, String label) throws StudyImporterException {
        String value = parser.getValueByLabel(label);
        if (value == null) {
            throw new StudyImporterException("missing mandatory column [" + label + "] in [" + datafile + "]:[" + parser.getLastLineNumber() + "]");
        }
        return value;
    }
}
