package org.eol.globi.geo;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class EcoRegionFinderImpl implements EcoRegionFinder {

    private static final Log LOG = LogFactory.getLog(EcoRegionFinder.class);

    private final EcoRegionFinderConfig config;
    private FileDataStore store = null;

    public EcoRegionFinderImpl(EcoRegionFinderConfig config) {
        this.config = config;
    }

    public Map<String, String> findEcoRegion(Point point) throws EcoRegionFinderException {
        lazyLoadStore();
        try {
            SimpleFeatureSource featureSource = store.getFeatureSource();
            return getFeatureProperties(point, featureSource.getFeatures());
        } catch (IOException e) {
            throw new EcoRegionFinderException("lookup feature for point [" + point.toText() + "] from shapefile at [" + config.getShapeFilePath() + "]", e);
        }
    }

    private void lazyLoadStore() throws EcoRegionFinderException {
        if (store == null) {
            URL dataStoreURL = getDataStoreURLForShapeFile(config.getShapeFilePath());
            try {
                store = FileDataStoreFinder.getDataStore(dataStoreURL);
            } catch (IOException e) {
                throw new EcoRegionFinderException("failed to load data store from url [" + dataStoreURL.toExternalForm() + "]", e);
            }
        }
    }

    private Map<String, String> getFeatureProperties(Point point, SimpleFeatureCollection featureCollection) {
        Map<String, String> map = null;
        SimpleFeatureIterator features = featureCollection.features();
        try {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                Object defaultGeometry = feature.getDefaultGeometry();
                if (defaultGeometry instanceof MultiPolygon) {
                    MultiPolygon polygon = (MultiPolygon) defaultGeometry;
                    if (polygon.contains(point)) {
                        map = new TreeMap<String, String>();
                        SimpleFeatureType featureType = feature.getFeatureType();
                        List<AttributeDescriptor> attributeDescriptors = featureType.getAttributeDescriptors();
                        for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
                            String localName = attributeDescriptor.getLocalName();
                            Object value = feature.getAttribute(localName);
                            if (value != null) {
                                if (value instanceof Number) {
                                    value = Integer.toString(((Number) value).intValue());
                                } else {
                                    value = value.toString();
                                }
                                map.put(attributeDescriptor.getLocalName(), value.toString());
                            }
                        }
                        break;
                    }
                }
            }
        } finally {
            features.close();
        }
        return map;
    }

    @Override
    public Collection<EcoRegion> findEcoRegion(double lat, double lng) throws EcoRegionFinderException {
        final Map<String, String> props = findEcoRegion(new GeometryFactory().createPoint(new Coordinate(lng, lat)));
        return props == null || !props.containsKey(config.getIdLabel()) ? null : new ArrayList<EcoRegion>() {{
            add(createEcoRegion(props));
        }};
    }

    @Override
    public void shutdown() {
        if (store != null) {
            store.dispose();
        }
    }

    private EcoRegion createEcoRegion(Map<String, String> props) {
        EcoRegion ecoRegion;
        ecoRegion = new EcoRegion();
        ecoRegion.setId(config.getNamespace() + ":" + props.get(config.getIdLabel()));
        ecoRegion.setName(props.get(config.getNameLabel()));
        ecoRegion.setGeometry(props.get(config.getGeometryLabel()));

        StringBuilder path = new StringBuilder();
        for (String label : config.getPathLabels()) {
            if (path.length() > 0) {
                path.append(" | ");
            }
            if (props.containsKey(label)) {
                String value = props.get(label);
                if (StringUtils.isNotBlank(value)) {
                    path.append(value);
                }

            }
        }
        ecoRegion.setPath(path.toString());
        return ecoRegion;
    }

    private URL getDataStoreURLForShapeFile(String shapeFile) {
        URI resourceURI = null;
        try {
            String shapeFileDir = System.getProperty("shapefiles.dir");
            if (StringUtils.isNotBlank(shapeFileDir)) {
                File file = new File(shapeFileDir + shapeFile);
                resourceURI = file.toURI();
            }

            if (null == resourceURI) {
                resourceURI = EcoRegionFinderFactoryImpl.class.getResource(shapeFile).toURI();
            }
            LOG.info("attempting to use using shapefile at [" + resourceURI.toString() + "]");
            return resourceURI.toURL();
        } catch (Exception e) {
            throw new RuntimeException("failed to find [" + shapeFile + "] ... did you run mvn install on the commandline to install shapefiles?");
        }
    }

}
