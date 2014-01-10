package org.eol.globi.service;

import org.apache.commons.lang.time.StopWatch;
import org.junit.Test;
import uk.me.jstott.jcoord.LatLng;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class GeoNamesServiceTest {

    @Test
    public void retrievePointForSpireLocalityAndCache() throws IOException {
        GeoNamesService geoNamesService = new GeoNamesService();
        StopWatch watch = new StopWatch();
        watch.start();
        assertVenezuela(geoNamesService);
        watch.stop();
        long firstDurationMs = watch.getTime();

        watch.reset();

        watch.start();
                assertVenezuela(geoNamesService);
                watch.stop();
        long secondDurationMs = watch.getTime();
        assertThat("first request should be much slower than second due to caching", firstDurationMs, is(greaterThan(10 * secondDurationMs)));
    }

    private void assertVenezuela(GeoNamesService geoNamesService) throws IOException {
        LatLng point = geoNamesService.findLatLngForSPIRELocality("Country: Venezuela");
        assertThat(point, is(notNullValue()));
    }

    @Test
    public void retrievePointForNonExistingSpireLocality() throws IOException {
        LatLng point = new GeoNamesService().findLatLngForSPIRELocality("Blabla: mickey mouse");
        assertThat(point, is(nullValue()));
    }

    @Test
    public void retrieveAnyGeoNamesId() throws IOException {
        // Half Moon Bay, http://www.geonames.org/2164089/half-moon-bay.html
        LatLng point = new GeoNamesService().findLatLng(2164089L);
        assertThat(point, is(notNullValue()));
    }
}
