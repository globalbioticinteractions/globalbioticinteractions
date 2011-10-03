package org.trophic.graph.repository;

import org.springframework.data.neo4j.repository.GraphRepository;
import org.trophic.graph.domain.User;

/**
 * @author mh
 * @since 02.04.11
 */
public interface UserRepository extends GraphRepository<User> {
}
