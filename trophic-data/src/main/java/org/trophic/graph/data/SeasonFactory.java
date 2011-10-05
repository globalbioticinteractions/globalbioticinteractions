package org.trophic.graph.data;

import org.trophic.graph.domain.Season;
import org.trophic.graph.repository.SeasonRepository;

public interface SeasonFactory {

    Season createSeason(String seasonName);

}
