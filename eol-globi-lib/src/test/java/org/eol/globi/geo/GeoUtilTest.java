package org.eol.globi.geo;

import ch.hsr.geohash.GeoHash;
import com.jillesvangurp.geo.GeoHashUtils;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;

public class GeoUtilTest {

    protected double[][] aPolygon() {
        return new double[][]{{-22.523432424324, 40.2090980098}
                , {-22.523432424324, 40.21982983232432}
                , {-22.494234232442, 40.21982983232432}
                , {-22.494234232442, 40.2090980098}};
    }

    @Test
    public void boundingBoxToGeoHash() {
        double[][] polygon = aPolygon();
        int min = 10;
        Set<String> geoHashesForPolygon = GeoHashUtils.geoHashesForPolygon(
                7, polygon);
        for (String h : geoHashesForPolygon) {
            min = Math.min(min, h.length());
        }
        assertThat("there should be some hashes with length=3", min, is(7));
        assertThat("huge area, should generate lots of hashes",
                geoHashesForPolygon.size(), is(147));

        assertThat(GeoHash.geoHashStringWithCharacterPrecision(40.21, -22.50, 7), is("ex0hb00"));
        assertThat(GeoHashUtils.encode(40.21, -22.50, 7), is("erpuzbp"));
        assertThat(geoHashesForPolygon, hasItems("ex0hb00", "erpuzbp"));
    }

    @Test
    public void boundingBoxToGeoHash2() {
        Set<String> geoHashesForPolygon = GeoHashUtils.geoHashesForPolygon(
                7, new double[][]{{-22.10, 40.21}
                                , {-22.12, 40.21}
                                , {-22.12, 40.3}
                                , {-22.10, 40.3}});
        int min = 10;
        for (String h : geoHashesForPolygon) {
            min = Math.min(min, h.length());
        }
        assertThat("there should be some hashes with length=3", min, is(6));
        assertThat("huge area, should generate lots of hashes",
                geoHashesForPolygon.size(), is(431));

        assertThat(GeoHash.geoHashStringWithCharacterPrecision(40.22, -22.11, 7), is("ex0kbcg"));
        assertThat(GeoHashUtils.encode(40.22, -22.11, 7), is("ex0kbcg"));
        assertThat(geoHashesForPolygon, hasItems("ex0kbcg"));
    }

    @Test
    public void boundingBoxHashes() {
        double[][] polygon = new double[][]{{-1, 1}, {2, 2}, {3, -1},
                {-2, -4}};
        int min = 10;
        Set<String> geoHashesForPolygon = GeoHashUtils.geoHashesForPolygon(
                8, polygon);
        for (String h : geoHashesForPolygon) {
            min = Math.min(min, h.length());
        }
        assertThat("there should be some hashes with length=3", min, is(4));
        assertThat("huge area, should generate lots of hashes",
                geoHashesForPolygon.size(), is(greaterThan(1000)));
    }

    @Test
    public void theLake() {
        String geoHash = GeoHashUtils.encode(37.810227, -122.248362, 7);
        assertThat(geoHash, is("9q9p45j"));
        String geoHash1 = GeoHash.geoHashStringWithCharacterPrecision(37.810227, -122.248362, 7);
        assertThat(geoHash1, is("9q9p45j"));
        String geoHashPrefixes = GeoUtil.createGeoHashPrefixes(37.810227, -122.248362, 7);
        assertThat(geoHashPrefixes, is("9 9q 9q9 9q9p 9q9p4 9q9p45 9q9p45j"));
    }

}