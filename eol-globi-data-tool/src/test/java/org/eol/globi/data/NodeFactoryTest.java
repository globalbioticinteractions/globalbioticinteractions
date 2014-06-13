package org.eol.globi.data;

import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.eol.globi.data.taxon.CorrectionService;
import org.eol.globi.data.taxon.TaxonService;
import org.eol.globi.data.taxon.TaxonServiceImpl;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.service.DOIResolver;
import org.eol.globi.service.TaxonPropertyEnricher;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.IndexHits;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class NodeFactoryTest extends GraphDBTestCase {

    @Test
    public void createFindLocation() throws NodeFactoryException {
        Location location = nodeFactory.getOrCreateLocation(1.2d, 1.4d, -1.0d);
        nodeFactory.getOrCreateLocation(2.2d, 1.4d, -1.0d);
        nodeFactory.getOrCreateLocation(1.2d, 2.4d, -1.0d);
        Location locationNoDepth = nodeFactory.getOrCreateLocation(1.5d, 2.8d, null);
        Assert.assertNotNull(location);
        Location location1 = nodeFactory.findLocation(location.getLatitude(), location.getLongitude(), location.getAltitude());
        Assert.assertNotNull(location1);
        Location foundLocationNoDepth = nodeFactory.findLocation(locationNoDepth.getLatitude(), locationNoDepth.getLongitude(), null);
        Assert.assertNotNull(foundLocationNoDepth);
    }

    @Test(expected = NodeFactoryException.class)
    public void createInvalidLocation() throws NodeFactoryException {
        nodeFactory.getOrCreateLocation(91.3d, -104.0d, -1.0d);
        nodeFactory.getOrCreateLocation(-100.3d, 104d, -1.0d);
        nodeFactory.getOrCreateLocation(-10.3d, -200.0d, -1.0d);
        nodeFactory.getOrCreateLocation(-20.0d, 300.0d, -1.0d);
    }

    @Test
    public void createAndFindEnvironment() throws NodeFactoryException {
        nodeFactory.setEnvoLookupService(new TermLookupService() {
            @Override
            public List<Term> lookupTermByName(String name) throws TermLookupServiceException {
                ArrayList<Term> terms = new ArrayList<Term>();
                terms.add(new Term("NS:" + name, StringUtils.replace(name, " ", "_")));
                return terms;
            }
        });
        Location location = nodeFactory.getOrCreateLocation(0.0, 1.0, 2.0);
        List<Environment> first = nodeFactory.getOrCreateEnvironments(location, "BLA:123", "this and that");
        location = nodeFactory.getOrCreateLocation(0.0, 1.0, 2.0);
        List<Environment> second = nodeFactory.getOrCreateEnvironments(location, "BLA:123", "this and that");
        assertThat(first.size(), is(second.size()));
        assertThat(first.get(0).getNodeID(), is(second.get(0).getNodeID()));
        Environment foundEnvironment = nodeFactory.findEnvironment("this_and_that");
        assertThat(foundEnvironment, is(notNullValue()));

        List<Environment> environments = location.getEnvironments();
        assertThat(environments.size(), is(1));
        Environment environment = environments.get(0);
        assertThat(environment.getNodeID(), is(foundEnvironment.getNodeID()));
        assertThat(environment.getName(), is("this_and_that"));
        assertThat(environment.getExternalId(), is("NS:this and that"));

        Location anotherLocation = nodeFactory.getOrCreateLocation(48.2, 123.1, null);
        assertThat(anotherLocation.getEnvironments().size(), is(0));
        anotherLocation.addEnvironment(environment);
        assertThat(anotherLocation.getEnvironments().size(), is(1));

        // don't add environment that has already been associated
        anotherLocation.addEnvironment(environment);
        assertThat(anotherLocation.getEnvironments().size(), is(1));

        nodeFactory.getOrCreateEnvironments(anotherLocation, "BLA:124", "that");
        assertThat(anotherLocation.getEnvironments().size(), is(2));
    }


    @Test
    public void addDOIToStudy() {
        nodeFactory.setDoiResolver(new DOIResolver() {
            @Override
            public String findDOIForReference(String reference) throws IOException {
                return "doi:1234";
            }

            @Override
            public String findCitationForDOI(String doi) throws IOException {
                return "my citation";
            }
        });
        Study study = nodeFactory.getOrCreateStudy("my title", "my contr", null, null, "some description", null, null);
        assertThat(study.getDOI(), is("doi:1234"));
        assertThat(study.getExternalId(), is("doi:1234"));
        assertThat(study.getCitation(), is("my citation"));

        nodeFactory.setDoiResolver(new DOIResolver() {
            @Override
            public String findDOIForReference(String reference) throws IOException {
                throw new IOException("kaboom!");
            }

            @Override
            public String findCitationForDOI(String doi) throws IOException {
                throw new IOException("kaboom!");
            }
        });
        study = nodeFactory.getOrCreateStudy("my other title", "my contr", null, null, "some description", null, null);
        assertThat(study.getDOI(), nullValue());
        assertThat(study.getExternalId(), nullValue());
        assertThat(study.getCitation(), nullValue());


    }

    @Test
    public void describeAndClassifySpecimenImplicit() throws NodeFactoryException {
        CorrectionService correctionService = new CorrectionService() {
            @Override
            public String correct(String taxonName) {
                return "mickey corrected";
            }
        };
        TaxonService taxonService = new TaxonServiceImpl(new TaxonPropertyEnricher() {
            @Override
            public void enrich(Taxon taxon) {

            }
        }, correctionService, getGraphDb()
        );
        nodeFactory.setTaxonService(taxonService);
        Specimen specimen = nodeFactory.createSpecimen("mickey");
        assertThat(specimen.getOriginalTaxonDescription(), is("mickey"));
        assertThat("original taxon descriptions are indexed", nodeFactory.findTaxon("mickey").getName(), is("mickey"));
    }

    @Test
    public void createEcoRegion() throws NodeFactoryException {
        Location locationA = nodeFactory.getOrCreateLocation(37.689254, -122.295799, null);
        // ensure that no duplicate node are created ...
        nodeFactory.getOrCreateLocation(37.689255, -122.295798, null);
        assertEcoRegions(locationA);
        nodeFactory.enrichLocationWithEcoRegions(locationA);
        assertEcoRegions(locationA);

        // check that multiple locations are associated to single eco region
        Location locationB = nodeFactory.getOrCreateLocation(37.689255, -122.295799, null);
        assertEcoRegions(locationB);

        IndexHits<Node> hits = nodeFactory.findCloseMatchesForEcoregion("some elo egion");
        assertThat(hits.size(), is(1));
        assertThat((String) hits.iterator().next().getProperty(PropertyAndValueDictionary.NAME), is("some eco region"));

        hits = nodeFactory.findCloseMatchesForEcoregion("mickey mouse goes shopping");
        assertThat(hits.size(), is(0));
        hits = nodeFactory.findCloseMatchesForEcoregionPath("mickey mouse goes shopping");
        assertThat(hits.size(), is(0));

        hits = nodeFactory.findCloseMatchesForEcoregionPath("path");
        assertThat(hits.size(), is(1));
        hits = nodeFactory.findCloseMatchesForEcoregionPath("some");
        assertThat(hits.size(), is(1));

        hits = nodeFactory.suggestEcoregionByName("some eco region");
        assertThat(hits.size(), is(1));
        hits = nodeFactory.suggestEcoregionByName("path");
        assertThat(hits.size(), is(1));

    }

    private void assertEcoRegions(Location location) {
        Iterable<Relationship> relationships = location.getUnderlyingNode().getRelationships(Direction.OUTGOING, RelTypes.IN_ECO_REGION);
        int count = 0;
        for (Relationship relationship : relationships) {
            Node associatedEcoRegion = relationship.getEndNode();
            assertThat((String) associatedEcoRegion.getProperty("name"), is("some eco region"));
            count++;
        }
        assertThat(count, is(1));
    }


}
