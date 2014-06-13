package org.eol.globi.geo;

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

public class EcoregionFinderImpl2 implements EcoregionFinder2 {

    private static final Log LOG = LogFactory.getLog(EcoregionFinder2.class);

    private final EcoregionFinderConfig2 config;
    private FileDataStore store = null;

    public EcoregionFinderImpl2(EcoregionFinderConfig2 config) {
        this.config = config;
    }

    public Map<String, Object> findEcoregion(Point point) throws EcoregionFinderException2 {
        lazyLoadStore();
        try {
            SimpleFeatureSource featureSource = store.getFeatureSource();
            return getFeatureProperties(point, featureSource.getFeatures());
        } catch (IOException e) {
            throw new EcoregionFinderException2("lookup feature for point [" + point.toText() + "] from shapefile at [" + config.getShapeFilePath() + "]", e);
        }
    }

    private void lazyLoadStore() throws EcoregionFinderException2 {
        if (store == null) {
            URL dataStoreURL = getDataStoreURLForShapeFile(config.getShapeFilePath());
            try {
                store = FileDataStoreFinder.getDataStore(dataStoreURL);
            } catch (IOException e) {
                throw new EcoregionFinderException2("failed to load data store from url [" + dataStoreURL.toExternalForm() + "]", e);
            }
        }
    }

    public static Map<String, Object> getFeatureProperties(Point point, SimpleFeatureCollection featureCollection) {
        Map<String, Object> map = null;
        SimpleFeatureIterator features = featureCollection.features();
        try {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                Object defaultGeometry = feature.getDefaultGeometry();
                if (defaultGeometry instanceof MultiPolygon) {
                    MultiPolygon polygon = (MultiPolygon) defaultGeometry;
                    if (polygon.contains(point)) {
                        map = new TreeMap<String, Object>();
                        SimpleFeatureType featureType = feature.getFeatureType();
                        List<AttributeDescriptor> attributeDescriptors = featureType.getAttributeDescriptors();
                        for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
                            String localName = attributeDescriptor.getLocalName();
                            Object value = feature.getAttribute(localName);
                            if (value != null) {
                                map.put(attributeDescriptor.getLocalName(), value);
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
    public Collection<Ecoregion2> findEcoregion(double lat, double lng) throws EcoregionFinderException2 {
        final Map<String, Object> props = findEcoregion(GeoUtil.getPoint(lat, lng));
        return props == null || !props.containsKey(config.getIdLabel()) ? null : new ArrayList<Ecoregion2>() {{
            add(createEcoregion(props));
        }};
    }

    @Override
    public void shutdown() {
        if (store != null) {
            store.dispose();
        }
    }

    private Ecoregion2 createEcoregion(Map<String, Object> props) {
        Ecoregion2 ecoregion2;
        ecoregion2 = new Ecoregion2();
        Object obj = props.get(config.getIdLabel());
        if (obj instanceof Number) {
            obj = Integer.toString(((Number) obj).intValue());
        } else {
            obj = obj.toString();
        }
        ecoregion2.setId(config.getNamespace() + ":" + obj);
        ecoregion2.setName((String) props.get(config.getNameLabel()));
        ecoregion2.setGeometry(props.get(config.getGeometryLabel()).toString());

        StringBuilder path = new StringBuilder();
        for (String label : config.getPathLabels()) {
            if (path.length() > 0) {
                path.append(" | ");
            }
            if (props.containsKey(label)) {
                String value = props.get(label).toString();
                if (StringUtils.isNotBlank(value)) {
                    path.append(value);
                }

            }
        }
        ecoregion2.setPath(path.toString());
        return ecoregion2;
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
                resourceURI = EcoregionFinderFactoryImpl2.class.getResource(shapeFile).toURI();
            }
            LOG.info("using shapefile at [" + resourceURI.toString() + "]");
            return resourceURI.toURL();
        } catch (Exception e) {
            throw new RuntimeException("failed to find [" + shapeFile + "] ... did you run mvn install on the commandline to install shapefiles?");
        }
    }

}
