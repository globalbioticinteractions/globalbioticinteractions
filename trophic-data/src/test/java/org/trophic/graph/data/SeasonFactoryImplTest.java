package org.trophic.graph.data;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.trophic.graph.domain.Season;
import org.trophic.graph.repository.SeasonRepository;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/base-test-context.xml"})
@Transactional
public class SeasonFactoryImplTest {

    private SeasonFactoryImpl seasonFactory;
    private SeasonRepository seasonRepository;

    @Before
    public void create() {
        seasonFactory = new SeasonFactoryImpl();
        seasonRepository = mock(SeasonRepository.class);
        seasonFactory.setSeasonRepository(seasonRepository);
    }

    @Test
    public void getSeason() {
        when(seasonRepository.findByPropertyValue("name", "winter")).thenReturn(new Season("winter"));
        Season winter = seasonFactory.createSeason("Winter");
        assertEquals("winter", winter.getName());
    }

    @Test
    public void createSeason() {
        when(seasonRepository.findByPropertyValue("name", "winter")).thenReturn(null);
        Season winter = seasonFactory.createSeason("Winter");
        assertEquals("winter", winter.getName());
    }

}
