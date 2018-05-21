package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import com.Ostermiller.util.MD5;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import org.apache.commons.io.*;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.geo.GeoUtil;
import org.eol.globi.geo.LatLng;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.globalbioticinteractions.dataset.CitationUtil;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class StudyImporterForRaymond extends BaseStudyImporter {

    private final static Log LOG = LogFactory.getLog(StudyImporterForRaymond.class);

    private static final String OBSERVATION_DATE_START = "OBSERVATION_DATE_START";
    private static final String OBSERVATION_DATE_END = "OBSERVATION_DATE_END";
    private static final String WEST = "WEST";
    private static final String EAST = "EAST";
    private static final String SOUTH = "SOUTH";
    private static final String NORTH = "NORTH";
    private static final String SOURCES_CSV = "sources.csv";
    private static final String DIET_CSV = "diet.csv";
    private static final String RESOURCE_URL = "https://data.aad.gov.au/aadc/trophic/trophic.zip";
    private static final String RESOURCE_URL_FALLBACK = "https://depot.globalbioticinteractions.org/datasets/org/eol/globi/data/raymond2011/0.2/raymond2011-0.2.zip";

    private static final int MAX_ATTEMPT = 3;
    private Collection<String> locations = new HashSet<String>();

    public StudyImporterForRaymond(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        if (!retrieveAndImport(RESOURCE_URL)) {
            retrieveAndImport(RESOURCE_URL_FALLBACK);
        }
    }

    private boolean retrieveAndImport(String resourceUrl) throws StudyImporterException {
        boolean isDone = false;
        for (int attemptCount = 1; !isDone && attemptCount <= MAX_ATTEMPT; attemptCount++) {
            try {
                LOG.info("[" + resourceUrl + "] downloading (attempt " + attemptCount + ")...");
                InputStream inputStream = getDataset().getResource(resourceUrl);
                importData(inputStream);
                isDone = true;
                LOG.info("[" + resourceUrl + "] downloaded and imported.");
            } catch (IOException e) {
                String msg = "failed to download [" + resourceUrl + "]";
                if (attemptCount > MAX_ATTEMPT) {
                    throw new StudyImporterException(msg, e);
                }
                LOG.warn(msg + " retrying ...", e);
            }
        }
        return isDone;
    }

    private void importData(InputStream inputStream) throws IOException, StudyImporterException {
        File dietFile = null;
        File sourcesFile = null;
        LabeledCSVParser sourcesParser = null;
        LabeledCSVParser dietParser = null;
        try {
            ZipInputStream zis = new ZipInputStream(inputStream);
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (DIET_CSV.equals(entry.getName())) {
                    dietFile = File.createTempFile("raymondDiet", ".csv");
                    dietParser = CSVTSVUtil.createParser(dietFile, zis);
                } else if (SOURCES_CSV.equals(entry.getName())) {
                    sourcesFile = File.createTempFile("raymondSources", ".csv");
                    sourcesParser = CSVTSVUtil.createParser(sourcesFile, zis);
                } else {
                    IOUtils.copy(zis, new NullOutputStream());
                }
            }
            if (sourcesParser == null) {
                throw new StudyImporterException("failed to find [" + SOURCES_CSV + "] in [" + RESOURCE_URL + "]");
            }
            if (dietParser == null) {
                throw new StudyImporterException("failed to find [" + DIET_CSV + "] in [" + RESOURCE_URL + "]");
            }
            importData(sourcesParser, dietParser);
        } finally {
            org.apache.commons.io.FileUtils.deleteQuietly(dietFile);
            org.apache.commons.io.FileUtils.deleteQuietly(sourcesFile);
        }
    }

    public void importData(LabeledCSVParser sourcesParser, LabeledCSVParser dietParser) throws IOException, StudyImporterException {
        Map<Integer, String> sourceMap = buildStudyLookup(sourcesParser);

        while (dietParser.getLine() != null) {
            String sourceId = dietParser.getValueByLabel("SOURCE_ID");
            String citation = sourceMap.get(Integer.parseInt(sourceId));
            if (StringUtils.isBlank(citation)) {
                LOG.error("no source found for id [" + sourceId + "]: line [" + dietParser.lastLineNumber() + "]");
            } else {
                Study study = getOrCreateStudy(citation);
                parseDietObservation(dietParser, study);
            }

        }
    }

    private void parseDietObservation(LabeledCSVParser dietParser, Study study) throws StudyImporterException {
        try {
            Specimen predator = getSpecimen(dietParser, "PREDATOR_NAME", "PREDATOR_LIFE_STAGE", study);

            dietParser.getValueByLabel("ALTITUDE_MIN");
            dietParser.getValueByLabel("ALTITUDE_MAX");

            dietParser.getValueByLabel("DEPTH_MIN");
            dietParser.getValueByLabel("DEPTH_MAX");

            Location sampleLocation = parseLocation(dietParser, study);
            predator.caughtIn(sampleLocation);

            Specimen prey = getSpecimen(dietParser, "PREY_NAME", "PREY_LIFE_STAGE", study);
            prey.caughtIn(sampleLocation);
            predator.ate(prey);

            Date date = parseCollectionDate(dietParser);
            nodeFactory.setUnixEpochProperty(prey, date);
            nodeFactory.setUnixEpochProperty(predator, date);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to import data", e);
        }
    }

    private Specimen getSpecimen(LabeledCSVParser dietParser, String nameLabel, String lifeStageLabel, Study study) throws NodeFactoryException {
        String predatorName = dietParser.getValueByLabel(nameLabel);
        Specimen predator = nodeFactory.createSpecimen(study, new TaxonImpl(predatorName, null));
        String predatorLifeStage = dietParser.getValueByLabel(lifeStageLabel);
        predator.setLifeStage(nodeFactory.getOrCreateLifeStage("RAYMOND:" + predatorLifeStage, predatorLifeStage));
        return predator;
    }

    private Date parseCollectionDate(LabeledCSVParser dietParser) throws StudyImporterException {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy").withZoneUTC();
        Date date = null;
        try {
            String observationDateStart = dietParser.getValueByLabel(OBSERVATION_DATE_START);
            String observationDateEnd = dietParser.getValueByLabel(OBSERVATION_DATE_END);
            if (StringUtils.isNotBlank(observationDateStart) && StringUtils.isNotBlank(observationDateEnd)) {
                DateTime startDate = formatter.parseDateTime(observationDateStart);
                DateTime endDate = formatter.parseDateTime(observationDateEnd);
                DateTime meanTime = startDate.plus(endDate.minus(startDate.toDate().getTime()).toDate().getTime());
                date = meanTime.toDate();
            }
        } catch (IllegalArgumentException ex) {
            throw new StudyImporterException("malformed date on line [" + dietParser.lastLineNumber() + "]", ex);
        }
        return date;
    }

    private Location parseLocation(LabeledCSVParser dietParser, Study study) throws StudyImporterException {
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

        Location loc = null;
        if (StringUtils.isBlank(westString) || StringUtils.isBlank(eastString) || StringUtils.isBlank(northString) || StringUtils.isBlank(southString)) {
            try {
                loc = locationFromLocale(dietParser, study);
            } catch (NodeFactoryException ex) {
                throw new StudyImporterException("found invalid location on line [" + dietParser.lastLineNumber() + "]", ex);
            }
        } else {
            double left = Double.parseDouble(westString);
            double top = Double.parseDouble(northString);
            double right = Double.parseDouble(eastString);
            double bottom = Double.parseDouble(southString);
            LatLng centroid = calculateCentroidOfBBox(left, top, right, bottom);
            try {
                loc = nodeFactory.getOrCreateLocation(new LocationImpl(centroid.getLat(), centroid.getLng(), null, null));
            } catch (NodeFactoryException ex) {
                String locationString = StringUtils.join(Arrays.asList(westString, northString, eastString, southString), ",");
                LOG.warn("found invalid locations [" + locationString + "] on line [" + (dietParser.lastLineNumber() + 1) + "]: " + ex.getMessage());
            }
        }
        return loc;
    }

    private Location locationFromLocale(LabeledCSVParser dietParser, Study study) throws NodeFactoryException {
        Location loc = null;
        LatLng centroid;
        String location = dietParser.getValueByLabel("LOCATION");
        if (StringUtils.isNotBlank(location)) {
            String cleanedLocationString = location.replaceAll("\\.$", "");
            getLocations().add(cleanedLocationString);
            try {
                centroid = getGeoNamesService().findLatLng(cleanedLocationString);
                if (centroid == null) {
                    getLogger().warn(study, "missing lat/lng bounding box [" + dietParser.lastLineNumber() + "] and attempted to using location [" + location + "] failed.");
                } else {
                    loc = nodeFactory.getOrCreateLocation(new LocationImpl(centroid.getLat(), centroid.getLng(), null, null));
                }
            } catch (IOException e) {
                getLogger().warn(study, "failed to lookup point for location [" + location + "] on line [" + dietParser.lastLineNumber() + "]");
            }
        }
        return loc;
    }

    static protected LatLng calculateCentroidOfBBox(double left, double top, double right, double bottom) {
        LatLng latLng;
        if (left == right && top == bottom) {
            latLng = new LatLng(top, left);
        } else {
            Coordinate[] points = {GeoUtil.getCoordinate(top, left), GeoUtil.getCoordinate(top, right),
                    GeoUtil.getCoordinate(bottom, right), GeoUtil.getCoordinate(bottom, left), GeoUtil.getCoordinate(top, left)};
            GeometryFactory geometryFactory = new GeometryFactory();
            Polygon polygon = geometryFactory.createPolygon(points);
            Point centroid = polygon.getCentroid();
            latLng = new LatLng(centroid.getCoordinate().y, centroid.getCoordinate().x);
        }
        return latLng;
    }

    private Map<Integer, String> buildStudyLookup(LabeledCSVParser sourcesParser) throws IOException {
        Map<Integer, String> sourceMap = new HashMap<Integer, String>();
        while (sourcesParser.getLine() != null) {
            Integer sourceId = Integer.parseInt(sourcesParser.getValueByLabel("SOURCE_ID"));
            String reference = sourcesParser.getValueByLabel("DETAILS");
            String citation = ExternalIdUtil.toCitation(null, reference, null);
            sourceMap.put(sourceId, citation);
        }
        return sourceMap;
    }

    private Study getOrCreateStudy(String citation) throws NodeFactoryException {
        String title = StringUtils.abbreviate(citation, 16) + MD5.getHashString(citation);
        return nodeFactory.getOrCreateStudy(new StudyImpl(title, "Raymond, B., Marshall, M., Nevitt, G., Gillies, C., van den Hoff, J., Stark, J.S., Losekoot, M., Woehler, E.J., and Constable, A.J. (2011) A Southern Ocean dietary database. Ecology 92(5):1188. Available from https://doi.org/10.1890/10-1907.1 . Data set supplied by Ben Raymond. " + CitationUtil.createLastAccessedString(RESOURCE_URL), null, citation));
    }


    public Collection<String> getLocations() {
        return locations;
    }
}
