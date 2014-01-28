package org.eol.globi.data;

import junit.framework.Assert;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.eol.globi.data.taxon.CorrectionService;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.geo.EcoRegion;
import org.eol.globi.service.DOIResolver;
import org.eol.globi.service.TaxonPropertyEnricher;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.io.IOException;
import java.util.List;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class NodeFactoryTest extends GraphDBTestCase {

    public static final String EXPECTED_COMMON_NAMES = "some german name @de" + CharsetConstant.SEPARATOR + "some english name @en" + CharsetConstant.SEPARATOR;

    @Test
    public void findByStringWithWhitespaces() throws NodeFactoryException {
        nodeFactory = new NodeFactory(getGraphDb(), new TaxonPropertyEnricher() {
            @Override
            public void enrich(Taxon taxon) {
                taxon.setPath("kingdom" + CharsetConstant.SEPARATOR + "phylum" + CharsetConstant.SEPARATOR + "Homo sapiens" + CharsetConstant.SEPARATOR);
                taxon.setExternalId("anExternalId");
                taxon.setCommonNames(EXPECTED_COMMON_NAMES);
                taxon.setName("this is the actual name");
            }
        });
        nodeFactory.getOrCreateTaxon("Homo sapiens");

        assertThat(nodeFactory.getGraphDb().index().existsForNodes("taxonNameSuggestions"), is(true));
        Index<Node> index = nodeFactory.getGraphDb().index().forNodes("taxonNameSuggestions");
        Query query = new TermQuery(new Term("name", "name"));
        IndexHits<Node> hits = index.query(query);
        assertThat(hits.size(), is(1));

        hits = index.query("name", "s nme~");
        assertThat(hits.size(), is(1));

        hits = index.query("name", "geRman~");
        assertThat(hits.size(), is(1));

        hits = index.query("name:geRman~ AND name:som~");
        assertThat(hits.size(), is(1));

        hits = index.query("name:hmo~ AND name:SApiens~");
        assertThat(hits.size(), is(1));

        hits = index.query("name:hmo~ AND name:sapiens~");
        assertThat(hits.size(), is(1));

        // queries are case sensitive . . . should all be lower cased.
        hits = index.query("name:HMO~ AND name:saPIENS~");
        assertThat(hits.size(), is(0));


    }

    @Test
    public void ensureThatEnrichedPropertiesAreIndexed() throws NodeFactoryException {
        nodeFactory = new NodeFactory(getGraphDb(), new TaxonPropertyEnricher() {
            @Override
            public void enrich(Taxon taxon) {
                taxon.setPath("kingdom" + CharsetConstant.SEPARATOR + "phylum" + CharsetConstant.SEPARATOR + "etc" + CharsetConstant.SEPARATOR);
                taxon.setExternalId("anExternalId");
                taxon.setCommonNames(EXPECTED_COMMON_NAMES);
            }
        });

        assertThat(getGraphDb().index().existsForNodes("taxonCommonNames"), is(true));
        assertThat(getGraphDb().index().existsForNodes("taxons"), is(true));
        assertThat(getGraphDb().index().existsForNodes("taxonpaths"), is(true));
        assertThat(getGraphDb().index().existsForNodes("taxonNameSuggestions"), is(true));
        assertThat(getGraphDb().index().existsForNodes("thisDoesnoTExist"), is(false));

        assertEnrichedPropertiesSet(nodeFactory.getOrCreateTaxon("some name"));
        assertEnrichedPropertiesSet(nodeFactory.findTaxon("some name"));
        IndexHits<Node> hits = nodeFactory.findTaxaByPath("etc");
        assertThat(hits.size(), is(1));
        assertEnrichedPropertiesSet(new TaxonNode(hits.getSingle()));
        hits = nodeFactory.findTaxaByCommonName("some german name");
        assertThat(hits.size(), is(1));
        assertEnrichedPropertiesSet(new TaxonNode(hits.getSingle()));

        hits = nodeFactory.suggestTaxaByName("kingdom");
        assertThat(hits.size(), is(1));
        assertEnrichedPropertiesSet(new TaxonNode(hits.getSingle()));

        hits = nodeFactory.suggestTaxaByName("phylum");
        assertThat(hits.size(), is(1));
        assertEnrichedPropertiesSet(new TaxonNode(hits.getSingle()));

        hits = nodeFactory.suggestTaxaByName("some");
        assertThat(hits.size(), is(1));
        assertEnrichedPropertiesSet(new TaxonNode(hits.getSingle()));

        hits = nodeFactory.suggestTaxaByName("german");
        assertThat(hits.size(), is(1));
        assertEnrichedPropertiesSet(new TaxonNode(hits.getSingle()));

        hits = nodeFactory.suggestTaxaByName("@de");
        assertThat(hits.size(), is(1));
        assertEnrichedPropertiesSet(new TaxonNode(hits.getSingle()));
    }

    private void assertEnrichedPropertiesSet(TaxonNode aTaxon) {
        assertThat(aTaxon.getPath(), is("kingdom" + CharsetConstant.SEPARATOR + "phylum" + CharsetConstant.SEPARATOR + "etc" + CharsetConstant.SEPARATOR));
        assertThat(aTaxon.getCommonNames(), is(EXPECTED_COMMON_NAMES));
        assertThat(aTaxon.getName(), is("some name"));
        assertThat(aTaxon.getExternalId(), is("anExternalId"));
    }

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
        Location location = nodeFactory.getOrCreateLocation(0.0, 1.0, 2.0);
        List<Environment> first = nodeFactory.getOrCreateEnvironments(location, "BLA:123", "this and that");
        location = nodeFactory.getOrCreateLocation(0.0, 1.0, 2.0);
        List<Environment> second = nodeFactory.getOrCreateEnvironments(location, "BLA:123", "this and that");
        assertThat(first.size(), is(second.size()));
        assertThat(first.get(0).getNodeID(), is(second.get(0).getNodeID()));
        Environment foundEnvironment = nodeFactory.findEnvironment("this and that");
        assertThat(foundEnvironment, is(notNullValue()));

        List<Environment> environments = location.getEnvironments();
        assertThat(environments.size(), is(1));
        Environment environment = environments.get(0);
        assertThat(environment.getNodeID(), is(foundEnvironment.getNodeID()));
        assertThat(environment.getName(), is("this and that"));

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
    public void createTaxon() throws NodeFactoryException {
        TaxonNode taxon = nodeFactory.getOrCreateTaxon("bla bla");
        assertThat(taxon, is(notNullValue()));
        assertEquals("bla bla", taxon.getName());
    }

    @Test
    public void createSpeciesMatchHigherOrder() throws NodeFactoryException {
        nodeFactory = new NodeFactory(getGraphDb(), new TaxonPropertyEnricher() {
            @Override
            public void enrich(Taxon taxon) {
                if ("bla".equals(taxon.getName())) {
                    taxon.setPath("a path");
                    taxon.setExternalId("anExternalId");
                    taxon.setCommonNames(EXPECTED_COMMON_NAMES);
                }
            }
        });

        TaxonNode taxon = nodeFactory.getOrCreateTaxon("bla bla");
        assertEquals("bla", taxon.getName());
        assertEquals("a path", taxon.getPath());
        assertEquals("anExternalId", taxon.getExternalId());

        taxon = nodeFactory.getOrCreateTaxon("bla bla boo");
        assertEquals("bla", taxon.getName());
        assertEquals("a path", taxon.getPath());
        assertEquals("anExternalId", taxon.getExternalId());

        taxon = nodeFactory.getOrCreateTaxon("boo bla");
        assertEquals("boo bla", taxon.getName());
        assertThat(taxon.getExternalId(), is(PropertyAndValueDictionary.NO_MATCH));
        assertNull(taxon.getPath());
    }

    @Test
    public void findCloseMatchForTaxonPath() throws NodeFactoryException {
        TaxonNode homoSapiens = nodeFactory.getOrCreateTaxon("Homo sapiens", null, "Animalia Mammalia");
        Transaction transaction = homoSapiens.getUnderlyingNode().getGraphDatabase().beginTx();
        transaction.success();
        transaction.finish();
        nodeFactory.getOrCreateTaxon("Homo erectus");
        assertMatch("Mammalia");
        assertMatch("Mammali");
        assertMatch("mammali");
        assertMatch("inmalia");
        IndexHits<Node> hits = nodeFactory.findCloseMatchesForTaxonPath("il");
        assertThat(hits.hasNext(), is(false));
    }

    private void assertMatch(String taxonRankOfClassName) {
        IndexHits<Node> hits = nodeFactory.findCloseMatchesForTaxonPath(taxonRankOfClassName);
        assertThat(hits.hasNext(), is(true));
        Node firstHit = hits.next();
        assertThat((String) firstHit.getProperty(PropertyAndValueDictionary.NAME), is("Homo sapiens"));
        assertThat((String) firstHit.getProperty(PropertyAndValueDictionary.PATH), is("Animalia Mammalia"));
        assertThat(hits.hasNext(), is(false));
    }

    @Test
    public void findCloseMatch() throws NodeFactoryException {
        nodeFactory.getOrCreateTaxon("Homo sapiens");
        IndexHits<Node> hits = nodeFactory.findCloseMatchesForTaxonName("Homo sapiens");
        assertThat(hits.hasNext(), is(true));
        hits.close();
        hits = nodeFactory.findCloseMatchesForTaxonName("Homo saliens");
        assertThat(hits.hasNext(), is(true));
        hits = nodeFactory.findCloseMatchesForTaxonName("Homo");
        assertThat(hits.hasNext(), is(true));
        hits = nodeFactory.findCloseMatchesForTaxonName("homo sa");
        assertThat(hits.hasNext(), is(true));
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
    public void ensureCorrectedIndexing() throws NodeFactoryException {
        nodeFactory.setCorrectionService(new CorrectionService() {
            @Override
            public String correct(String taxonName) {
                String corrected = taxonName;
                if (!taxonName.endsWith("corrected")) {
                    corrected = taxonName + " corrected";
                }
                return corrected;
            }
        });
        TaxonNode taxon = nodeFactory.getOrCreateTaxon("bla");
        assertEquals("bla corrected", taxon.getName());

        TaxonNode bla = nodeFactory.findTaxonOfType("bla");
        assertThat(bla.getName(), is("bla corrected"));

        TaxonNode taxonMatch = nodeFactory.findTaxonOfType("bla corrected");
        assertThat(taxonMatch.getName(), is("bla corrected"));
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
        Location locationInSanFranciscoBay = nodeFactory.getOrCreateLocation(37.689254, -122.295799, null);
        // ensure that no duplicate node are created ...
        nodeFactory.getOrCreateLocation(37.689255, -122.295798, null);
        List<EcoRegion> ecoRegions = nodeFactory.getOrCreateEcoRegions(locationInSanFranciscoBay);
        assertThat(ecoRegions.size(), not(is(0)));
        EcoRegion ecoRegion = ecoRegions.get(0);
        assertThat(ecoRegion.getName(), is("some eco region"));
        assertEcoRegions(locationInSanFranciscoBay);
        nodeFactory.getOrCreateEcoRegions(locationInSanFranciscoBay);
        assertEcoRegions(locationInSanFranciscoBay);

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
