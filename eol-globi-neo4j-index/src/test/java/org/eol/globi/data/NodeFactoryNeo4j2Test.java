package org.eol.globi.data;

import org.eol.globi.domain.Environment;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.LocationNode;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

public class NodeFactoryNeo4j2Test extends NodeFactoryNeo4jTest {

    public static final int MAX_INDEX_LENGTH_TIMES_TWO = NodeFactoryNeo4j2.MAX_NEO4J_INDEX_LENGTH * 2;

    @Override
    protected NodeFactoryNeo4j createNodeFactory() {
        NodeFactoryNeo4j2 nodeFactoryNeo4j = new NodeFactoryNeo4j2(getGraphDb());
        nodeFactoryNeo4j.setEnvoLookupService(getEnvoLookupService());
        nodeFactoryNeo4j.setTermLookupService(getTermLookupService());
        return nodeFactoryNeo4j;
    }


    @Test
    public void indexLongLocationValueString() throws NodeFactoryException {
        LocationImpl location2 = new LocationImpl(1.2d, 1.4d, -1.0d, null);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < MAX_INDEX_LENGTH_TIMES_TWO; i++) {
            builder.append("a");
        }
        String longLocalityString = builder.toString();
        location2.setLocality(longLocalityString);
        location2.setLocalityId("some:id");
        LocationNode orCreateLocation = getNodeFactory().getOrCreateLocation(location2);
        assertThat(orCreateLocation.getLocality(), Is.is(longLocalityString));
    }

    @Test
    public void indexLongLocationIdString() throws NodeFactoryException {
        LocationImpl location2 = new LocationImpl(1.2d, 1.4d, -1.0d, null);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < MAX_INDEX_LENGTH_TIMES_TWO; i++) {
            builder.append("a");
        }
        final String veryLongLocationIdString = builder.toString();
        location2.setLocality("some:value");
        location2.setLocalityId(veryLongLocationIdString);
        LocationNode orCreateLocation = getNodeFactory().getOrCreateLocation(location2);
        assertThat(orCreateLocation.getLocalityId().length(), Is.is(veryLongLocationIdString.length()));


        Location locationQueryNotTruncated = createLocationQuery(veryLongLocationIdString);
        assertThat(getNodeFactory().findLocation(locationQueryNotTruncated), Is.is(nullValue()));
    }

    private Location createLocationQuery(String longValue) {
        return new Location() {

            @Override
            public Double getAltitude() {
                return null;
            }

            @Override
            public Double getLongitude() {
                return null;
            }

            @Override
            public Double getLatitude() {
                return null;
            }

            @Override
            public String getFootprintWKT() {
                return null;
            }

            @Override
            public String getLocality() {
                return null;
            }

            @Override
            public String getLocalityId() {
                return longValue;
            }

            @Override
            public void addEnvironment(Environment environment) {

            }

            @Override
            public List<Environment> getEnvironments() {
                return null;
            }
        };
    }
}