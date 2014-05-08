package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyImporterForFWDP extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForFWDP.class);

    public static final DateTimeFormatter DATE_TIME_FORMATTER = ISODateTimeFormat.dateTimeParser();

    public StudyImporterForFWDP(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        Study study = nodeFactory.getOrCreateStudy("NOAA; Northeast Fisheries Science Center; Food Web Dynamics Program",
                "Brian Smith",
                "",
                "",
                "Food Habits Database of Food Web Dynamics Program, Northeast Fisheries Science Center, National Oceanic and Atmospheric Administration (NOAA)."
                , null
                , "http://www.nefsc.noaa.gov/femad/pbio/fwdp/");
        String studyResource = "fwdp/NEshelf/NEshelf_diet.csv.gz";

        Map<String, Long> predatorSpecimenMap = new HashMap<String, Long>();
        try {
            LabeledCSVParser parser = parserFactory.createParser(studyResource, CharsetConstant.UTF8);
            while (parser.getLine() != null) {
                int lastLineNumber = parser.getLastLineNumber();
                if (importFilter.shouldImportRecord((long) lastLineNumber)) {
                    Specimen predatorSpecimen = getPredatorSpecimen(study, predatorSpecimenMap, parser);
                    associatePreySpecimen(parser, predatorSpecimen);
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to access resource [" + studyResource + "]");
        }

        return study;
    }

    private void associatePreySpecimen(LabeledCSVParser parser, Specimen predatorSpecimen) throws StudyImporterException {
        try {
            String prey = parseTaxonName(parser, "PYNAM");
            Specimen preySpecimen = nodeFactory.createSpecimen(prey);
            String preyLength = parser.getValueByLabel("pylen");
            if (StringUtils.isNotBlank(preyLength)) {
                predatorSpecimen.setLengthInMm(new Double(preyLength));
            }
            predatorSpecimen.ate(preySpecimen);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to associate predator to prey on line [" + parser.lastLineNumber() + "]", e);
        }
    }

    private Specimen getPredatorSpecimen(Study study, Map<String, Long> predatorSpecimenMap, LabeledCSVParser parser) throws StudyImporterException {
        Specimen predatorSpecimen = null;
        try {
            String uniquePredatorId = parser.getValueByLabel("cruise6") + "-" + parser.getValueByLabel("PDID") + "-" + parser.getValueByLabel("CATNUM");
            Long nodeId = predatorSpecimenMap.get(uniquePredatorId);
            if (nodeId != null) {
                Node nodeById = nodeFactory.getGraphDb().getNodeById(nodeId);
                if (nodeById != null) {
                    predatorSpecimen = new Specimen(nodeById);
                }
            }
            if (predatorSpecimen == null) {
                String predatorTaxonName = parseTaxonName(parser, "pdscinam");
                predatorSpecimen = nodeFactory.createSpecimen(predatorTaxonName);
                predatorSpecimen.setExternalId(uniquePredatorId);
                Relationship collected = study.collected(predatorSpecimen);
                try {
                    addDateTime(parser, collected);
                } catch (IllegalArgumentException ex) {
                    getLogger().warn(study, "found illegal date time on line [" + parser.lastLineNumber() + "]: " + ex.getMessage());
                }
                try {
                    addLocation(parser, predatorSpecimen);
                } catch (NumberFormatException ex) {
                    getLogger().warn(study, "found illegal location line [" + parser.lastLineNumber() + "]: " + ex.getMessage());
                }
                String lengthCm = parser.getValueByLabel("PDLEN");
                if (StringUtils.isNotBlank(lengthCm)) {
                    predatorSpecimen.setLengthInMm(new Double(lengthCm) * 10.0);
                }
                predatorSpecimenMap.put(StringUtils.trim(uniquePredatorId), predatorSpecimen.getNodeID());
            }
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create predator specimen on line [" + parser.lastLineNumber() + "]", e);
        }
        return predatorSpecimen;
    }

    private void addLocation(LabeledCSVParser parser, Specimen instigatorSpecimen) throws NodeFactoryException {
        String latString = parser.getValueByLabel("declat");
        String lngString = parser.getValueByLabel("declon");
        Double lng = new Double(lngString);
        Double lat = new Double(latString);
        // note that the minus sign for longitude was apparently omitted
        Location loc = nodeFactory.getOrCreateLocation(lat, -1.0 * lng, null);
        instigatorSpecimen.caughtIn(loc);
    }

    private void addDateTime(LabeledCSVParser parser, Relationship collected) {
        String year = parser.getValueByLabel("year");
        String month = parser.getValueByLabel("month");
        String day = parser.getValueByLabel("day");
        DateTime catchDateTime = parseDateTime(year, month, day, parser.getValueByLabel("hour"), parser.getValueByLabel("minute"));
        nodeFactory.setUnixEpochProperty(collected, catchDateTime.toDate());
    }

    protected static DateTime parseDateTime(String year, String month, String day, String hourOfDay, String minute) {
        DateTime dateTime = null;
        if (StringUtils.isNotBlank(year) && StringUtils.isNotBlank(month) && StringUtils.isNotBlank(day)) {
            dateTime = parseDateTimeString(year, month, day, hourOfDay, minute);
        }
        return dateTime;
    }

    private static DateTime parseDateTimeString(String year, String month, String day, String hourOfDay, String minute) {
        DateTime dateTime;
        StringBuilder dateTimeString = new StringBuilder();
        dateTimeString.append(year);
        dateTimeString.append("-").append(month);
        dateTimeString.append("-").append(day);
        dateTimeString.append("T").append(StringUtils.isBlank(hourOfDay) ? "00" : hourOfDay);
        dateTimeString.append(":").append(StringUtils.isBlank(minute) ? "00" : minute);
        dateTimeString.append(":00Z");
        dateTime = DATE_TIME_FORMATTER.parseDateTime(dateTimeString.toString());
        return dateTime;
    }

    private String parseTaxonName(LabeledCSVParser parser, String taxonLabel) throws StudyImporterException {
        String instigatorScientificName = parser.getValueByLabel(taxonLabel);
        if (StringUtils.isBlank(instigatorScientificName)) {
            throw new StudyImporterException("found missing instigator scientific name at line [" + parser.getLastLineNumber() + "]");
        } else {
            instigatorScientificName = StringUtils.lowerCase(StringUtils.trim(instigatorScientificName));
            String[] parts = StringUtils.split(instigatorScientificName);

            List<String> cleanerParts = new ArrayList<String>();
            for (int i = 0; i < parts.length; i++) {
                String part = StringUtils.lowerCase(parts[i]);
                if (i == 0) {
                    part = StringUtils.capitalize(part);
                }
                cleanerParts.add(part);
            }
            return StringUtils.join(cleanerParts, " ");
        }
    }
}
