package org.trophic.graph.domain;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.trophic.graph.data.GraphDBTestCase;
import org.trophic.graph.data.TaxonFactory;
import org.trophic.graph.data.TaxonFactoryException;
import org.trophic.graph.repository.StudyRepository;

import static org.junit.Assert.*;

@Ignore
public class StudyTest extends GraphDBTestCase {

    public static final String CARCHARODON = "Carcharodon";
    public static final String CARCHARODON_CARCHARIAS = CARCHARODON + " carcharias";
    public static final String CARASSIUS_AURATUS_AURATUS = "Carassius auratus auratus";
    public static final String WHITE_SHARK_FAMILY = "Lamnidae";

    protected StudyRepository studyRepository;

    private TaxonFactory factory;

    @Before
    public void createFactory() {
        factory = new TaxonFactory(getGraphDb(), null);
    }

    @Test
    public void researcherCanContributeToPaper() throws TaxonFactoryException {
        Study study = new Study("1", "Our first study").persist();


        Family family = factory.createFamily(WHITE_SHARK_FAMILY);


        Genus genus2 = factory.createGenus(CARCHARODON);
        genus2.partOf(family);
        Genus genus = genus2;

        Species greatWhiteSpecies = factory.createSpecies(genus, CARCHARODON_CARCHARIAS);

        Species goldFishSpecies = new Species(getGraphDb().createNode(), "2", CARASSIUS_AURATUS_AURATUS);

        Specimen goldFish = new Specimen("2").persist();
        goldFish.classifyAs(goldFishSpecies);

        Specimen shark = new Specimen("1").persist();
        shark.classifyAs(greatWhiteSpecies);
        Specimen fuzzyShark = new Specimen("3").persist();
        fuzzyShark.classifyAs(genus);

        shark.ate(goldFish);
        fuzzyShark.ate(goldFish);

        Location bolinasBay = new Location("1", 12.2d, 12.1d, -100.0d).persist();
        shark.caughtIn(bolinasBay);

        Season winter = new Season("1", "winter").persist();
        shark.caughtDuring(winter);
        study.getSpecimens().add(shark);
        study.getSpecimens().add(fuzzyShark);

        shark.setLengthInMm(1.2d);

        Study foundStudy = this.studyRepository.findByPropertyValue("id", "1");

        assertEquals(study, foundStudy);

        assertEquals(2, study.getSpecimens().size());

        for (Specimen specimen : study.getSpecimens()) {
            if (specimen.getId().equals("1")) {
                assertEquals(shark, specimen);
                assertEquals(1, specimen.getClassifications().size());
                Taxon species = specimen.getClassifications().iterator().next();
                assertEquals(CARCHARODON_CARCHARIAS, species.getName());
                assertTrue(species instanceof Species);
                Genus genus1 = ((Species) species).getGenus();
                assertEquals(CARCHARODON, genus1.getName());
                Family family1 = genus.getFamily();
                assertTrue(family1 instanceof Family);
                assertEquals(WHITE_SHARK_FAMILY, family.getName());
                assertEquals(new Double(-100.0d), specimen.getSampleLocation().getAltitude());
                assertEquals("winter", specimen.getSeason().getTitle());
                assertEquals(new Double(1.2d), specimen.getLengthInMm());

            } else if (specimen.getId().equals("3")) {
                Taxon actualGenus = specimen.getClassifications().iterator().next();
                assertEquals(CARCHARODON, actualGenus.getName());
                assertTrue(actualGenus instanceof Genus);
                assertEquals(1, specimen.getClassifications().size());
            } else {
                fail("found unexpected specimen [" + specimen + "] in study");
            }
        }
    }

}
