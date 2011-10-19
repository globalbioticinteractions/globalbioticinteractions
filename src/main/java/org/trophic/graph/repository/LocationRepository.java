package org.trophic.graph.repository;

import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.repository.NamedIndexRepository;
import org.trophic.graph.domain.Location;

public interface LocationRepository extends GraphRepository<Location>,
		NamedIndexRepository<Location> {
}
