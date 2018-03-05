package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import com.Ostermiller.util.MD5;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.geo.LatLng;
import org.geotools.data.FeatureReader;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.globalbioticinteractions.dataset.CitationUtil;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.eol.globi.data.StudyImporterForTSV.*;

public class StudyImporterForSzoboszlai extends BaseStudyImporter {

    public StudyImporterForSzoboszlai(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        try {
            Map<Integer, LatLng> localeMap = importShapes();
            importLinks(getDataset().getResource("links")
                    , new InteractionListenerImpl(nodeFactory, getGeoNamesService(), getLogger())
                    , localeMap);
        } catch (IOException e) {
            throw new StudyImporterException("failed to import [" + getDataset().getArchiveURI().toString() + "]");
        }
    }

    protected void importLinks(InputStream is, InteractionListener interactionListener, Map<Integer, LatLng> localeMap) throws IOException, StudyImporterException {
        LabeledCSVParser parser = new LabeledCSVParser(new CSVParser(is));
        while (parser.getLine() != null) {
            Map<String, String> e = importLink(parser, localeMap);
            if (e != null) {
                interactionListener.newLink(e);
            }
        }
    }

    protected Map<String, String> importLink(LabeledCSVParser parser, Map<Integer, LatLng> localeMap) throws IOException, StudyImporterException {
        TreeMap<String, String> link = new TreeMap<String, String>();

        link.put(STUDY_SOURCE_CITATION, getSourceCitationLastAccessed());

        String predNum = StringUtils.trim(parser.getValueByLabel("PredatorSciNameTSN"));
        if (StringUtils.isNotBlank(predNum)) {
            link.put(SOURCE_TAXON_ID, TaxonomyProvider.ITIS.getIdPrefix() + predNum);
        }

        String predName = StringUtils.trim(parser.getValueByLabel("PredatorSciName"));
        if (StringUtils.isNotBlank(predName)) {
            link.put(SOURCE_TAXON_NAME, predName);
        }

        String preyNum = StringUtils.trim(parser.getValueByLabel("PreySciNameTSN"));
        if (StringUtils.isNotBlank(preyNum)) {
            link.put(TARGET_TAXON_ID, TaxonomyProvider.ITIS.getIdPrefix() + preyNum);
        }

        String preyName = StringUtils.trim(parser.getValueByLabel("PreySciName"));
        if (StringUtils.isNotBlank(preyName)) {
            link.put(TARGET_TAXON_NAME, preyName);
        }

        String[] citeFields = {"CiteAuth", "CiteYear", "CiteTitle", "CiteSource", "CiteVolume", "CitePages"};
        List<String> citeValues = new ArrayList<String>();
        for (String citeField : citeFields) {
            String value = StringUtils.trim(parser.getValueByLabel(citeField));
            if (StringUtils.isNotBlank(value)) {
                String prefix;
                if ("CiteVolume".equals(citeField)) {
                    prefix = "v.";
                } else if ("CitePages".equals(citeField)) {
                    prefix = "pp.";
                } else {
                    prefix = "";
                }
                citeValues.add(prefix + value);
            }
        }
        String referenceCitation = StringUtils.join(citeValues, ". ");
        link.put(REFERENCE_ID, getSourceDOI() + '/' + MD5.getHashString(referenceCitation));
        link.put(REFERENCE_CITATION, referenceCitation);
        link.put(INTERACTION_TYPE_NAME, "preysOn");
        link.put(INTERACTION_TYPE_ID, "RO:0002439");
        link.put(LOCALITY_NAME, StringUtils.trim(parser.getValueByLabel("LocatName")));
        String locatNum = StringUtils.trim(parser.getValueByLabel("LocatNum"));
        if (StringUtils.isNotBlank(locatNum)) {
            try {
                LatLng latLng = localeMap.get(Integer.parseInt(locatNum));
                if (latLng != null) {
                    link.put(DECIMAL_LATITUDE, Double.toString(latLng.getLat()));
                    link.put(DECIMAL_LONGITUDE, Double.toString(latLng.getLng()));
                }
            } catch (NumberFormatException ex) {
                throw new StudyImporterException("found invalid LocalNum [" + locatNum + "] " + parser.lastLineNumber(), ex);
            }
        }
        return link;
    }

    protected Map<Integer, LatLng> importShapes() throws StudyImporterException {
        Map<Integer, LatLng> localityMap = new TreeMap<>();
        FileDataStore dataStore = null;
        try {
            InputStream shapeZipArchive = getDataset().getResource("shapes");
            File tmpFolder = new File(FileUtils.getTempDirectory(), UUID.randomUUID().toString());
            tmpFolder.deleteOnExit();
            unpackZip(shapeZipArchive, tmpFolder);
            dataStore = FileDataStoreFinder.getDataStore(new File(tmpFolder, "LocatPolygonsPoints.shp"));
            if (dataStore == null) {
                throw new StudyImporterException("failed to parse shapefiles");
            }
            FeatureReader<SimpleFeatureType, SimpleFeature> featureReader = dataStore.getFeatureReader();
            while (featureReader.hasNext()) {
                SimpleFeature feature = featureReader.next();
                Object geom = feature.getAttribute("the_geom");
                if (geom instanceof Point) {
                    Coordinate coordinate = ((Point) geom).getCoordinate();
                    Object localNum = feature.getAttribute("LocatNum");
                    if (localNum instanceof Integer) {
                        localityMap.put((Integer) localNum
                                , new LatLng(coordinate.y, coordinate.x));
                    }
                }
            }
            featureReader.close();
        } catch (IOException e) {
            throw new StudyImporterException(e);
        } finally {
            if (dataStore != null) {
                dataStore.dispose();
            }
        }
        return localityMap;
    }

    private void unpackZip(InputStream is, File outputDir) throws IOException {
        ZipInputStream zipStream = new ZipInputStream(is);
        try {
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                File entryDestination = new File(outputDir, entry.getName());
                if (entry.isDirectory())
                    entryDestination.mkdirs();
                else {
                    entryDestination.getParentFile().mkdirs();
                    OutputStream out = new FileOutputStream(entryDestination);
                    IOUtils.copy(zipStream, out);
                    out.flush();
                    zipStream.closeEntry();
                    IOUtils.closeQuietly(out);
                }
            }
        } finally {
            IOUtils.closeQuietly(zipStream);
        }
    }
}
