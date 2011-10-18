package org.trophic.graph.repository;

import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.repository.NamedIndexRepository;
import org.trophic.graph.domain.Species;
import org.trophic.graph.domain.Taxon;

public interface TaxonRepository extends GraphRepository<Taxon>,
		NamedIndexRepository<Taxon> {
}