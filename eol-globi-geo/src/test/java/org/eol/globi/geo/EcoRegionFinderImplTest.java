package org.eol.globi.geo;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.junit.Test;

import java.util.Map;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EcoRegionFinderImplTest {

    @Test
    public void fourDifferentEcoRegionsGulfOfMexico() throws EcoRegionFinderException {
        EcoRegionFinder finder = new EcoRegionFinderFactory().createEcoRegionFinder(EcoRegionType.Marine);
        Map<String, String> ecoRegion70 = finder.findEcoRegion(26.863281, -82.756349);
        assertThat(ecoRegion70.get("ECOREGION"), is("Floridian"));
        Map<String, String> ecoRegion43 = finder.findEcoRegion(29.190533,-92.556153);
        assertThat(ecoRegion43.get("ECOREGION"), is("Northern Gulf of Mexico"));
        Map<String, String> ecoRegion60 = finder.findEcoRegion(20.179724,-92.380372);
        assertThat(ecoRegion60.get("ECOREGION"), is("Southern Gulf of Mexico"));
        Map<String, String> ecoRegion65 = finder.findEcoRegion(23.241346,-83.327638);
        assertThat(ecoRegion65.get("ECOREGION"), is("Greater Antilles"));
    }

    @Test
    public void isMarineRegionInNorthernCalifornia() throws EcoRegionFinderException {
        Map<String, String> props = findEcoRegionProperties(pointInSanFranciscoBay(), new EcoRegionFinderFactory().createEcoRegionFinder(EcoRegionType.Marine));
        assertThat(props.toString(), is("{ALT_CODE=53, ECOREGION=Northern California, ECO_CODE=25058, ECO_CODE_X=58, Lat_Zone=Temperate, PROVINCE=Cold Temperate Northeast Pacific, PROV_CODE=10, REALM=Temperate Northern Pacific, RLM_CODE=3}"));
    }

    @Test
    public void isFreshWaterRegionInNorthernCalifornia() throws EcoRegionFinderException {
        Map<String, String> props = findEcoRegionProperties(getPoint(37.880815, -122.142677), new EcoRegionFinderFactory().createEcoRegionFinder(EcoRegionType.Freshwater));
        assertThat(props.toString(), is("{ECOREGION=Sacramento - San Joaquin, ECO_ID=125, ECO_ID_U=30125, MHT_NO=5, MHT_TXT=temperate coastal rivers, OLD_ID=6}"));
    }

    @Test
    public void isFreshWaterRegionNetherlands() throws EcoRegionFinderException {
        Map<String, String> props = findEcoRegionProperties(getPoint(52.57635, 5.282593), new EcoRegionFinderFactory().createEcoRegionFinder(EcoRegionType.Freshwater));
        assertThat(props.toString(), is("{ECOREGION=Central & Western Europe, ECO_ID=404, ECO_ID_U=30404, MHT_NO=7, MHT_TXT=temperate floodplain rivers and wetlands, OLD_ID=306}"));
    }

    @Test
    public void isMarineRegionInSacramento() throws EcoRegionFinderException {
        Map<String, String> props = findEcoRegionProperties(pointInSacramento(), new EcoRegionFinderFactory().createEcoRegionFinder(EcoRegionType.Marine));
        assertThat(props.toString(), is("{ALT_CODE=53, ECOREGION=Northern California, ECO_CODE=25058, ECO_CODE_X=58, Lat_Zone=Temperate, PROVINCE=Cold Temperate Northeast Pacific, PROV_CODE=10, REALM=Temperate Northern Pacific, RLM_CODE=3}"));
    }

    @Test
    public void isMarineRegionInGulfOfMexico() throws EcoRegionFinderException {
        // for some reason the coordinate system is lng, lat.
        Map<String, String> props = findEcoRegionProperties(pointInGulfOfMexico(), new EcoRegionFinderFactory().createEcoRegionFinder(EcoRegionType.Marine));
        assertThat(props.get("ECOREGION"), is("Northern Gulf of Mexico"));
    }


    @Test
    public void terrestrialMinnesota() throws EcoRegionFinderException {
        Point pointInMinnesota = getPoint(44.4626988695, -93.1504054967);
        Map<String, String> props = findEcoRegionProperties(pointInMinnesota, new EcoRegionFinderFactory().createEcoRegionFinder(EcoRegionType.Terrestrial));
        assertThat(props.toString(), is("{CLS_CODE=1046, ECODE_NAME=NA0415. Prairie-Forest Border, ECO_CODE=NA0415, ECO_ID_U=17020, ECO_NAME=Prairie-Forest Border, ECO_NOTES=, ECO_NUM=15, ER_DATE_U=1/1/2001, ER_RATION=NA / D. Dorfman/ECO2001, ER_UPDATE=Original, RealmMHT=NA4, SOURCEDATA=Bailey, R, modified by TNC. 2004. Ecoregions of th, WWF_MHTNAM=Temperate Broadleaf and Mixed Forests, WWF_MHTNUM=4, WWF_REALM=NA, WWF_REALM2=Nearctic}"));

    }

    @Test
    public void terrestrialGulfOfMexico() throws EcoRegionFinderException {
        assertNull(findEcoRegionProperties(pointInGulfOfMexico(), new EcoRegionFinderFactory().createEcoRegionFinder(EcoRegionType.Terrestrial)));
    }


    @Test
    public void terrestrialSacramento() throws EcoRegionFinderException {
        Map<String, String> props = findEcoRegionProperties(pointInSacramento(), new EcoRegionFinderFactory().createEcoRegionFinder(EcoRegionType.Terrestrial));
        assertThat(props.toString(), is("{CLS_CODE=1013, ECODE_NAME=NA1209. Great Central Valley, ECO_CODE=NA1209, ECO_ID_U=17087, ECO_NAME=Great Central Valley, ECO_NOTES=Updated MHT from 8 to 12 ref Hoeksta et al May 05 MHT posse, ECO_NUM=9, ER_DATE_U=6/1/2003, ER_RATION=Yes/June2003/LSotomayor / M. Merrifield/June2003, ER_UPDATE=Updated, RealmMHT=NA12, SOURCEDATA=Bailey, R, modified by TNC. 2004. Ecoregions of th, WWF_MHTNAM=Mediterranean Forests, Woodlands and Scrub, WWF_MHTNUM=12, WWF_REALM=NA, WWF_REALM2=Nearctic}"));
    }

    @Test
    public void terrestrialSanFranciscoBay() throws EcoRegionFinderException {
        Map<String, String> props = findEcoRegionProperties(pointInSanFranciscoBay(), new EcoRegionFinderFactory().createEcoRegionFinder(EcoRegionType.Terrestrial));
        assertThat(props.toString(), is("{CLS_CODE=1015, ECODE_NAME=NA1201. California Central Coast, ECO_CODE=NA1201, ECO_ID_U=17085, ECO_NAME=California Central Coast, ECO_NOTES=, ECO_NUM=1, ER_DATE_U=6/1/2003, ER_RATION=Yes/June2003/LSotomayor / M. Merrifield/June2003, ER_UPDATE=Updated, RealmMHT=NA12, SOURCEDATA=Bailey, R, modified by TNC. 2004. Ecoregions of th, WWF_MHTNAM=Mediterranean Forests, Woodlands and Scrub, WWF_MHTNUM=12, WWF_REALM=NA, WWF_REALM2=Nearctic}"));
    }

    @Test
    public void terrestrialCoastalSanFranciscoBay() throws EcoRegionFinderException {
        assertNull(findEcoRegionProperties(pointInOffTheCoastOfSanFranciscoBay(), new EcoRegionFinderFactory().createEcoRegionFinder(EcoRegionType.Terrestrial)));
    }

    public Map<String, String> findEcoRegionProperties(Point point, EcoRegionFinder finder) throws EcoRegionFinderException {
        return finder.findEcoRegion(point);
    }

    private Point pointInSanFranciscoBay() {
        return getPoint(37.689254, -122.295799);
    }

    private Point pointInOffTheCoastOfSanFranciscoBay() {
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
