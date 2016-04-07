package org.eol.globi.geo;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.util.ResourceUtil;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class EcoregionFinderImpl implements EcoregionFinder {

    private static final Log LOG = LogFactory.getLog(EcoregionFinder.class);

    private final EcoregionFinderConfig config;
    private FileDataStore store = null;

    public EcoregionFinderImpl(EcoregionFinderConfig config) {
        this.config = config;
    }

    public Map<String, Object> findEcoregion(Point point) throws EcoregionFinderException {
        lazyLoadStore();
        try {
            SimpleFeatureSource featureSource = store.getFeatureSource();
            return getFeatureProperties(point, featureSource.getFeatures());
        } catch (IOException e) {
            throw new EcoregionFinderException("lookup feature for point [" + point.toText() + "] from shapefile at [" + config.getShapeFilePath() + "]", e);
        }
    }

    private void lazyLoadStore() throws EcoregionFinderException {
        if (store == null) {
            URL dataStoreURL = getDataStoreURLForShapeFile(config.getShapeFilePath());
            try {
                store = FileDataStoreFinder.getDataStore(dataStoreURL);
            } catch (IOException e) {
                throw new EcoregionFinderException("failed to load data store from url [" + dataStoreURL.toExternalForm() + "]", e);
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
    public Collection<Ecoregion> findEcoregion(double lat, double lng) throws EcoregionFinderException {
        final Map<String, Object> props = findEcoregion(GeoUtil.getPoint(lat, lng));
        return props == null || !props.containsKey(config.getIdLabel()) ? null : new ArrayList<Ecoregion>() {{
            add(createEcoregion(props));
        }};
    }

    @Override
    public void shutdown() {
        if (store != null) {
            store.dispose();
        }
    }

    private Ecoregion createEcoregion(Map<String, Object> props) {
        Ecoregion ecoregion;
        ecoregion = new Ecoregion();
        Object obj = props.get(config.getIdLabel());
        if (obj instanceof Number) {
            obj = Integer.toString(((Number) obj).intValue());
        } else {
            obj = obj.toString();
        }
        ecoregion.setId(config.getNamespace() + ":" + obj);
        ecoregion.setName((String) props.get(config.getNameLabel()));
        ecoregion.setGeometry(props.get(config.getGeometryLabel()).toString());

        StringBuilder path = new StringBuilder();
        for (String label : config.getPathLabels()) {
            if (path.length() > 0) {
                path.append(CharsetConstant.SEPARATOR);
            }
            if (props.containsKey(label)) {
                String value = props.get(label).toString();
                if (StringUtils.isNotBlank(value)) {
                    path.append(value);
                }

            }
        }
        ecoregion.setPath(path.toString());
        return ecoregion;
    }

    private URL getDataStoreURLForShapeFile(String shapeFile) {

        try {
            URI resourceURI = ResourceUtil.fromShapefileDir(shapeFile);

            if (null == resourceURI) {
                resourceURI = EcoregionFinderFactoryImpl.class.getResource(shapeFile).toURI();
            }
            LOG.info("using shapefile at [" + resourceURI.toString() + "]");
            return resourceURI.toURL();
        } catch (Exception e) {
            throw new RuntimeException("failed to find [" + shapeFile + "] ... did you run mvn install on the commandline to install shapefiles?");
        }
    }

}
