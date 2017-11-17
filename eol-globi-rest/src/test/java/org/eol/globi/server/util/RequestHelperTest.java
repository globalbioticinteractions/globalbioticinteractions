package org.eol.globi.server.util;

import org.eol.globi.server.QueryType;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.eol.globi.geo.LatLng;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertThat;

public class RequestHelperTest {

    @Test
    public void parseSearch() {
        Map<String, String[]> paramMap = new HashMap<>();
        List<LatLng> latLngs = RequestHelper.parseSpatialSearchParams(paramMap);
        assertThat(latLngs.size(), Is.is(0));
    }

    @Test
    public void parseSearchSinglePoint() {
        Map<String, String[]> paramMap = new HashMap<>();
        paramMap.put("lat", new String[]{"12.2"});
        paramMap.put("lng", new String[]{"12.1"});
        List<LatLng> points = RequestHelper.parseSpatialSearchParams(paramMap);
        assertThat(points.size(), Is.is(1));
        assertThat(points.get(0).getLat(), Is.is(12.2d));
        assertThat(points.get(0).getLng(), Is.is(12.1d));

        StringBuilder clause = new StringBuilder();
        RequestHelper.addSpatialClause(points, clause, QueryType.MULTI_TAXON_ALL);
        assertThat(clause.toString().trim().replaceAll("\\s+", " "), Is.is(", sourceSpecimen-[:COLLECTED_AT]->loc " +
                "WHERE has(loc.latitude) AND has(loc.longitude)" +
                " AND loc.latitude = 12.2" +
                " AND loc.longitude = 12.1"));
    }

    private void assertLocationQuery(Map<String, String[]> paramMap) {
        List<LatLng> points = RequestHelper.parseSpatialSearchParams(paramMap);
        assertThat(points.size(), Is.is(2));
        assertThat(points.get(0).getLat(), Is.is(10d));
        assertThat(points.get(0).getLng(), Is.is(-20d));
        assertThat(points.get(1).getLat(), Is.is(-10d));
        assertThat(points.get(1).getLng(), Is.is(20d));

        StringBuilder clause = new StringBuilder();
         RequestHelper.addSpatialClause(points, clause, QueryType.MULTI_TAXON_ALL);
        assertThat(clause.toString().trim().replaceAll("\\s+", " "),
                Is.is(", sourceSpecimen-[:COLLECTED_AT]->loc WHERE" +
                        " has(loc.latitude)" +
                        " AND has(loc.longitude)" +
                        " AND loc.latitude < 10.0" +
                        " AND loc.longitude > -20.0" +
                        " AND loc.latitude > -10.0" +
                        " AND loc.longitude < 20.0"));
    }

    @Test
    public void buildCypherSpatialQueryClause() {
        /*
         * from http://www.opensearch.org/Specifications/OpenSearch/Extensions/Geo/1.0/Draft_2
         * and https://github.com/jhpoelen/eol-globi-data/issues/10
         *           N <---
         *                  |
         *
         *    W             E
         *
         *    |             ^
         *    |             |
         *      ---> S -----
         *
         * assuming that four points make a rectangle
         */
        Map<String, String[]> paramMap = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-20,-10,20,10"});
            }
        };
        assertLocationQuery(paramMap);
    }

    @Test
    public void buildCypherSpatialQueryClausePoint() {
        /*
         * from http://www.opensearch.org/Specifications/OpenSearch/Extensions/Geo/1.0/Draft_2
         */
        Map<String, String[]> paramMap = new HashMap<String, String[]>() {
            {
                put("g", new String[]{"POINT(10.0 12.4)"});
            }
        };
        List<LatLng> points = RequestHelper.parseSpatialSearchParams(paramMap);
        assertThat(points.size(), Is.is(1));
        assertThat(points.get(0).getLat(), Is.is(10.0));
        assertThat(points.get(0).getLng(), Is.is(12.4d));

        StringBuilder clause = new StringBuilder();
        RequestHelper.addSpatialClause(points, clause, QueryType.MULTI_TAXON_ALL);
        assertThat(clause.toString().trim().replaceAll("\\s+", " "),
                Is.is(", sourceSpecimen-[:COLLECTED_AT]->loc WHERE" +
                        " has(loc.latitude)" +
                        " AND has(loc.longitude)" +
                        " AND loc.latitude = 10.0" +
                        " AND loc.longitude = 12.4"));
    }

    @Test(expected = NumberFormatException.class)
    public void buildInvalidPointGeometry() {
        RequestHelper.parseSpatialSearchParams(new HashMap<String, String[]>() {
            {
                put("g", new String[]{"POINT(bla blah)"});
            }
        });
    }

    @Test
    public void invalidBoundingBoxParams() {
        RequestHelper.parseSpatialSearchParams(new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"this ain't no bounding box"});
            }
        });
    }

    @Test(expected = NumberFormatException.class)
    public void invalidBadCoordinatesBoundingBoxParams() {
        RequestHelper.parseSpatialSearchParams(new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"a,b,c,d"});
            }
        });
    }

}
