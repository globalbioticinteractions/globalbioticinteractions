package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

        Map<String, List<Specimen>> predatorIdToPredatorSpecimen = new HashMap<String, List<Specimen>>();
        Map<String, List<Specimen>> predatorIdToPreySpecimen = new HashMap<String, List<Specimen>>();
        addSpecimen(predatorIdToPredatorSpecimen, "gomexsi/Predators.csv", "PRED_SCI_NAME");
        addSpecimen(predatorIdToPreySpecimen, "gomexsi/Prey.csv", "DATABASE_PREY_NAME");

        for (Map.Entry<String, List<Specimen>> predatorIdPredatorSpecimen : predatorIdToPredatorSpecimen.entrySet()) {
            List<Specimen> predatorSpecimens = predatorIdPredatorSpecimen.getValue();
            for (Specimen predatorSpecimen : predatorSpecimens) {
                study.collected(predatorSpecimen);
                List<Specimen> preySpecimens = predatorIdToPreySpecimen.get(predatorIdPredatorSpecimen.getKey());
                if (preySpecimens != null) {
                    for (Specimen preySpecimen : preySpecimens) {
                        predatorSpecimen.ate(preySpecimen);
                    }
                }
            }

        }

        addLocations(predatorIdToPredatorSpecimen);

        return study;
    }

    private void addLocations(Map<String, List<Specimen>> predatorIdToPredatorSpecimen) throws StudyImporterException {
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
                List<Specimen> specimens = predatorIdToPredatorSpecimen.get(refId + specimenId);
                if (specimens == null) {
                    LOG.warn("failed to lookup location for predator [" + refId + ":" + specimenId + "] from [" + locationResource + ":" + parser.getLastLineNumber() + "]");
                } else {
                    for (Specimen specimen : specimens) {
                        specimen.caughtIn(location);
                    }
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

    private void addSpecimen(Map<String, List<Specimen>> specimenMap, String datafile, String scientificNameLabel) throws StudyImporterException {
        try {
            LabeledCSVParser parser = parserFactory.createParser(datafile, CharsetConstant.UTF8);
            while (parser.getLine() != null) {
                String refId = getMandatoryValue(datafile, parser, "REF_ID");
                String specimenId = getMandatoryValue(datafile, parser, "PRED_ID");
                String scientificName = getMandatoryValue(datafile, parser, scientificNameLabel);
                Specimen specimen = nodeFactory.createSpecimen(scientificName);
                String predatorUID = refId + specimenId;
                List<Specimen> specimenList = specimenMap.get(predatorUID);
                if (specimenList == null) {
                    specimenList = new ArrayList<Specimen>();
                }
                specimenList.add(specimen);
                specimenMap.put(predatorUID, specimenList);
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
