package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Season;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.neo4j.graphdb.Relationship;
import uk.me.jstott.jcoord.LatLng;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class StudyImporterForWrast extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForWrast.class);

    public static final String LENGTH_IN_MM = "lengthInMm";
    public static final String SEASON = "season";
    public static final String PREY_SPECIES = "prey species";
    public static final String PREDATOR_SPECIES = "predator species";
    public static final String PREDATOR_FAMILY = "predatorFamily";
    private static final String REGION = "region";
    private static final String HABITAT = "habitat";
    private static final String SITE = "site";
    private static final String PREDATOR_SPECIMEN_ID = "specimenId";
    private static final String MONTH = "Month";
    private static final String DAY = "Day";
    private static final String YEAR = "Year";

    static final HashMap<String, String> COLUMN_MAPPER = new HashMap<String, String>() {
        {
            put(SEASON, "Season");
            put(PREY_SPECIES, "Prey item");
            put(PREDATOR_SPECIES, "Predator Species");
            put(LENGTH_IN_MM, "TL (mm)");
            put(PREDATOR_FAMILY, "Family");
            put(REGION, "Region");
            put(HABITAT, "Habitat");
            put(SITE, "Site");
            put(PREDATOR_SPECIMEN_ID, "Call #");
            put(MONTH, "Month");
            put(DAY, "Day");
            put(YEAR, "Year");
        }
    };

    protected static final String LAVACA_BAY_DATA_SOURCE = "wrast/Wrast Thesis Raw Data gut content.csv";
    protected static final String LAVACA_BAY_LOCATIONS = "wrast/lavacaBayLocations.csv";
    protected static final String LAVACA_BAY_ENVIRONMENTAL = "wrast/lavacaBayEnvironmental.csv";


    private Map<String, LatLng> locationMap;
    private Map<String, Double> depthMap;
    private final HashMap<String, Specimen> predatorSpecimenMap = new HashMap<String, Specimen>();

    public StudyImporterForWrast(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    protected static SimpleDateFormat getSimpleDateFormat() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yy");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("US/Central"));
        return simpleDateFormat;
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        Study study = nodeFactory.getOrCreateStudy("Wrast 2008",
                "Jenny L. Wrast",
                "Department of Life Sciences Texas A&M University-Corpus Christi",
                "July 2006- April 2007",
                "Spatiotemporal And Habitat-Mediated Food Web Dynamics in Lavaca Bay, Texas.", "2008", StudyImporterForGoMexSI.GOMEXSI_URL);
        try {
            LabeledCSVParser csvParser = parserFactory.createParser(LAVACA_BAY_DATA_SOURCE, CharsetConstant.UTF8);
            LengthParser parser = new LengthParserImpl(COLUMN_MAPPER.get(LENGTH_IN_MM));
            getPredatorSpecimenMap().clear();
            while (csvParser.getLine() != null) {
                addNextRecordToStudy(csvParser, study, COLUMN_MAPPER, parser);
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to create study [" + LAVACA_BAY_DATA_SOURCE + "]", e);
        } finally {
            getPredatorSpecimenMap().clear();
        }
        return study;
    }


    private void addNextRecordToStudy(LabeledCSVParser csvParser, Study study, Map<String, String> columnToNormalizedTermMapper, LengthParser lengthParser) throws StudyImporterException {
        String seasonName = csvParser.getValueByLabel(columnToNormalizedTermMapper.get(SEASON));
        String preyItem = csvParser.getValueByLabel(columnToNormalizedTermMapper.get(PREY_SPECIES));
        if (preyItem == null) {
            LOG.warn("no prey name for line [" + csvParser.getLastLineNumber() + "]");
        } else {
            Specimen prey = createAndClassifySpecimen(preyItem);

            String habitat = csvParser.getValueByLabel(COLUMN_MAPPER.get(HABITAT));
            String site = csvParser.getValueByLabel(COLUMN_MAPPER.get(SITE));
            String region = csvParser.getValueByLabel(COLUMN_MAPPER.get(REGION));
            String sampleLocationId = createLocationId(habitat, region, site);

            Map<String, LatLng> averageLocations = getLocationMap();

            LatLng latLng1 = averageLocations.get(sampleLocationId);
            if (latLng1 == null) {
                throw new StudyImporterException("no location information for [" + sampleLocationId + "] on line [" + csvParser.getLastLineNumber() + "] found in [" + averageLocations + "]");
            }

            if (depthMap == null) {
                depthMap = createDepthMap();
            }

            Double depth = depthMap.get(createDepthId(seasonName, region, site, habitat));

            Double altitude = depth == null ? null : -depth;
            if (depth == null) {
                LOG.warn(createMsgPrefix(csvParser) + " failed to find depth for habitat, region, site and season: [" + createDepthId(seasonName, region, site, habitat) + "], skipping entry");
            }

            Location sampleLocation = nodeFactory.getOrCreateLocation(latLng1.getLat(), latLng1.getLng(), altitude);
            prey.caughtIn(sampleLocation);
            prey.caughtDuring(getOrCreateSeason(seasonName));

            String speciesName = csvParser.getValueByLabel(columnToNormalizedTermMapper.get(PREDATOR_SPECIES));
            String predatorId = csvParser.getValueByLabel(columnToNormalizedTermMapper.get(PREDATOR_SPECIMEN_ID));
            Map<String, Specimen> predatorMap = getPredatorSpecimenMap();
            Specimen predator = predatorMap.get(predatorId);
            if (predator == null) {
                predator = addPredatorSpecimen(csvParser, study, lengthParser, seasonName, sampleLocation, speciesName, predatorId, predatorMap);
            }

            predator.ate(prey);
        }
    }

    private Specimen addPredatorSpecimen(LabeledCSVParser csvParser, Study study, LengthParser lengthParser, String seasonName, Location sampleLocation, String speciesName, String predatorId, Map<String, Specimen> predatorMap) throws StudyImporterException {
        Specimen predator;
        predator = createAndClassifySpecimen(speciesName);
        predatorMap.put(predatorId, predator);
        predator.setLengthInMm(lengthParser.parseLengthInMm(csvParser));

        if (null != sampleLocation) {
            predator.caughtIn(sampleLocation);
        }
        predator.caughtDuring(getOrCreateSeason(seasonName));
        Relationship collectedRel = study.collected(predator);
        addCollectionDate(csvParser, collectedRel);

        return predator;
    }

    private void addCollectionDate(LabeledCSVParser csvParser, Relationship collectedRel) {
        String dayString = csvParser.getValueByLabel(COLUMN_MAPPER.get(DAY));
        String monthString = csvParser.getValueByLabel(COLUMN_MAPPER.get(MONTH));
        String yearString = csvParser.getValueByLabel(COLUMN_MAPPER.get(YEAR));
        if (StringUtils.isNotBlank(dayString) && StringUtils.isNotBlank(monthString) && StringUtils.isNotBlank(yearString)) {
            String dateString = monthString + "/" + dayString + "/" + yearString;
            try {
                Date collectionDate = getSimpleDateFormat().parse(dateString);
                nodeFactory.setUnixEpochProperty(collectedRel, collectionDate);
            } catch (ParseException e) {
                LOG.warn("failed to parse [" + dateString + "]", e);
            }
        }
    }

    private HashMap<String, Specimen> getPredatorSpecimenMap() {
        return predatorSpecimenMap;
    }

    private Map<String, Double> createDepthMap() throws StudyImporterException {
        Map<String, Double> depthMap;
        try {
            LabeledCSVParser depthParser = parserFactory.createParser(LAVACA_BAY_ENVIRONMENTAL, CharsetConstant.UTF8);
            depthMap = new HashMap<String, Double>();
            while (depthParser.getLine() != null) {
                String seasonDepth = depthParser.getValueByLabel("Season");
                String regionDepth = depthParser.getValueByLabel("Upper/Lower");
                String siteDepth = depthParser.getValueByLabel("Site");
                String habitatDepth = depthParser.getValueByLabel("Habitat");
                String depthString = depthParser.getValueByLabel("Depth (m)");
                String depthId = createDepthId(seasonDepth, regionDepth, siteDepth, habitatDepth);
                if (depthMap.get(depthId) == null) {
                    try {
                        depthMap.put(depthId, Double.parseDouble(depthString));
                    } catch (NumberFormatException ex) {
                        LOG.warn(createMsgPrefix(depthParser) + "failed to parse depth for depthId [" + depthId + "], skipping entry");
                    }
                } else {
                    throw new StudyImporterException(createMsgPrefix(depthParser) + " found duplicate entries for unique combination of season,region,site and habitat: [" + seasonDepth + ", " + regionDepth + ", " + siteDepth + ", " + seasonDepth);
                }
            }

        } catch (IOException e1) {
            throw new StudyImporterException("failed to read from [" + LAVACA_BAY_ENVIRONMENTAL + "]");
        }
        return depthMap;
    }

    protected static String createDepthId(String seasonString, String region, String site, String habitat) {
        region = region.trim();
        if ("U".equals(region)) {
            region = "Upper";
        } else if ("L".equals(region)) {
            region = "Lower";
        }

        habitat = habitat.trim();
        if ("M".equals(habitat)) {
            habitat = "Marsh";
        } else if ("R".equals(habitat)) {
            habitat = "Reef";
        } else if ("NV".equals(habitat)) {
            habitat = "Non-Veg";
        }
        return createLocationId(habitat, region, site) + seasonString;
    }

    private Season getOrCreateSeason(String seasonName) {
        String seasonNameLower = seasonName.toLowerCase().trim();
        Season season = nodeFactory.findSeason(seasonNameLower);
        if (null == season) {
            season = nodeFactory.createSeason(seasonNameLower);
        }
        return season;
    }

    private Map<String, LatLng> getLocationMap() throws StudyImporterException {
        if (locationMap == null) {
            createLocationMap();
        }
        return locationMap;
    }

    private void createLocationMap() throws StudyImporterException {
        Map<String, List<LatLng>> locations = new HashMap<String, List<LatLng>>();

        LabeledCSVParser parser = null;
        try {
            parser = parserFactory.createParser(LAVACA_BAY_LOCATIONS, CharsetConstant.UTF8);
            while (parser.getLine() != null) {
                String habitateDef = parser.getValueByLabel(COLUMN_MAPPER.get(HABITAT));
                String regionDef = parser.getValueByLabel(COLUMN_MAPPER.get(REGION));
                String siteDef = parser.getValueByLabel(COLUMN_MAPPER.get(SITE));
                String latitude = parser.getValueByLabel("Latitude");
                if (null == latitude) {
                    throw new StudyImporterException(createMsgPrefix(parser) + " missing latitude");
                }
                String longitude = parser.getValueByLabel("Longitude");
                if (null == longitude) {
                    throw new StudyImporterException(createMsgPrefix(parser) + " missing longitude");
                }
                Double lat;
                try {
                    lat = Double.parseDouble(latitude);
                } catch (NumberFormatException ex) {
                    throw new StudyImporterException(createMsgPrefix(parser) + " invalid latitude [" + latitude + "]");
                }
                Double lng;
                try {
                    lng = Double.parseDouble(longitude);
                } catch (NumberFormatException ex) {
                    throw new StudyImporterException("in [" + LAVACA_BAY_LOCATIONS + ":" + parser.getLastLineNumber() + "]: invalid longtude [" + longitude + "]");
                }
                LatLng latLng = new LatLng(lat, lng);
                String locationId = createLocationId(habitateDef, regionDef, siteDef);
                List<LatLng> latLngs = locations.get(locationId);
                if (null == latLngs) {
                    latLngs = new ArrayList<LatLng>();
                    locations.put(locationId, latLngs);
                }
                latLngs.add(latLng);
            }
        } catch (IOException e) {
            throw new StudyImporterException("problem to reading from [" + LAVACA_BAY_LOCATIONS + "]", e);
        }


        locationMap = new HashMap<String, LatLng>();

        for (Map.Entry<String, List<LatLng>> entry : locations.entrySet()) {
            Double lat = 0.0d;
            Double lng = 0.0d;
            int count = 0;
            for (LatLng latLng : entry.getValue()) {
                lat += latLng.getLat();
                lng += latLng.getLng();
                count++;
            }
            if (count == 0) {
                throw new StudyImporterException("must have more than one location per locationId");
            }
            locationMap.put(entry.getKey(), new LatLng(lat / count, lng / count));
        }
    }

    private String createMsgPrefix(LabeledCSVParser parser) {
        return "in [" + LAVACA_BAY_LOCATIONS + ":" + parser.getLastLineNumber() + "]:";
    }

    private static String createLocationId(String habitateDef, String regionDef, String siteDef) {
        if ("Marsh edge".equals(habitateDef)) {
            habitateDef = "Marsh";
        }
        return habitateDef.trim() + regionDef.trim() + siteDef.trim();
    }

    private Specimen createAndClassifySpecimen(final String speciesName) throws StudyImporterException {
        try {
            return nodeFactory.createSpecimen(speciesName);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to classify specimen with name [" + speciesName + "]", e);
        }
    }

}


