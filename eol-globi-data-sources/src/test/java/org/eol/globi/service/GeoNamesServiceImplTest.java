package org.eol.globi.service;

import org.apache.commons.lang.time.StopWatch;
import org.eol.globi.domain.TaxonomyProvider;
import org.junit.Test;
import org.eol.globi.geo.LatLng;

import java.io.IOException;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class GeoNamesServiceImplTest {

    @Test
    public void retrievePointForSpireLocalityAndCache() throws IOException {
        GeoNamesService geoNamesServiceImpl = new GeoNamesServiceImpl();
        StopWatch watch = new StopWatch();
        watch.start();
        assertVenezuela(geoNamesServiceImpl);
        watch.stop();
        long firstDurationMs = watch.getTime();

        watch.reset();

        watch.start();
        assertVenezuela(geoNamesServiceImpl);
        watch.stop();
        long secondDurationMs = watch.getTime();
        assertThat("first request should be much slower than second due to caching", firstDurationMs, is(greaterThan(10 * secondDurationMs)));
    }

    @Test
    public void retrievePointForGEONAMEIdAndCache() throws IOException {
        GeoNamesService geoNamesServiceImpl = new GeoNamesServiceImpl();
        StopWatch watch = new StopWatch();
        watch.start();
        LatLng point = lookupByGeoNameTerm(geoNamesServiceImpl);
        assertThat(point, is(notNullValue()));
        watch.stop();
        long firstDurationMs = watch.getTime();

        watch.reset();

        watch.start();
        lookupByGeoNameTerm(geoNamesServiceImpl);
        watch.stop();
        long secondDurationMs = watch.getTime();
        assertThat("first request should be much slower than second due to caching", firstDurationMs, is(greaterThan(10 * secondDurationMs)));
    }

    protected LatLng lookupByGeoNameTerm(GeoNamesService geoNamesServiceImpl) throws IOException {
        return geoNamesServiceImpl.findLatLng(TaxonomyProvider.GEONAMES.getIdPrefix() + "5532567");
    }

    private void assertVenezuela(GeoNamesService geoNamesServiceImpl) throws IOException {
        LatLng point = geoNamesServiceImpl.findLatLng("Country: Venezuela");
        assertThat(point, is(notNullValue()));
    }

    @Test
    public void assertPacific() throws IOException {
        LatLng point = new GeoNamesServiceImpl().findLatLng("Country: Pacific");
        assertThat(point.getLat(), is(3.51342));
        assertThat(point.getLng(), is(-132.1875));
    }

    @Test
    public void assertTransPecos() throws IOException {
        LatLng point = lookupByGeoNameTerm(new GeoNamesServiceImpl());
        assertThat(point, is(notNullValue()));
        assertThat(point.getLat(), is(30.70016));
        assertThat(point.getLng(), is(-103.40045));
    }

    @Test
    public void assertEarth() throws IOException {
        LatLng point = new GeoNamesServiceImpl().findLatLng("Country: General;   Locality: General");
        assertNull(point);
    }

    @Test
    public void retrievePointForNonExistingSpireLocality() throws IOException {
        LatLng point = new GeoNamesServiceImpl().findLatLng("Blabla: mickey mouse");
        assertThat(point, is(nullValue()));
    }

    @Test
    public void retrieveAnyGeoNamesId() throws IOException {
        // Half Moon Bay, http://www.geonames.org/2164089/half-moon-bay.html
        LatLng point = new GeoNamesServiceImpl().getCentroid(2164089L);
        assertThat(point, is(notNullValue()));
    }

    @Test
    public void retrieveAnyGeoNamesIdYugoslavia() throws IOException {
        // http://www.geonames.org/7500737
        LatLng point = new GeoNamesServiceImpl().getCentroid(7500737L);
        assertThat(point.getLat(), is(44.0d));
        assertThat(point.getLng(), is(19.75));
    }

    @Test
    public void findPointForLocalitys() throws IOException {
        LatLng point = new GeoNamesServiceImpl().findLatLng("Kerguelen Island");
        assertThat(point, is(notNullValue()));
    }

    @Test
    public void parseGeoIdInvalidId () {
        assertThat(GeoNamesServiceImpl.parseGeoId("GEO:"), is(nullValue()));

    }
}
