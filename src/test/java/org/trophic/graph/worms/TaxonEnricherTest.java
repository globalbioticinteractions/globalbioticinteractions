package org.trophic.graph.worms;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Before;
import org.junit.Test;
import org.trophic.graph.dao.SpecimentDaoJava;
import org.trophic.graph.data.GraphDBTestCase;
import org.trophic.graph.data.NodeFactoryException;
import org.trophic.graph.domain.Specimen;
import org.trophic.graph.domain.Study;
import org.trophic.graph.domain.Taxon;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class TaxonEnricherTest extends GraphDBTestCase {

    private TaxonEnricher taxonEnricher;

    public static final String[] TAXON_NAMES = new String[]{
            "Zalieutes mcgintyi",
            "Serranus atrobranchus",
            "Peprilus burti",
            "Prionotus longispinosus",
            "Neopanope sayi"
    };

    @Before
    public void init() {
        taxonEnricher = new TaxonEnricher(nodeFactory.getGraphDb());
    }

    @Test
    public void matchPredatorTaxon() throws NodeFactoryException, IOException {
        matchTaxon("Syacium gunteri");
    }

    @Test
    public void matchPreyTaxon() throws IOException, NodeFactoryException {
        enrichPreyTaxon("Syacium gunteri");
    }

    private void enrichPreyTaxon(String preyName) throws IOException, NodeFactoryException {
        Taxon taxon = nodeFactory.createTaxonOfType("blabla", Taxon.SPECIES);
        Study study = nodeFactory.createStudy("bla");
        Specimen predator = nodeFactory.createSpecimen();
        predator.classifyAs(taxon);

        Specimen prey = nodeFactory.createSpecimen();
        prey.classifyAs(nodeFactory.createTaxonOfType(preyName, Taxon.SPECIES));
        predator.ate(prey);

        study.collected(predator);

        taxonEnricher.enrichTaxons();

        Taxon taxonOfType = nodeFactory.findTaxonOfType(preyName, Taxon.SPECIES);
        assertThat("failed to match [" + preyName + "]", taxonOfType.getExternalId(), containsString("urn:lsid:marinespecies.org:taxname"));
    }

    private void matchTaxon(String speciesName) throws IOException, NodeFactoryException {
        Taxon taxon = nodeFactory.createTaxonOfType(speciesName, Taxon.SPECIES);
        Study study = nodeFactory.createStudy("bla");
        Specimen specimen = nodeFactory.createSpecimen();
        specimen.classifyAs(taxon);
        study.collected(specimen);

        taxonEnricher.enrichTaxons();

        Taxon taxonOfType = nodeFactory.findTaxonOfType(speciesName, Taxon.SPECIES);
        assertThat("failed to match [" + speciesName + "]", taxonOfType.getExternalId(), containsString("urn:lsid:marinespecies.org:taxname"));
    }

    @Test
    public void matchManyPredatorTaxons() throws IOException, NodeFactoryException {

        //warm-up
        matchTaxon("Syacium gunteri");

        StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        for (String taxonName : TAXON_NAMES) {
            matchTaxon(taxonName);
        }
        stopwatch.stop();

        float rate = 1000.0f * TAXON_NAMES.length / stopwatch.getTime();
        assertThat("rate of term matching [" + rate + "] is less than 1 term/s", rate > 1.0, is(true));
    }

    @Test
    public void matchManyPreyTaxons() throws IOException, NodeFactoryException {

        //warm-up
        enrichPreyTaxon("Syacium gunteri");

        StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        for (String taxonName : TAXON_NAMES) {
            enrichPreyTaxon(taxonName);
        }
        stopwatch.stop();

        float rate = 1000.0f * TAXON_NAMES.length / stopwatch.getTime();
        assertThat("rate of term matching [" + rate + "] is less than 1 term/s", rate > 1.0, is(true));
    }


}
