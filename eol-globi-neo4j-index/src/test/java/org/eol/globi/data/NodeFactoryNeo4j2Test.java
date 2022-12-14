package org.eol.globi.data;

import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.LocationNode;
import org.hamcrest.core.Is;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

public class NodeFactoryNeo4j2Test extends NodeFactoryNeo4jTest {

    public static final int MAX_INDEX_LENGTH_TIMES_TWO = ((1 << 15) - 2) * 2;

    @Override
    protected NodeFactoryNeo4j createNodeFactory() {
        NodeFactoryNeo4j2 nodeFactoryNeo4j = new NodeFactoryNeo4j2(getGraphDb());
        nodeFactoryNeo4j.setEnvoLookupService(getEnvoLookupService());
        nodeFactoryNeo4j.setTermLookupService(getTermLookupService());
        return nodeFactoryNeo4j;
    }


    @Test(expected = IllegalArgumentException.class)
    public void indexLongLocationValueString() throws NodeFactoryException {
        LocationImpl location2 = new LocationImpl(1.2d, 1.4d, -1.0d, null);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < MAX_INDEX_LENGTH_TIMES_TWO; i++) {
            builder.append("a");
        }
        location2.setLocality(builder.toString());
        location2.setLocalityId("some:id");
        LocationNode orCreateLocation = getNodeFactory().getOrCreateLocation(location2);
        assertThat(orCreateLocation, Is.is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void indexLongLocationIdString() throws NodeFactoryException {
        LocationImpl location2 = new LocationImpl(1.2d, 1.4d, -1.0d, null);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < MAX_INDEX_LENGTH_TIMES_TWO; i++) {
            builder.append("a");
        }
        location2.setLocality("some:value");
        location2.setLocalityId(builder.toString());
        LocationNode orCreateLocation = getNodeFactory().getOrCreateLocation(location2);
        assertThat(orCreateLocation, Is.is(nullValue()));
    }
}