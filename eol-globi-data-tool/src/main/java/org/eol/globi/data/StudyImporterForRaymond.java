package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import com.Ostermiller.util.MD5;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.geo.GeoUtil;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.service.GeoNamesServiceImpl;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.neo4j.graphdb.Relationship;
import uk.me.jstott.jcoord.LatLng;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class StudyImporterForRaymond extends BaseStudyImporter {

    private final static Log LOG = LogFactory.getLog(StudyImporter.class);
    public static final String OBSERVATION_DATE_START = "OBSERVATION_DATE_START";
    public static final String OBSERVATION_DATE_END = "OBSERVATION_DATE_END";
    public static final String WEST = "WEST";
    public static final String EAST = "EAST";
    public static final String SOUTH = "SOUTH";
    public static final String NORTH = "NORTH";
    private Collection<String> locations = new HashSet<String>();

    private GeoNamesService geoNamesService = new GeoNamesServiceImpl();

    public StudyImporterForRaymond(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        try {
            LabeledCSVParser dietParser = parserFactory.createParser("raymond/diet.csv.gz", "UTF-8");
            LabeledCSVParser sourcesParser = parserFactory.createParser("raymond/sources.csv.gz", "UTF-8");

            importData(sourcesParser, dietParser);

        } catch (IOException e) {
            throw new StudyImporterException("failed to import [" + getClass().getSimpleName() + "]", e);
        }
        return null;
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
            String predatorName = dietParser.getValueByLabel("PREDATOR_NAME");
            Specimen predator = nodeFactory.createSpecimen(predatorName);

            String label = "PREDATOR_LIFE_STAGE";
            String predatorLifeStage = dietParser.getValueByLabel(label);
            predator.setLifeStage(nodeFactory.getOrCreateLifeStage("RAYMOND:" + predatorLifeStage, predatorLifeStage));

            Relationship collected = study.collected(predator);
            parseCollectionDate(dietParser, study, collected);

            dietParser.getValueByLabel("ALTITUDE_MIN");
            dietParser.getValueByLabel("ALTITUDE_MAX");

            dietParser.getValueByLabel("DEPTH_MIN");
            dietParser.getValueByLabel("DEPTH_MAX");

            predator.caughtIn(parseLocation(dietParser, study));

            dietParser.getValueByLabel("PREY_NAME");
            dietParser.getValueByLabel("PREY_LIFE_STAGE");
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to import data", e);
        }
    }

    private void parseCollectionDate(LabeledCSVParser dietParser, Study study, Relationship collected) throws StudyImporterException {
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
            String title = reference.split("\\)")[0] + ")" + MD5.getHashString(reference);
            Study study = nodeFactory.getOrCreateStudy(title, null, null, null, reference, null, "http://esapubs.org/archive/ecol/E092/097/");
            sourceMap.put(sourceId, study);
        }
        return sourceMap;
    }

    private File download(String prefix, String dataUrl) throws StudyImporterException {
        try {
            File tmpFile = File.createTempFile(prefix, ".csv");
            FileOutputStream os = new FileOutputStream(tmpFile);
            LOG.info("[" + tmpFile.getAbsolutePath() + "] downloading...");
            InputStream is = new URL(dataUrl).openStream();
            IOUtils.copy(is, os);
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
            if (tmpFile.exists()) {
                LOG.info("[" + tmpFile.getAbsolutePath() + "] downloaded.");
            }
            return tmpFile;
        } catch (IOException e) {
            throw new StudyImporterException("failed to donwload [" + dataUrl + "]", e);
        }
    }

    public Collection<String> getLocations() {
        return locations;
    }
}
