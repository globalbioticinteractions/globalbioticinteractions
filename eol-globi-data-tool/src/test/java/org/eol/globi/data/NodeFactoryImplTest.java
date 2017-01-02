package org.eol.globi.data;

import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.EnvironmentNode;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.LocationNode;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Term;
import org.eol.globi.service.DOIResolver;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.taxon.CorrectionService;
import org.eol.globi.taxon.TaxonIndexImpl;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.IndexHits;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class NodeFactoryImplTest extends GraphDBTestCase {

    @Test
    public void toCitation() {
        assertThat(ExternalIdUtil.toCitation(null, null, null), is(""));
    }

    @Test
    public void createInteraction() throws NodeFactoryException {
        StudyNode study = getNodeFactory().createStudy("bla");
        SpecimenNode specimen = getNodeFactory().createSpecimen(study, "Donalda duckus");
        SpecimenNode specimen1 = getNodeFactory().createSpecimen(study, "Mickeya mouseus");
        specimen.interactsWith(specimen1, InteractType.ATE);
        final Iterator<Relationship> relIter = specimen.getUnderlyingNode().getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(InteractType.ATE)).iterator();
        assertThat(relIter.hasNext(), is(true));
        final Relationship rel = relIter.next();
        assertThat(rel.getProperty("iri").toString(), is("http://purl.obolibrary.org/obo/RO_0002470"));
        assertThat(rel.getProperty("label").toString(), is("eats"));

        Iterable<Relationship> relationships = specimen1.getUnderlyingNode().getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(InteractType.EATEN_BY));
        Iterator<Relationship> iterator = relationships.iterator();
        assertThat(iterator.hasNext(), is(true));
        Relationship relInverted = iterator.next();
        assertThat(relInverted.getProperty("iri").toString(), is("http://purl.obolibrary.org/obo/RO_0002471"));
        assertThat(relInverted.getProperty("label").toString(), is("eatenBy"));
    }

    @Test
    public void createStudyDOIlookup() throws NodeFactoryException {
        getNodeFactory().setDoiResolver(new DOIResolver() {
            @Override
            public String findDOIForReference(String reference) throws IOException {
                throw new IOException("kaboom!");
            }

            @Override
            public String findCitationForDOI(String doi) throws IOException {
                throw new IOException("kaboom!");
            }
        });
        Study study = getNodeFactory().getOrCreateStudy("title", "some source", "some citation");
        assertThat(study.getSource(), is("some source"));
        assertThat(study.getCitation(), is("some citation"));
        assertThat(study.getTitle(), is("title"));
    }

    @Test
    public void createStudyDOIlookupCitationWithURL() throws NodeFactoryException {
        getNodeFactory().setDoiResolver(new DOIResolver() {
            @Override
            public String findDOIForReference(String reference) throws IOException {
                fail("should not call this");
                return "bla";
            }

            @Override
            public String findCitationForDOI(String doi) throws IOException {
                fail("should not call this");
                return "bla";
            }
        });
        Study study = getNodeFactory().getOrCreateStudy("title", "some source", "http://bla");
        assertThat(study.getSource(), is("some source"));
        assertThat(study.getCitation(), is("http://bla"));
        assertThat(study.getTitle(), is("title"));
    }


    @Test
    public void createFindLocation() throws NodeFactoryException {
        Location location = getNodeFactory().getOrCreateLocation(1.2d, 1.4d, -1.0d);
        getNodeFactory().getOrCreateLocation(2.2d, 1.4d, -1.0d);
        getNodeFactory().getOrCreateLocation(1.2d, 2.4d, -1.0d);
        Location locationNoDepth = getNodeFactory().getOrCreateLocation(1.5d, 2.8d, null);
        Assert.assertNotNull(location);
        Location location1 = getNodeFactory().findLocation(location.getLatitude(), location.getLongitude(), location.getAltitude());
        Assert.assertNotNull(location1);
        Location foundLocationNoDepth = getNodeFactory().findLocation(locationNoDepth.getLatitude(), locationNoDepth.getLongitude(), null);
        Assert.assertNotNull(foundLocationNoDepth);
    }

    @Test
    public void createFindLocationWith() throws NodeFactoryException {
        Location location = getNodeFactory().getOrCreateLocation(1.2d, 1.4d, -1.0d);
        getNodeFactory().getOrCreateLocation(2.2d, 1.4d, -1.0d);
        getNodeFactory().getOrCreateLocation(1.2d, 2.4d, -1.0d);
        Location locationNoDepth = getNodeFactory().getOrCreateLocation(1.5d, 2.8d, null);
        Assert.assertNotNull(location);
        LocationNode location1 = getNodeFactory().findLocation(location.getLatitude(), location.getLongitude(), location.getAltitude());
        Assert.assertNotNull(location1);
        LocationNode foundLocationNoDepth = getNodeFactory().findLocation(locationNoDepth.getLatitude(), locationNoDepth.getLongitude(), null);
        Assert.assertNotNull(foundLocationNoDepth);
    }

    @Test
    public void createFindLocationWKT() throws NodeFactoryException {
        Location location = getNodeFactory().getOrCreateLocation(2.0d, 1.0d, -1.0d);
        assertThat(location.getFootprintWKT(), is(nullValue()));
        final String expectedFootprintWKT = "POLYGON((10 20, 11 20, 11 21, 10 21, 10 20))";
        final LocationImpl otherLocation = new LocationImpl(location.getAltitude(), location.getLongitude(), location.getLatitude(),
                expectedFootprintWKT);

        final LocationNode locationWithFootprintWKT = getNodeFactory().getOrCreateLocation(otherLocation);
        assertThat(locationWithFootprintWKT.getFootprintWKT(), is(expectedFootprintWKT));
        assertThat(getNodeFactory().findLocation(otherLocation).getFootprintWKT(), is(expectedFootprintWKT));

        final LocationImpl yetAnotherLocation = new LocationImpl(location.getAltitude(), location.getLongitude(), location.getLatitude(),
                expectedFootprintWKT);
        yetAnotherLocation.setLocality("this is my place");
        getNodeFactory().getOrCreateLocation(yetAnotherLocation);

        assertThat(getNodeFactory().findLocation(yetAnotherLocation).getLocality(), is("this is my place"));
    }

    @Test(expected = NodeFactoryException.class)
    public void createInvalidLocation() throws NodeFactoryException {
        getNodeFactory().getOrCreateLocation(91.3d, -104.0d, -1.0d);
        getNodeFactory().getOrCreateLocation(-100.3d, 104d, -1.0d);
        getNodeFactory().getOrCreateLocation(-10.3d, -200.0d, -1.0d);
        getNodeFactory().getOrCreateLocation(-20.0d, 300.0d, -1.0d);
    }

    private NodeFactoryImpl getNodeFactory() {
        return (NodeFactoryImpl) nodeFactory;
    }

    @Test
    public void createAndFindEnvironment() throws NodeFactoryException {
        getNodeFactory().setEnvoLookupService(new TermLookupService() {
            @Override
            public List<Term> lookupTermByName(String name) throws TermLookupServiceException {
                ArrayList<Term> terms = new ArrayList<Term>();
                terms.add(new Term("NS:" + name, StringUtils.replace(name, " ", "_")));
                return terms;
            }
        });
        Location location = getNodeFactory().getOrCreateLocation(0.0, 1.0, 2.0);
        List<Environment> first = getNodeFactory().getOrCreateEnvironments(location, "BLA:123", "this and that");
        location = getNodeFactory().getOrCreateLocation(0.0, 1.0, 2.0);
        List<Environment> second = getNodeFactory().getOrCreateEnvironments(location, "BLA:123", "this and that");
        assertThat(first.size(), is(second.size()));
        assertThat(((NodeBacked)first.get(0)).getNodeID(), is(((NodeBacked)second.get(0)).getNodeID()));
        EnvironmentNode foundEnvironment = getNodeFactory().findEnvironment("this_and_that");
        assertThat(foundEnvironment, is(notNullValue()));

        List<Environment> environments = location.getEnvironments();
        assertThat(environments.size(), is(1));
        Environment environment = environments.get(0);
        NodeBacked environmentNode = (NodeBacked) environment;
        assertThat(environmentNode.getNodeID(), is(foundEnvironment.getNodeID()));
        assertThat(environment.getName(), is("this_and_that"));
        assertThat(environment.getExternalId(), is("NS:this and that"));

        Location anotherLocation = getNodeFactory().getOrCreateLocation(48.2, 123.1, null);
        LocationNode anotherLocationNode = (LocationNode) anotherLocation;
        assertThat(anotherLocationNode.getEnvironments().size(), is(0));
        anotherLocationNode.addEnvironment((EnvironmentNode)environment);
        assertThat(anotherLocationNode.getEnvironments().size(), is(1));

        // don't add environment that has already been associated
        anotherLocationNode.addEnvironment(environment);
        assertThat(anotherLocationNode.getEnvironments().size(), is(1));

        getNodeFactory().getOrCreateEnvironments(anotherLocation, "BLA:124", "that");
        assertThat(anotherLocationNode.getEnvironments().size(), is(2));
    }


    @Test
    public void addDOIToStudy() throws NodeFactoryException {
        getNodeFactory().setDoiResolver(new DOIResolver() {
            @Override
            public String findDOIForReference(String reference) throws IOException {
                return "doi:1234";
            }

            @Override
            public String findCitationForDOI(String doi) throws IOException {
                return "my citation";
            }
        });
        StudyNode study = getNodeFactory().getOrCreateStudy("my title", "some source", ExternalIdUtil.toCitation("my contr", "some description", null));
        assertThat(study.getDOI(), is("doi:1234"));
        assertThat(study.getExternalId(), is("http://dx.doi.org/1234"));
        assertThat(study.getCitation(), is("my citation"));

        getNodeFactory().setDoiResolver(new DOIResolver() {
            @Override
            public String findDOIForReference(String reference) throws IOException {
                throw new IOException("kaboom!");
            }

            @Override
            public String findCitationForDOI(String doi) throws IOException {
                throw new IOException("kaboom!");
            }
        });
        study = getNodeFactory().getOrCreateStudy("my other title", "some source", ExternalIdUtil.toCitation("my contr", "some description", null));
        assertThat(study.getDOI(), nullValue());
        assertThat(study.getExternalId(), nullValue());
        assertThat(study.getCitation(), is("my contr. some description"));


    }

    @Test
    public void createStudy() throws NodeFactoryException {
        StudyNode study = getNodeFactory().getOrCreateStudy2("myTitle", "mySource", "doi:myDoi");
        assertThat(study.getDOI(), is("doi:myDoi"));
        assertThat(study.getExternalId(), is("http://dx.doi.org/myDoi"));
    }

    @Test
    public void specimenWithNoName() throws NodeFactoryException {
        Specimen specimen = getNodeFactory().createSpecimen(getNodeFactory().createStudy("bla"), null, "bla:123");
        assertThat(NodeUtil.getClassifications(specimen).iterator().hasNext(), is(false));
    }

    @Test
    public void specimenWithLifeStageInName() throws NodeFactoryException {
        initTaxonService();
        Specimen specimen = getNodeFactory().createSpecimen(getNodeFactory().createStudy("bla"), "mickey eggs scales");
        assertThat(specimen.getLifeStage().getName(), is("egg"));
        assertThat(specimen.getLifeStage().getId(), is("UBERON:0007379"));
        assertThat(specimen.getBodyPart().getName(), is("scale"));
        assertThat(specimen.getBodyPart().getId(), is("UBERON:0002542"));
    }

    @Test
    public void specimenWithBasisOfRecord() throws NodeFactoryException {
        initTaxonService();
        Specimen specimen = getNodeFactory().createSpecimen(getNodeFactory().createStudy("bla"), "mickey mouse");
        specimen.setBasisOfRecord(getNodeFactory().getOrCreateBasisOfRecord("something:123", "theBasis"));
        assertThat(specimen.getBasisOfRecord().getName(), is("theBasis"));
        assertThat(specimen.getBasisOfRecord().getId(), is("TEST:theBasis"));
    }

    protected void initTaxonService() {
        CorrectionService correctionService = new CorrectionService() {
            @Override
            public String correct(String taxonName) {
                return "mickey corrected";
            }
        };
        this.taxonIndex = new TaxonIndexImpl(new PassThroughEnricher(),
                correctionService, getGraphDb()
        );
    }


    @Test
    public void createEcoRegion() throws NodeFactoryException {
        Location locationA = getNodeFactory().getOrCreateLocation(37.689254, -122.295799, null);
        // ensure that no duplicate node are created ...
        getNodeFactory().getOrCreateLocation(37.689255, -122.295798, null);
        assertEcoRegions(locationA);
        getNodeFactory().enrichLocationWithEcoRegions(locationA);
        assertEcoRegions(locationA);

        // check that multiple locations are associated to single eco region
        Location locationB = getNodeFactory().getOrCreateLocation(37.689255, -122.295799, null);
        assertEcoRegions(locationB);

        IndexHits<Node> hits = getNodeFactory().findCloseMatchesForEcoregion("some elo egion");
        assertThat(hits.size(), is(1));
        assertThat((String) hits.iterator().next().getProperty(PropertyAndValueDictionary.NAME), is("some eco region"));

        hits = getNodeFactory().findCloseMatchesForEcoregion("mickey mouse goes shopping");
        assertThat(hits.size(), is(0));
        hits = getNodeFactory().findCloseMatchesForEcoregionPath("mickey mouse goes shopping");
        assertThat(hits.size(), is(0));

        hits = getNodeFactory().findCloseMatchesForEcoregionPath("path");
        assertThat(hits.size(), is(1));
        hits = getNodeFactory().findCloseMatchesForEcoregionPath("some");
        assertThat(hits.size(), is(1));

        hits = getNodeFactory().suggestEcoregionByName("some eco region");
        assertThat(hits.size(), is(1));
        hits = getNodeFactory().suggestEcoregionByName("path");
        assertThat(hits.size(), is(1));

    }

    private void assertEcoRegions(Location location) {
        Iterable<Relationship> relationships = ((NodeBacked)location).getUnderlyingNode().getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(RelTypes.IN_ECOREGION));
        int count = 0;
        for (Relationship relationship : relationships) {
            Node associatedEcoRegion = relationship.getEndNode();
            assertThat(associatedEcoRegion.getProperty("name"), is("some eco region"));
            count++;
        }
        assertThat(count, is(1));
    }


}
