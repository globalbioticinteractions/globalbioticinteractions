package org.trophic.graph.repository;

import org.trophic.graph.domain.Season;

public interface SeasonRepository  {
    Season findByPropertyValue(String title, String seasonNameLower);

    long count();
}
