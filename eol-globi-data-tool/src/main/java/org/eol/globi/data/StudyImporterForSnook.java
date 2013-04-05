package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StudyImporterForSnook extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForSnook.class);

    public StudyImporterForSnook(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        Study study = nodeFactory.createStudy("Blewett2000CharlotteHarborFL", "David A. Blewett", "Fish and Wildlife Research Institute, Florida Fish and Wildlife Conservation Commission", "Mar 2000- Feb 2002", "Feeding Habits of Common Snook, Centropomus undecimalis, in Charlotte Harbor, Florida.");

        try {
            LabeledCSVParser locationParser = parserFactory.createParser("blewett/SnookDietData2000_02_Charlotte_Harbor_FL_Blewett_date_and_abiotic.csv.gz");
            Map<String, Location> locationMap = new HashMap<String, Location>();
            String[] header = locationParser.getLabels();
            String[] line = null;
            while ((line = locationParser.getLine()) != null) {
                if (line.length < 3) {
                    LOG.warn("line:" + locationParser.getLastLineNumber() + " [" + StringUtils.join(line, ",") + "] has missing location information");
                }
                String locationCode = line[0];
                if (StringUtils.isBlank(locationCode)) {
                    LOG.warn("blank location code for line:" + locationParser.getLastLineNumber());
                }
                String latitude = line[2];
                if (StringUtils.isBlank(latitude)) {
                    LOG.warn("blank value for lattitude for line:" + locationParser.getLastLineNumber());
                }
                String longitude = line[1];
                if (StringUtils.isBlank(longitude)) {
                    LOG.warn("blank value for longitude for line:" + locationParser.getLastLineNumber());
                }
                Location location = nodeFactory.getOrCreateLocation(Double.parseDouble(latitude), Double.parseDouble(longitude), 0.0);
                locationMap.put(locationCode, location);
            }
            parsePredatorPreyInteraction(study, locationMap);
        } catch (IOException e) {
            throw new StudyImporterException("failed to read resource", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create taxon", e);
        }


        return study;
    }

    private void parsePredatorPreyInteraction(Study study, Map<String, Location> locationMap) throws IOException, NodeFactoryException {
        LabeledCSVParser parser = parserFactory.createParser("blewett/SnookDietData2000_02_Charlotte_Harbor_FL_Blewett_numeric_abundance.csv.gz");
        String[] header = parser.getLabels();

        String[] line = null;

        while ((line = parser.getLine()) != null) {
            if (line.length < 2) {
                break;
            }

            String length = line[2];
            Specimen predatorSpecimen = nodeFactory.createSpecimen("Centropomus undecimalis");
            study.collected(predatorSpecimen);
            try {
                predatorSpecimen.setLengthInMm(Double.parseDouble(length));
            } catch (NumberFormatException ex) {
                LOG.warn("found malformed length in line:" + parser.lastLineNumber() + " [" + StringUtils.join(line, ",") + "]");
            }

            String locationCode = line[0];
            if (locationCode != null) {
                Location location = locationMap.get(locationCode.trim());
                if (location != null) {
                    predatorSpecimen.caughtIn(location);
                }
            }

            int preyColumn = 4;
            for (int i = preyColumn; i < header.length; i++) {
                if (i < line.length) {
                    String preyCountString = line[i];
                    if (preyCountString.trim().length() > 0) {
                        try {
                            int preyCount = Integer.parseInt(preyCountString);
                            String preyName = header[i];
                            for (int j = 0; j < preyCount; j++) {
                                Specimen preySpecimen = nodeFactory.createSpecimen(preyName);
                                predatorSpecimen.ate(preySpecimen);
                            }
                        } catch (NumberFormatException e) {
                            LOG.warn("failed to parse prey count line/column:");
                        }
                    }
                }
            }
        }
    }
}
