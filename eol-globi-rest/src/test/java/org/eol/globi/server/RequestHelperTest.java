package org.eol.globi.server;

import org.hamcrest.core.Is;
import org.junit.Test;
import uk.me.jstott.jcoord.LatLng;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertThat;

public class RequestHelperTest {

    @Test
    public void parseSearch() {
        Map<String, String[]> paramMap = new HashMap<String, String[]>();
        List<LatLng> latLngs = RequestHelper.parseSpatialSearchParams(paramMap);
        assertThat(latLngs.size(), Is.is(0));
    }

    @Test
    public void parseSearchSinglePoint() {
        Map<String, String[]> paramMap = new HashMap<String, String[]>();
        paramMap.put("lat", new String[]{"12.2"});
        paramMap.put("lng", new String[]{"12.1"});
        List<LatLng> points = RequestHelper.parseSpatialSearchParams(paramMap);
        assertThat(points.size(), Is.is(1));
        assertThat(points.get(0).getLat(), Is.is(12.2d));
        assertThat(points.get(0).getLng(), Is.is(12.1d));

        String clause = RequestHelper.buildCypherSpatialWhereClause(points);
        assertThat(clause.trim(), Is.is("WHERE loc is not null AND loc.latitude = 12.2" +
                " AND loc.longitude = 12.1"));
    }

    @Test
    public void parseSearchSquare() {
        Map<String, String[]> paramMap = new HashMap<String, String[]>();

        /**
         * NW (0)  --->    NE

         *    ^              |
         *    |              |
         *    |              V
         *
         * SW      <---    SE (1)
         *
         * assuming that four points make a rectangle
         */
        paramMap.put("nw_lat", new String[]{"10"});
        paramMap.put("nw_lng", new String[]{"-20"});
        paramMap.put("se_lat", new String[]{"-10"});
        paramMap.put("se_lng", new String[]{"20"});
        List<LatLng> points = RequestHelper.parseSpatialSearchParams(paramMap);
        assertThat(points.size(), Is.is(2));
        assertThat(points.get(0).getLat(), Is.is(10d));
        assertThat(points.get(0).getLng(), Is.is(-20d));
        assertThat(points.get(1).getLat(), Is.is(-10d));
        assertThat(points.get(1).getLng(), Is.is(20d));

        String clause = RequestHelper.buildCypherSpatialWhereClause(points);
        assertThat(clause.trim(),
                Is.is("WHERE loc is not null AND loc.latitude < 10.0" +
                        " AND loc.longitude > -20.0" +
                        " AND loc.latitude > -10.0" +
                        " AND loc.longitude < 20.0"));
    }

    @Test
    public void buildCypherSpatialQueryClause() {


    }

}
