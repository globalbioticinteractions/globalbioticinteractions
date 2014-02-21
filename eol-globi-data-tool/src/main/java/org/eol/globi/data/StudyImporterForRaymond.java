package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import com.Ostermiller.util.MD5;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.geo.GeoUtil;
import org.eol.globi.util.HttpUtil;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.neo4j.graphdb.Relationship;
import uk.me.jstott.jcoord.LatLng;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class StudyImporterForRaymond extends BaseStudyImporter {

    private final static Log LOG = LogFactory.getLog(StudyImporterForRaymond.class);

    public static final String OBSERVATION_DATE_START = "OBSERVATION_DATE_START";
    public static final String OBSERVATION_DATE_END = "OBSERVATION_DATE_END";
    public static final String WEST = "WEST";
    public static final String EAST = "EAST";
    public static final String SOUTH = "SOUTH";
    public static final String NORTH = "NORTH";
    public static final String SOURCES_CSV = "sources.csv";
    public static final String DIET_CSV = "diet.csv";
    public static final String RESOURCE_URL = "http://data.aad.gov.au/aadc/trophic/trophic.zip";
    private Collection<String> locations = new HashSet<String>();

    public StudyImporterForRaymond(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        File dietFile = null;
        File sourcesFile = null;

        try {
            LOG.info("[" + RESOURCE_URL + "] downloading...");
            HttpResponse response = HttpUtil.createHttpClient().execute(new HttpGet(RESOURCE_URL));
            LabeledCSVParser sourcesParser = null;
            LabeledCSVParser dietParser = null;
            ZipInputStream zis = new ZipInputStream(response.getEntity().getContent());
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (DIET_CSV.equals(entry.getName())) {
                    dietFile = File.createTempFile("raymondDiet", ".csv");
                    dietParser = createParser(dietFile, zis);
                } else if (SOURCES_CSV.equals(entry.getName())) {
                    sourcesFile = File.createTempFile("raymondSources", ".csv");
                    sourcesParser = createParser(sourcesFile, zis);
                } else {
                    IOUtils.copy(zis, new NullOutputStream());
                }
            }
            LOG.info("[" + RESOURCE_URL + "] downloaded.");
            if (sourcesParser == null) {
                throw new StudyImporterException("failed to find [" + SOURCES_CSV + "] in [" + RESOURCE_URL + "]");
            }
            if (dietParser == null) {
                throw new StudyImporterException("failed to find [" + DIET_CSV + "] in [" + RESOURCE_URL + "]");
            }
            importData(sourcesParser, dietParser);

        } catch (IOException e) {
            throw new StudyImporterException("failed to import [" + getClass().getSimpleName() + "]", e);
        } finally {
            if (dietFile != null) {
                dietFile.delete();
            }
            if (sourcesFile != null) {
                sourcesFile.delete();
            }
        }
        return null;
    }

    private LabeledCSVParser createParser(File dietFile, ZipInputStream zis) throws IOException {
        LabeledCSVParser dietParser;
        streamToFile(dietFile, zis);
        Reader reader = FileUtils.getUncompressedBufferedReader(new FileInputStream(dietFile), "UTF-8");
        dietParser = new LabeledCSVParser(new CSVParser(reader));
        return dietParser;
    }

    private static void streamToFile(File sourcesFile, ZipInputStream zis) throws IOException {
        FileOutputStream output = new FileOutputStream(sourcesFile);
        IOUtils.copy(zis, output);
        output.flush();
        IOUtils.closeQuietly(output);
    }

    public void importData(LabeledCSVParser sourcesParser, LabeledCSVParser dietParser) throws IOException, StudyImporterException {
        Map<Integer, Study> sourceMap = buildStudyLookup(sourcesParser);

        while (dietParser.getLine() != null) {
            String sourceId = dietParser.getValueByLabel("SOURCE_ID");
            Study study = sourceMap.get(Integer.parseInt(sourceId));
            if (study == null) {
                LOG.error("no source found for id [" + sourceId + "]: line [" + dietParser.lastLineNumber() + "]");
            } else {
                parseDietObservation(dietParser, study);
            }

        }
    }

    private void parseDietObservation(LabeledCSVParser dietParser, Study study) throws StudyImporterException {
        try {
            Specimen predator = getSpecimen(dietParser, "PREDATOR_NAME", "PREDATOR_LIFE_STAGE");

            Relationship collected = study.collected(predator);
            parseCollectionDate(dietParser, collected);

            dietParser.getValueByLabel("ALTITUDE_MIN");
            dietParser.getValueByLabel("ALTITUDE_MAX");

            dietParser.getValueByLabel("DEPTH_MIN");
            dietParser.getValueByLabel("DEPTH_MAX");

            predator.caughtIn(parseLocation(dietParser, study));

            Specimen prey = getSpecimen(dietParser, "PREY_NAME", "PREY_LIFE_STAGE");
            predator.ate(prey);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to import data", e);
        }
    }

    private Specimen getSpecimen(LabeledCSVParser dietParser, String nameLabel, String lifeStageLabel) throws NodeFactoryException {
        String predatorName = dietParser.getValueByLabel(nameLabel);
        Specimen predator = nodeFactory.createSpecimen(predatorName);
        String predatorLifeStage = dietParser.getValueByLabel(lifeStageLabel);
        predator.setLifeStage(nodeFactory.getOrCreateLifeStage("RAYMOND:" + predatorLifeStage, predatorLifeStage));
        return predator;
    }

    private void parseCollectionDate(LabeledCSVParser dietParser, Relationship collected) throws StudyImporterException {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy");
        try {
            String observationDateStart = dietParser.getValueByLabel(OBSERVATION_DATE_START);
            String observationDateEnd = dietParser.getValueByLabel(OBSERVATION_DATE_END);
            if (StringUtils.isNotBlank(observationDateStart) && StringUtils.isNotBlank(observationDateEnd)) {
                DateTime startDate = formatter.parseDateTime(observationDateStart);
                DateTime endDate = formatter.parseDateTime(observationDateEnd);
                DateTime meanTime = startDate.plus(endDate.minus(startDate.toDate().getTime()).toDate().getTime());
                nodeFactory.setUnixEpochProperty(collected, meanTime.toDate());
            }
        } catch (IllegalArgumentException ex) {
            throw new StudyImporterException("malformed date on line [" + dietParser.lastLineNumber() + "]", ex);
        }
    }

    private Location parseLocation(LabeledCSVParser dietParser, Study study) {
        /**
         * left, top ------- right, top
         *  |                 |
         *  |                 |
         * left, bottom  -- right, bottom
         *
         */

        String westString = dietParser.getValueByLabel(WEST);
        String eastString = dietParser.getValueByLabel(EAST);
        String northString = dietParser.getValueByLabel(NORTH);
        String southString = dietParser.getValueByLabel(SOUTH);

        LatLng centroid = null;
        if (StringUtils.isBlank(westString) || StringUtils.isBlank(eastString) || StringUtils.isBlank(northString) || StringUtils.isBlank(southString)) {
            String location = dietParser.getValueByLabel("LOCATION");
            if (StringUtils.isNotBlank(location)) {
                String cleanedLocationString = location.replaceAll("\\.$", "");
                getLocations().add(cleanedLocationString);
                try {
                    centroid = getGeoNamesService().findPointForLocality(cleanedLocationString);
                    if (centroid == null) {
                        getLogger().warn(study, "missing lat/lng bounding box [" + dietParser.lastLineNumber() + "] and attempted to using location [" + location + "] failed.");
                    }
                } catch (IOException e) {
                    getLogger().warn(study, "failed to lookup point for location [" + location + "] on line [" + dietParser.lastLineNumber() + "]");
                }
            }
        } else {
            double left = Double.parseDouble(westString);
            double top = Double.parseDouble(northString);
            double right = Double.parseDouble(eastString);
            double bottom = Double.parseDouble(southString);
            centroid = calculateCentroidOfBBox(left, top, right, bottom);
        }

        return centroid == null ? null : nodeFactory.getOrCreateLocation(centroid.getLat(), centroid.getLng(), null);
    }

    static protected LatLng calculateCentroidOfBBox(double left, double top, double right, double bottom) {
        LatLng latLng;
        if (left == right && top == bottom) {
            latLng = new LatLng(left, top);
        } else {
            Coordinate[] points = {GeoUtil.getCoordinate(left, top), GeoUtil.getCoordinate(right, top),
                    GeoUtil.getCoordinate(right, bottom), GeoUtil.getCoordinate(left, bottom), GeoUtil.getCoordinate(left, top)};
            GeometryFactory geometryFactory = new GeometryFactory();
            Polygon polygon = geometryFactory.createPolygon(points);
            Point centroid = polygon.getCentroid();
            latLng = new LatLng(centroid.getCoordinate().y, centroid.getCoordinate().x);
        }
        return latLng;
    }

    private Map<Integer, Study> buildStudyLookup(LabeledCSVParser sourcesParser) throws IOException {
        Map<Integer, Study> sourceMap = new HashMap<Integer, Study>();
        while (sourcesParser.getLine() != null) {
            Integer sourceId = Integer.parseInt(sourcesParser.getValueByLabel("SOURCE_ID"));
            String reference = sourcesParser.getValueByLabel("DETAILS");
            String title = StringUtils.abbreviate(reference, 16) + MD5.getHashString(reference);
            Study study = nodeFactory.getOrCreateStudy(title, null, null, null, reference, null, "Raymond, B., Marshall, M., Nevitt, G., Gillies, C., van den Hoff, J., Stark, J.S., Losekoot, M., Woehler, E.J., and Constable, A.J. (2011) A Southern Ocean dietary database. Ecology 92(5):1188. http://data.aad.gov.au/aadc/trophic/. doi: 10.1890/i0012-9658-92-5-1188. Data set supplied by Ben Raymond. The data can also be accessed at http://data.aad.gov.au/aadc/trophic/.");
            sourceMap.put(sourceId, study);
        }
        return sourceMap;
    }

    public Collection<String> getLocations() {
        return locations;
    }
}
