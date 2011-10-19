package org.trophic.graph.repository;

import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.repository.NamedIndexRepository;
import org.trophic.graph.domain.Paper;

public interface PaperRepository extends GraphRepository<Paper>, NamedIndexRepository<Paper> {
}
