package org.eol.globi.topo;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class EcoRegionsServiceTest {
    // http://maps.tnc.org/gis_data.html
    // Marine Ecosystems of the World (MEOW) http://maps.tnc.org/files/metadata/MEOW.xml
    // http://maps.tnc.org/files/shp/MEOW-TNC.zip
    private URL getMarineShapeFile() {
        return getDataStoreURLForShapeFile("/meow-tnc/meow_ecos.shp");
    }

    private URL getDataStoreURLForShapeFile(String shapeFile) {
        try {
            return getClass().getResource(shapeFile).toURI().toURL();
        } catch (Exception e) {
            throw new RuntimeException("failed to find [" + shapeFile + "] ... did you run mvn install on the commandline to install shapefiles?");
        }
    }

    // Fresh Water Ecosystems of the World (FEW) http://www.feow.org/
    // http://maps.tnc.org/files/metadata/FEOW.xml
    // http://maps.tnc.org/files/shp/FEOW-TNC.zip
    private URL getFreshWaterShapeFile()  {
        return getDataStoreURLForShapeFile("/feow-tnc/FEOWv1_TNC.shp");
    }


    // Terrestrial Ecosystem of the World
    // http://maps.tnc.org/files/metadata/TerrEcos.xml
    // http://maps.tnc.org/files/shp/terr-ecoregions-TNC.zip
    private URL getTerrestrialShapeFile()  {
        return getDataStoreURLForShapeFile("/teow-tnc/tnc_terr_ecoregions.shp");
    }

    @Test
    public void isMarineRegionInNorthernCalifornia() throws IOException {
        Map<String, String> props = ecoRegionInfoForPoint(pointInSanFranciscoBay(), getMarineShapeFile());
        assertThat(props.toString(), is("{ALT_CODE=53, ECOREGION=Northern California, ECO_CODE=25058, ECO_CODE_X=58, Lat_Zone=Temperate, PROVINCE=Cold Temperate Northeast Pacific, PROV_CODE=10, REALM=Temperate Northern Pacific, RLM_CODE=3}"));
    }

    @Test
    public void isFreshWaterRegionInNorthernCalifornia() throws IOException {
        Map<String, String> props = ecoRegionInfoForPoint(getPoint(37.880815, -122.142677), getFreshWaterShapeFile());
        assertThat(props.toString(), is("{ECOREGION=Sacramento - San Joaquin, ECO_ID=125, ECO_ID_U=30125, MHT_NO=5, MHT_TXT=temperate coastal rivers, OLD_ID=6}"));
    }

    @Test
    public void isFreshWaterRegionNetherlands() throws IOException {
        Map<String, String> props = ecoRegionInfoForPoint(getPoint(52.57635, 5.282593), getFreshWaterShapeFile());
        assertThat(props.toString(), is("{ECOREGION=Central & Western Europe, ECO_ID=404, ECO_ID_U=30404, MHT_NO=7, MHT_TXT=temperate floodplain rivers and wetlands, OLD_ID=306}"));
    }

    @Test
    public void isMarineRegionInSacramento() throws IOException {
        Map<String, String> props = ecoRegionInfoForPoint(pointInSacramento(), getMarineShapeFile());
        assertThat(props.toString(), is("{ALT_CODE=53, ECOREGION=Northern California, ECO_CODE=25058, ECO_CODE_X=58, Lat_Zone=Temperate, PROVINCE=Cold Temperate Northeast Pacific, PROV_CODE=10, REALM=Temperate Northern Pacific, RLM_CODE=3}"));
    }

    @Test
    public void isMarineRegionInGulfOfMexico() throws IOException {
        // for some reason the coordinate system is lng, lat.
        ecoRegionInfoForPoint(pointInGulfOfMexico(), getMarineShapeFile());
    }


    @Test
    public void terrestrialMinnesota() throws IOException {
        Point pointInMinnesota = getPoint(44.4626988695, -93.1504054967);
        Map<String, String> props = ecoRegionInfoForPoint(pointInMinnesota, getTerrestrialShapeFile());
        assertThat(props.toString(), is("{CLS_CODE=1046, ECODE_NAME=NA0415. Prairie-Forest Border, ECO_CODE=NA0415, ECO_ID_U=17020, ECO_NAME=Prairie-Forest Border, ECO_NOTES=, ECO_NUM=15, ER_DATE_U=1/1/2001, ER_RATION=NA / D. Dorfman/ECO2001, ER_UPDATE=Original, RealmMHT=NA4, SOURCEDATA=Bailey, R, modified by TNC. 2004. Ecoregions of th, WWF_MHTNAM=Temperate Broadleaf and Mixed Forests, WWF_MHTNUM=4, WWF_REALM=NA, WWF_REALM2=Nearctic}"));

    }

    @Test
    public void terrestrialGulfOfMexico() throws IOException {
        assertNull(ecoRegionInfoForPoint(pointInGulfOfMexico(), getTerrestrialShapeFile()));
    }


    @Test
    public void terrestrialSacramento() throws IOException {
        Map<String, String> props = ecoRegionInfoForPoint(pointInSacramento(), getTerrestrialShapeFile());
        assertThat(props.toString(), is("{CLS_CODE=1013, ECODE_NAME=NA1209. Great Central Valley, ECO_CODE=NA1209, ECO_ID_U=17087, ECO_NAME=Great Central Valley, ECO_NOTES=Updated MHT from 8 to 12 ref Hoeksta et al May 05 MHT posse, ECO_NUM=9, ER_DATE_U=6/1/2003, ER_RATION=Yes/June2003/LSotomayor / M. Merrifield/June2003, ER_UPDATE=Updated, RealmMHT=NA12, SOURCEDATA=Bailey, R, modified by TNC. 2004. Ecoregions of th, WWF_MHTNAM=Mediterranean Forests, Woodlands and Scrub, WWF_MHTNUM=12, WWF_REALM=NA, WWF_REALM2=Nearctic}"));
    }

    @Test
    public void terrestrialSanFranciscoBay() throws IOException {
        Map<String, String> props = ecoRegionInfoForPoint(pointInSanFranciscoBay(), getTerrestrialShapeFile());
        assertThat(props.toString(), is("{CLS_CODE=1015, ECODE_NAME=NA1201. California Central Coast, ECO_CODE=NA1201, ECO_ID_U=17085, ECO_NAME=California Central Coast, ECO_NOTES=, ECO_NUM=1, ER_DATE_U=6/1/2003, ER_RATION=Yes/June2003/LSotomayor / M. Merrifield/June2003, ER_UPDATE=Updated, RealmMHT=NA12, SOURCEDATA=Bailey, R, modified by TNC. 2004. Ecoregions of th, WWF_MHTNAM=Mediterranean Forests, Woodlands and Scrub, WWF_MHTNUM=12, WWF_REALM=NA, WWF_REALM2=Nearctic}"));
    }

    @Test
    public void terrestrialCoastalSanFranciscoBay() throws IOException {
        assertNull(ecoRegionInfoForPoint(pointInOffTheCoastOfSanFranciscoBay(), getTerrestrialShapeFile()));
    }

    public Map<String, String> ecoRegionInfoForPoint(Point pointInMinnesota, URL dataStoreURL) throws IOException {
        Map<String, String> map = null;
        FileDataStore store = FileDataStoreFinder.getDataStore(dataStoreURL);
        SimpleFeatureSource featureSource = store.getFeatureSource();
        assertThat(featureSource, is(notNullValue()));
        SimpleFeatureCollection featureCollection = featureSource.getFeatures();
        SimpleFeatureIterator features = featureCollection.features();
        while (features.hasNext()) {
            SimpleFeature feature = features.next();
            Object defaultGeometry = feature.getDefaultGeometry();
            if (defaultGeometry instanceof MultiPolygon) {
                MultiPolygon polygon = (MultiPolygon) defaultGeometry;
                if (polygon.contains(pointInMinnesota)) {
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
                            if (!"the_geom".equals(attributeDescriptor.getLocalName())) {
                                map.put(attributeDescriptor.getLocalName(), value.toString());
                            }
                        }
                    }
                    break;
                }
            }

        }
        store.dispose();
        return map;
    }

    private Point pointInSanFranciscoBay() {
        // for some reason the coordinate system is lng, lat.
        return getPoint(37.689254, -122.295799);
    }

    private Point pointInOffTheCoastOfSanFranciscoBay() {
        // for some reason the coordinate system is lng, lat.
        return getPoint(37.51844, -123.839722);
    }

    private Point getPoint(double lat, double lng) {
        return new GeometryFactory().createPoint(new Coordinate(lng, lat));
    }

    private Point pointInSacramento() {
        return getPoint(38.608286, -121.455689);
    }

    private Point pointInGulfOfMexico() {
        return getPoint(27.547242, -96.815186);
    }

}
