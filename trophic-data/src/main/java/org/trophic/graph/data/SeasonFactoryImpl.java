package org.trophic.graph.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.trophic.graph.domain.Season;
import org.trophic.graph.repository.SeasonRepository;

@Component
public class SeasonFactoryImpl implements SeasonFactory {

    @Autowired
    SeasonRepository seasonRepository;

    @Override
    public Season createSeason(String seasonName) {
        String seasonNameLower = seasonName.toLowerCase();
        Season season = seasonRepository.findByPropertyValue("name", seasonNameLower);
        if (null == season) {
            season = new Season(seasonNameLower).persist();
        }
        return season;
    }

    public void setSeasonRepository(SeasonRepository seasonRepository) {
        this.seasonRepository = seasonRepository;
    }
}
