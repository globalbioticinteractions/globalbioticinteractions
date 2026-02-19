package org.globalbioticinteractions.geo;

import org.hamcrest.core.Is;
import org.junit.Test;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class GeospatialUtilTest {


    public static final CRSFactory CRS_FACTORY = new CRSFactory();

    @Test
    public void coordinateReferenceWGS84() {
        assertNotNull(getWGS84());
    }

    private static CoordinateReferenceSystem getWGS84() {
        return CRS_FACTORY.createFromName("epsg:4326");
    }

    @Test
    public void coordinateReferenceUTM() {
        assertNotNull(getUTM());
    }

    private static CoordinateReferenceSystem getUTM() {
        return CRS_FACTORY.createFromName("epsg:25833");
    }

    @Test
    public void coordinateReferenceTM65() {
        CoordinateReferenceSystem TM65 = getTM65();
        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
        CoordinateTransform transform = ctFactory.createTransform(getTM65(), getWGS84());
        ProjCoordinate target = new ProjCoordinate();
        transform.transform(new ProjCoordinate(551459, 564522), target);

        assertThat(target.x, Is.is(-2.3357990693103874d));
        assertThat(target.y, Is.is(56.1956392271589d));
        assertNotNull(TM65);
    }

    @Test
    public void coordinateReferenceTM65EastingNorthing() {
        CoordinateReferenceSystem TM65 = CRS_FACTORY.createFromName("epsg:29902");
        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
        CoordinateTransform transform = ctFactory.createTransform(getTM65(), getWGS84());
        ProjCoordinate target = new ProjCoordinate();
        transform.transform(new ProjCoordinate(151504, 64482), target);

        assertThat(target.x, Is.is(-8.704194914787191d));
        assertThat(target.y, Is.is(51.83103610578331d));
        assertNotNull(TM65);
    }

    @Test
    public void coordinateReferenceTM65ToUTM() {
        CoordinateReferenceSystem TM65 = CRS_FACTORY.createFromName("epsg:29902");
        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
        CoordinateTransform transform = ctFactory.createTransform(getTM65(), getWGS84());
        ProjCoordinate target = new ProjCoordinate();
        transform.transform(new ProjCoordinate(151504, 64482), target);

        assertThat(target.x, Is.is(-8.704194914787191d));
        assertThat(target.y, Is.is(51.83103610578331d));
        assertNotNull(TM65);
    }
    @Test
    public void coordinateFromITMToWGS84() {
        CoordinateReferenceSystem ITM = CRS_FACTORY.createFromName("epsg:2157");
        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
        assertNotNull(ITM);
        CoordinateTransform transform = ctFactory.createTransform(ITM, CRS_FACTORY.createFromName("epsg:4326"));
        ProjCoordinate target = new ProjCoordinate();
        transform.transform(new ProjCoordinate(551459, 564522), target);

        assertThat(target.x, Is.is(-8.7042706216224d));
        assertThat(target.y, Is.is(51.83082239469658d));
    }

    private static CoordinateReferenceSystem getTM65() {
        return CRS_FACTORY.createFromName("epsg:29902");
    }

}
