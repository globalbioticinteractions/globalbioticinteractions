package org.eol.globi.data;

import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.eol.globi.data.taxon.CorrectionService;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Term;
import org.eol.globi.geo.EcoRegion;
import org.eol.globi.service.DOIResolver;
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
    public void createFindLocation() {
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

        Location anotherLocation = nodeFactory.getOrCreateLocation(123.2, 123.1, null);
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
        nodeFactory.setCorrectionService(new CorrectionService() {
            @Override
            public String correct(String taxonName) {
                return taxonName + " corrected";
            }
        });
        Specimen specimen = nodeFactory.createSpecimen("mickey");
        assertThat(specimen.getOriginalTaxonDescription(), is("mickey"));
        assertThat("original taxon descriptions are not indexed", nodeFactory.findTaxon("mickey").getName(), is(not("mickey")));
    }

    @Test
    public void createEcoRegion() throws NodeFactoryException {
        Location locationA = nodeFactory.getOrCreateLocation(37.689254, -122.295799, null);
        // ensure that no duplicate node are created ...
        nodeFactory.getOrCreateLocation(37.689255, -122.295798, null);
        List<EcoRegion> ecoRegions = nodeFactory.enrichLocationWithEcoRegions(locationA);
        assertThat(ecoRegions.size(), not(is(0)));
        EcoRegion ecoRegion = ecoRegions.get(0);
        assertThat(ecoRegion.getName(), is("some eco region"));
        assertEcoRegions(locationA);
        nodeFactory.enrichLocationWithEcoRegions(locationA);
        assertEcoRegions(locationA);

        // check that multiple locations are associated to single eco region
        Location locationB = nodeFactory.getOrCreateLocation(37.689255, -122.295799, null);
        assertEcoRegions(locationB);

        IndexHits<Node> hits = nodeFactory.findCloseMatchesForEcoRegion("some elo egion");
        assertThat(hits.size(), is(1));
        assertThat((String) hits.iterator().next().getProperty(PropertyAndValueDictionary.NAME), is("some eco region"));

        hits = nodeFactory.findCloseMatchesForEcoRegion("mickey mouse goes shopping");
        assertThat(hits.size(), is(0));
        hits = nodeFactory.findCloseMatchesForEcoRegionPath("mickey mouse goes shopping");
        assertThat(hits.size(), is(0));

        hits = nodeFactory.findCloseMatchesForEcoRegionPath("path");
        assertThat(hits.size(), is(1));
        hits = nodeFactory.findCloseMatchesForEcoRegionPath("some");
        assertThat(hits.size(), is(1));

        hits = nodeFactory.suggestEcoRegionByName("some eco region");
        assertThat(hits.size(), is(1));
        hits = nodeFactory.suggestEcoRegionByName("path");
        assertThat(hits.size(), is(1));

    }

    private void assertEcoRegions(Location locationInSanFranciscoBay) {
        Iterable<Relationship> relationships = locationInSanFranciscoBay.getUnderlyingNode().getRelationships(Direction.OUTGOING, RelTypes.IN_ECO_REGION);
        int count = 0;
        for (Relationship relationship : relationships) {
            Node associatedEcoRegion = relationship.getEndNode();
            assertThat((String) associatedEcoRegion.getProperty("name"), is("some eco region"));
            count++;
        }

        assertThat(count, is(1));
    }


}
