package org.eol.globi.data;

import org.eol.globi.geo.LatLng;
import org.eol.globi.util.InvalidLocationException;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class LocationUtilTest {

    @Test
    public void parseLongitudeLatitude() {
        assertThat(LocationUtil.parseDegrees("29º40'N"), is(29.0 + 40.0 / 60.0));
        assertThat(LocationUtil.parseDegrees("85º10'W"), is(-(85.0 + 10.0 / 60.0)));
        Double actual = LocationUtil.parseDegrees("85º10'11''E");
        assertThat(String.format("%.2f", actual), is(String.format("%.2f", (85.0 + 10.0 / 60.0 + 11.0 / 3600.0))));
        Double actual1 = LocationUtil.parseDegrees("29º40'01''S");
        assertThat(String.format("%.2f", actual1), is(String.format("%.2f", -(29.0 + 40.0 / 60.0 + 1.0 / 3600.0))));
    }


    @Test(expected = InvalidLocationException.class)
    public void parseLngLatInvalid() throws InvalidLocationException {
        LocationUtil.parseLatLng("123.0", "123.0");
    }

    @Test(expected = InvalidLocationException.class)
    public void parseLngLatInvalid2() throws InvalidLocationException {
        LocationUtil.parseLatLng("", "123.0");
    }

    @Test
    public void parseLngLat() throws InvalidLocationException {
        LatLng actual = LocationUtil.parseLatLng("12.0", "13.0");
        assertThat(actual.getLat(), is(12.0));
        assertThat(actual.getLng(), is(13.0));
    }

    @Test
    public void parseValidLngLat() throws InvalidLocationException {
        LatLng actual = LocationUtil.parseLatLng("12.0", "-120.0");
        assertThat(actual.getLat(), is(12.0));
        assertThat(actual.getLng(), is(-120.0));
    }

}