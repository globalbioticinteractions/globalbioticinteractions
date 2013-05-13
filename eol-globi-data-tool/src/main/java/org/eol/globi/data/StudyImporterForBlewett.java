package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class StudyImporterForBlewett extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForBlewett.class);
    public static final String COLLECTION_NO = "Collection #";

    public StudyImporterForBlewett(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        Study study = nodeFactory.createStudy("Blewett 2006", "David A. Blewett", "Fish and Wildlife Research Institute, Florida Fish and Wildlife Conservation Commission", "Mar 2000- Feb 2002", "<a href=\"http://research.myfwc.com/engine/download_redirection_process.asp?file=06blewett_0718.pdf&objid=50963&dltype=publication\">Feeding Habits of Common Snook, Centropomus undecimalis, in Charlotte Harbor, Florida</a>.", "2006");

        try {
            Map<String, Location> collectionLocationMap = new HashMap<String, Location>();
            Map<String, Date> collectionTimeMap = new HashMap<String, Date>();
            buildLocationTimeMaps(collectionLocationMap, collectionTimeMap);
            parsePredatorPreyInteraction(study, collectionLocationMap, collectionTimeMap);
        } catch (IOException e) {
            throw new StudyImporterException("failed to read resource", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create taxon", e);
        }


        return study;
    }

    private void buildLocationTimeMaps(Map<String, Location> collectionLocationMap, Map<String, Date> collectionTimeMap) throws IOException, StudyImporterException {
        LabeledCSVParser locationParser = parserFactory.createParser("blewett/SnookDietData2000_02_Charlotte_Harbor_FL_Blewett_date_and_abiotic.csv", CharsetConstant.UTF8);
        String[] line;
        while ((line = locationParser.getLine()) != null) {
            if (line.length < 3) {
                LOG.warn("line:" + locationParser.getLastLineNumber() + " [" + StringUtils.join(line, ",") + "] has missing location information");
            }
            String collectionCode = locationParser.getValueByLabel(COLLECTION_NO);
            if (StringUtils.isBlank(collectionCode)) {
                LOG.warn("blank location code for line: [" + locationParser.getLastLineNumber() + "]");
            }
            String latitude = locationParser.getValueByLabel("Latitude");
            if (StringUtils.isBlank(latitude)) {
                LOG.warn("blank value for lattitude for line: [" + locationParser.getLastLineNumber() + "]");
            }
            String longitude = locationParser.getValueByLabel("Longitude");
            if (StringUtils.isBlank(longitude)) {
                LOG.warn("blank value for longitude for line: [" + locationParser.getLastLineNumber() + "]");
            }
            Location location = nodeFactory.getOrCreateLocation(Double.parseDouble(latitude), Double.parseDouble(longitude), 0.0);
            collectionLocationMap.put(collectionCode, location);

            String timeString = locationParser.getValueByLabel("Time");
            if (StringUtils.isBlank(timeString)) {
                LOG.warn("blank value for time for line: [" + locationParser.getLastLineNumber() + "]");
            }

            String dateString = locationParser.getValueByLabel("Date");
            if (StringUtils.isBlank(dateString)) {
                LOG.warn("blank value for date for line: [" + locationParser.getLastLineNumber() + "]");
            }

            String dateTimeString =  dateString + " " + timeString;
            try {
                Date dateTime = parseDateString(dateTimeString);
                collectionTimeMap.put(collectionCode, dateTime);
            } catch (ParseException e) {
                throw new StudyImporterException("failed to parse date time [" + dateTimeString + "] for collection [" + collectionCode + "] on line [" + locationParser.getLastLineNumber() + "]");
            }
        }
    }

    static Date parseDateString(String dateTimeString) throws ParseException {
        DateTimeFormatter fmtDateTime1 = DateTimeFormat.forPattern("dd-MMM-yy HH:mm:ss");
        fmtDateTime1 = fmtDateTime1.withZone(DateTimeZone.forID("US/Central"));
        DateTimeFormatter fmtDateTime = fmtDateTime1;
        DateTime dateTimeWithZone = new DateTime(fmtDateTime.parseDateTime(dateTimeString));
        return dateTimeWithZone.toDate();
    }

    static String dateToString(Date time) {
        DateTime dateTime = new DateTime(time);
        DateTimeFormatter fmtDateTime = DateTimeFormat.forPattern("dd-MMM-yy HH:mm:ss zzzz");
        fmtDateTime = fmtDateTime.withZone(DateTimeZone.forID("US/Central"));
        return fmtDateTime.print(dateTime);
    }

    private void parsePredatorPreyInteraction(Study study, Map<String, Location> locationMap, Map<String, Date> collectionTimeMap) throws IOException, NodeFactoryException {
        LabeledCSVParser parser = parserFactory.createParser("blewett/SnookDietData2000_02_Charlotte_Harbor_FL_Blewett_numeric_abundance.csv", CharsetConstant.UTF8);
        String[] header = parser.getLabels();

        String[] line;
        while ((line = parser.getLine()) != null) {
            if (line.length < 2) {
                break;
            }
            Specimen predatorSpecimen = addPredator(study, locationMap, parser, line, collectionTimeMap);
            addPreyForPredator(header, line, predatorSpecimen);
        }
    }

    private Specimen addPredator(Study study, Map<String, Location> locationMap, LabeledCSVParser parser, String[] line, Map<String, Date> collectionTimeMap) throws NodeFactoryException {
        Specimen predatorSpecimen = nodeFactory.createSpecimen("Centropomus undecimalis");
        predatorSpecimen.setLifeStage(LifeStage.ADULT);
        Relationship collectedRel = study.collected(predatorSpecimen);
        try {
            String length = parser.getValueByLabel("Standard Length");
            predatorSpecimen.setLengthInMm(Double.parseDouble(length));
        } catch (NumberFormatException ex) {
            LOG.warn("found malformed length in line:" + parser.lastLineNumber() + " [" + StringUtils.join(line, ",") + "]");
        }

        String collectionCode = parser.getValueByLabel(COLLECTION_NO);
        if (collectionCode != null) {
            String collectionCodeTrim = collectionCode.trim();
            Location location = locationMap.get(collectionCodeTrim);
            if (location != null) {
                predatorSpecimen.caughtIn(location);
            }

            Date collectionDatetime = collectionTimeMap.get(collectionCodeTrim);
            if (collectionDatetime != null) {
                nodeFactory.setUnixEpochProperty(collectedRel, collectionDatetime);
            }
        }
        return predatorSpecimen;
    }

    private void addPreyForPredator(String[] header, String[] line, Specimen predatorSpecimen) throws NodeFactoryException {
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
