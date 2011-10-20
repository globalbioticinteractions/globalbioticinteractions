package org.trophic.graph.repository;

import org.neo4j.helpers.collection.ClosableIterable;
import org.trophic.graph.domain.Taxon;

public interface TaxonRepository  {
    ClosableIterable<Taxon> findAllByPropertyValue(String name, String taxonName);

    long count();
}