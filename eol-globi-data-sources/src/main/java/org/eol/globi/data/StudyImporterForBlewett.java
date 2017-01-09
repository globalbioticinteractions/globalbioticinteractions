package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.TermLookupServiceException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyImporterForBlewett extends BaseStudyImporter {
    public static final String COLLECTION_NO = "Collection #";

    public StudyImporterForBlewett(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        String citation = "Blewett DA, Hensley RA, and Stevens PW, Feeding Habits of Common Snook, Centropomus Undecimalis, in Charlotte Harbor, Florida, Gulf and Caribbean Research Vol 18, 1–13, 2006. doi:10.18785/gcr.1801.01 ";
        Study study = nodeFactory.getOrCreateStudy(
                new StudyImpl("Blewett 2006",
                        StudyImporterForGoMexSI2.GOMEXI_SOURCE_DESCRIPTION,
                        null,
                        citation));
        try {
            Map<String, Location> collectionLocationMap = new HashMap<>();
            Map<String, Date> collectionTimeMap = new HashMap<String, Date>();
            buildLocationTimeMaps(collectionLocationMap, collectionTimeMap, study);
            parsePredatorPreyInteraction(study, collectionLocationMap, collectionTimeMap);
        } catch (IOException e) {
            throw new StudyImporterException("failed to read resource", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create taxon", e);
        } catch (TermLookupServiceException e) {
            throw new StudyImporterException("failed to map terms", e);
        }
    }

    private void buildLocationTimeMaps(Map<String, Location> collectionLocationMap, Map<String, Date> collectionTimeMap, Study study) throws IOException, StudyImporterException {
        LabeledCSVParser locationParser = parserFactory.createParser("blewett/SnookDietData2000_02_Charlotte_Harbor_FL_Blewett_date_and_abiotic.csv", CharsetConstant.UTF8);
        String[] line;
        while ((line = locationParser.getLine()) != null) {
            if (line.length < 3) {
                getLogger().warn(study, "line:" + locationParser.getLastLineNumber() + " [" + StringUtils.join(line, ",") + "] has missing location information");
            }
            String collectionCode = locationParser.getValueByLabel(COLLECTION_NO);
            if (StringUtils.isBlank(collectionCode)) {
                getLogger().warn(study, "blank location code for line: [" + locationParser.getLastLineNumber() + "]");
            }
            String latitude = locationParser.getValueByLabel("Latitude");
            if (StringUtils.isBlank(latitude)) {
                getLogger().warn(study, "blank value for lattitude for line: [" + locationParser.getLastLineNumber() + "]");
            }
            String longitude = locationParser.getValueByLabel("Longitude");
            if (StringUtils.isBlank(longitude)) {
                getLogger().warn(study, "blank value for longitude for line: [" + locationParser.getLastLineNumber() + "]");
            }
            Location location;
            try {
                location = nodeFactory.getOrCreateLocation(new LocationImpl(Double.parseDouble(latitude), Double.parseDouble(longitude), 0.0, null));
            } catch (NodeFactoryException e) {
                throw new StudyImporterException("failed to create location", e);
            }
            collectionLocationMap.put(collectionCode, location);

            String timeString = locationParser.getValueByLabel("Time");
            if (StringUtils.isBlank(timeString)) {
                getLogger().warn(study, "blank value for time for line: [" + locationParser.getLastLineNumber() + "]");
            }

            String dateString = locationParser.getValueByLabel("Date");
            if (StringUtils.isBlank(dateString)) {
                getLogger().warn(study, "blank value for date for line: [" + locationParser.getLastLineNumber() + "]");
            }

            String dateTimeString = dateString + " " + timeString;
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

    private void parsePredatorPreyInteraction(Study study, Map<String, Location> locationMap, Map<String, Date> collectionTimeMap) throws IOException, NodeFactoryException, TermLookupServiceException {
        LabeledCSVParser parser = parserFactory.createParser("blewett/SnookDietData2000_02_Charlotte_Harbor_FL_Blewett_numeric_abundance.csv", CharsetConstant.UTF8);
        String[] header = parser.getLabels();

        String[] line;
        while ((line = parser.getLine()) != null) {
            if (line.length < 2) {
                break;
            }
            Specimen predatorSpecimen = addPredator(study, parser, line);
            List<Specimen> specimen = addPreyForPredator(header, line, study);
            for (Specimen prey : specimen) {
                predatorSpecimen.ate(prey);
            }

            String collectionCode = parser.getValueByLabel(COLLECTION_NO);
            if (collectionCode != null) {
                specimen.add(predatorSpecimen);
                setLocationAndDate(locationMap, collectionTimeMap, specimen, collectionCode);
            }
        }
    }

    private Specimen addPredator(Study study, LabeledCSVParser parser, String[] line) throws NodeFactoryException, TermLookupServiceException {
        Specimen predatorSpecimen = nodeFactory.createSpecimen(study, new TaxonImpl("Centropomus undecimalis", null));

        predatorSpecimen.setLifeStage(nodeFactory.getTermLookupService().lookupTermByName("adult"));
        try {
            String length = parser.getValueByLabel("Standard Length");
            predatorSpecimen.setLengthInMm(Double.parseDouble(length));
        } catch (NumberFormatException ex) {
            getLogger().warn(study, "found malformed length in line:" + parser.lastLineNumber() + " [" + StringUtils.join(line, ",") + "]");
        }

        return predatorSpecimen;
    }

    private void setLocationAndDate(Map<String, Location> locationMap, Map<String, Date> collectionTimeMap, List<Specimen> items, String collectionCode) throws NodeFactoryException {
        String collectionCodeTrim = collectionCode.trim();
        Location location = locationMap.get(collectionCodeTrim);
        if (location != null) {
            for (Specimen item : items) {
                item.caughtIn(location);
            }
        }

        Date collectionDatetime = collectionTimeMap.get(collectionCodeTrim);
        if (collectionDatetime != null) {
            for (Specimen item : items) {
                nodeFactory.setUnixEpochProperty(item, collectionDatetime);
            }
        }
    }

    private List<Specimen> addPreyForPredator(String[] header, String[] line, Study study) throws NodeFactoryException {
        List<Specimen> preyItems = new ArrayList<Specimen>();
        int preyColumn = 4;
        for (int i = preyColumn; i < header.length; i++) {
            if (i < line.length) {
                String preyCountString = line[i];
                if (preyCountString.trim().length() > 0) {
                    try {
                        int preyCount = Integer.parseInt(preyCountString);
                        String preyName = header[i];
                        for (int j = 0; j < preyCount; j++) {
                            Specimen preySpecimen = nodeFactory.createSpecimen(study, new TaxonImpl(preyName, null));
                            preyItems.add(preySpecimen);
                        }
                    } catch (NumberFormatException e) {
                        getLogger().warn(study, "failed to parse prey count line/column:");
                    }
                }
            }
        }
        return preyItems;
    }

}
