package org.trophic.graph.repository;

import org.neo4j.helpers.collection.ClosableIterable;
import org.trophic.graph.domain.Study;

public interface StudyRepository  {
    Study findByPropertyValue(String id, String s);

    ClosableIterable<Study> findAllByPropertyValue(String title, String mississippiAlabama);

    long count();
}
