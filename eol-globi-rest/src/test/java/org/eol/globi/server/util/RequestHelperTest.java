package org.eol.globi.server.util;

import org.eol.globi.server.QueryType;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.eol.globi.geo.LatLng;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
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
                "WHERE exists(loc.latitude) AND exists(loc.longitude)" +
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
                        " exists(loc.latitude)" +
                        " AND exists(loc.longitude)" +
                        " AND loc.latitude < 10.0" +
                        " AND loc.longitude > -20.0" +
                        " AND loc.latitude > -10.0" +
                        " AND loc.longitude < 20.0"));
    }

    @Test
    public void buildCypherSpatialQueryClause() {
        /*
         * from http://www.opensearch.org/Specifications/OpenSearch/Extensions/Geo/1.0/Draft_2
         * and https://github.com/globalbioticinteractions/globalbioticinteractions/issues/10
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
                        " exists(loc.latitude)" +
                        " AND exists(loc.longitude)" +
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

    @Test
    public void emptyData() throws IOException {
        String response = "{\"columns\":[\"source_taxon_external_id\",\"source_taxon_name\",\"source_taxon_path\",\"source_specimen_life_stage\",\"source_specimen_basis_of_record\",\"interaction_type\",\"target_taxon_external_id\",\"target_taxon_name\",\"target_taxon_path\",\"target_specimen_life_stage\",\"target_specimen_basis_of_record\",\"latitude\",\"longitude\",\"study_title\"],\"data\":[]}";
        assertThat(RequestHelper.nonEmptyData(response), Is.is(false));
    }

    @Test
    public void nonEmptyData() throws IOException {
        String response = "{\"columns\":[\"source_taxon_external_id\",\"source_taxon_name\",\"source_taxon_path\",\"source_specimen_life_stage\",\"source_specimen_basis_of_record\",\"interaction_type\",\"target_taxon_external_id\",\"target_taxon_name\",\"target_taxon_path\",\"target_specimen_life_stage\",\"target_specimen_basis_of_record\",\"latitude\",\"longitude\",\"study_title\"],\"data\":[[\"EOL_V2:328629\",\"Phoca vitulina\",\"Animalia | Chordata | Mammalia | Carnivora | Phocidae | Phoca | Phoca vitulina\",null,\"PreservedSpecimen\",\"hasParasite\",\"EOL:2857069\",\"Ascarididae\",\"Animalia | Nematoda | Secernentea | Ascaridida | Ascarididae\",null,\"PreservedSpecimen\",null,null,\"http://arctos.database.museum/guid/MSB:Para:1678\"]]}";
        assertThat(RequestHelper.nonEmptyData(response), Is.is(true));
    }

}
