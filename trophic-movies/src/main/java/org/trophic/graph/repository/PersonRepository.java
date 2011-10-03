package org.trophic.graph.repository;

import org.springframework.data.neo4j.repository.GraphRepository;
import org.trophic.graph.domain.Person;

/**
 * @author mh
 * @since 02.04.11
 */
public interface PersonRepository extends GraphRepository<Person> {
}
