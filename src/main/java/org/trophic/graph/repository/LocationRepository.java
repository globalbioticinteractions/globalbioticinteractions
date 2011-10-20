package org.trophic.graph.repository;

import org.neo4j.helpers.collection.ClosableIterable;
import org.trophic.graph.domain.Location;

public interface LocationRepository  {
    ClosableIterable<Location> findAllByPropertyValue(String longitude, Double longitude1);

    long count();
}
