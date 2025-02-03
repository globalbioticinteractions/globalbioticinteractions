package org.eol.globi.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.DatasetNode;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.EnvironmentNode;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Interaction;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.LocationNode;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.taxon.NonResolvingTaxonIndexNeo4j2;
import org.eol.globi.util.DateUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.NodeUtil;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetConstant;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.globalbioticinteractions.doi.DOI;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public abstract class NodeFactoryNeo4jTest extends GraphDBNeo4jTestCase {

    private static final DOI SOME_DOI = new DOI("some", "doi");

    @Test
    public void toCitation() {
        assertThat(ExternalIdUtil.toCitation(null, null, null), is(""));
    }

    @Test
    public void createInteraction() throws NodeFactoryException {
        StudyNode study = getNodeFactory().createStudy(new StudyImpl("bla", null, null));
        SpecimenNode specimen = getNodeFactory().createSpecimen(study, new TaxonImpl("Donalda duckus", null));
        SpecimenNode specimen1 = getNodeFactory().createSpecimen(study, new TaxonImpl("Mickeya mouseus", null));
        specimen.interactsWith(specimen1, InteractType.ATE);
        assertInteraction(specimen, specimen1, RelTypes.COLLECTED, "Donalda duckus");
    }

    @Test
    public void createPassiveInteraction() throws NodeFactoryException {
        StudyNode study = getNodeFactory().createStudy(new StudyImpl("bla", null, null));
        SpecimenNode specimen = getNodeFactory().createSpecimen(study, new TaxonImpl("Donalda duckus", null));
        SpecimenNode specimen1 = getNodeFactory().createSpecimen(study, new TaxonImpl("Mickeya mouseus", null));
        specimen1.interactsWith(specimen, InteractType.EATEN_BY);
        assertInteraction(specimen, specimen1, RelTypes.COLLECTED, "Donalda duckus");
    }

    private void assertInteraction(SpecimenNode specimen, SpecimenNode specimen1, RelTypes studyRelationType, String sourceTaxonName) {
        assertStudyType(studyRelationType, specimen);
        assertStudyType(studyRelationType, specimen1);

        final Iterator<Relationship> relIter = specimen.getUnderlyingNode().getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(InteractType.ATE)).iterator();
        assertThat(relIter.hasNext(), is(true));
        final Relationship rel = relIter.next();
        assertThat(rel.getProperty("iri").toString(), is("http://purl.obolibrary.org/obo/RO_0002470"));
        assertThat(rel.getProperty("label").toString(), is("eats"));
        assertFalse(rel.hasProperty("inverted"));

        final Iterator<Relationship> relIterClassification = specimen.getUnderlyingNode().getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(RelTypes.ORIGINALLY_DESCRIBED_AS)).iterator();
        assertThat(relIterClassification.hasNext(), is(true));
        final Node taxonNode = relIterClassification.next().getEndNode();
        TaxonNode actualSourceTaxonNode = new TaxonNode(taxonNode);
        assertThat(actualSourceTaxonNode.getName(), is(sourceTaxonName));

        Iterable<Relationship> relationships = specimen1.getUnderlyingNode().getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(InteractType.EATEN_BY));
        Iterator<Relationship> iterator = relationships.iterator();
        assertThat(iterator.hasNext(), is(true));
        Relationship relInverted = iterator.next();
        assertThat(relInverted.getProperty("iri").toString(), is("http://purl.obolibrary.org/obo/RO_0002471"));
        assertThat(relInverted.getProperty("label").toString(), is("eatenBy"));
    }

    private void assertStudyType(RelTypes studyRelationType, SpecimenNode specimen2) {
        boolean hasRelationship = specimen2.getUnderlyingNode().hasRelationship(Direction.INCOMING, NodeUtil.asNeo4j(studyRelationType));
        assertThat(hasRelationship, is(true));
    }

    @Test
    public void createRefutingInteraction() throws NodeFactoryException {
        StudyNode study = getNodeFactory().createStudy(new StudyImpl("bla", null, null));
        SpecimenNode specimen = getNodeFactory().createSpecimen(study, new TaxonImpl("Donalda duckus", null), RelTypes.REFUTES);
        SpecimenNode specimen1 = getNodeFactory().createSpecimen(study, new TaxonImpl("Mickeya mouseus", null), RelTypes.REFUTES);
        specimen.interactsWith(specimen1, InteractType.ATE);
        assertInteraction(specimen, specimen1, RelTypes.REFUTES, "Donalda duckus");
    }


    @Test
    public void createFindLocation() throws NodeFactoryException {
        LocationImpl location2 = new LocationImpl(1.2d, 1.4d, -1.0d, null);
        location2.setLocality("some locale");
        location2.setLocalityId("some:id");
        Location location = getNodeFactory().getOrCreateLocation(location2);
        getNodeFactory().getOrCreateLocation(new LocationImpl(2.2d, 1.4d, -1.0d, null));
        getNodeFactory().getOrCreateLocation(new LocationImpl(1.2d, 2.4d, -1.0d, null));
        Location locationNoDepth = getNodeFactory().getOrCreateLocation(new LocationImpl(1.5d, 2.8d, null, null));
        assertNotNull(location);
        Location location1 = getNodeFactory().findLocation(location2);
        assertNotNull(location1);
        Location foundLocationNoDepth = getNodeFactory().findLocation(new LocationImpl(locationNoDepth.getLatitude(), locationNoDepth.getLongitude(), null, null));
        assertNotNull(foundLocationNoDepth);
    }

    @Test
    public void createFindLocationWith() throws NodeFactoryException {
        Location location = getNodeFactory().getOrCreateLocation(new LocationImpl(1.2d, 1.4d, -1.0d, null));
        getNodeFactory().getOrCreateLocation(new LocationImpl(2.2d, 1.4d, -1.0d, null));
        getNodeFactory().getOrCreateLocation(new LocationImpl(1.2d, 2.4d, -1.0d, null));
        Location locationNoDepth = getNodeFactory().getOrCreateLocation(new LocationImpl(1.5d, 2.8d, null, null));
        assertNotNull(location);
        Location location1 = getNodeFactory().findLocation(new LocationImpl(location.getLatitude(), location.getLongitude(), location.getAltitude(), null));
        assertNotNull(location1);
        Location foundLocationNoDepth = getNodeFactory().findLocation(new LocationImpl(locationNoDepth.getLatitude(), locationNoDepth.getLongitude(), null, null));
        assertNotNull(foundLocationNoDepth);
    }

    @Test
    public void createFindLocationWKT() throws NodeFactoryException {
        Location location = getNodeFactory().getOrCreateLocation(new LocationImpl(2.0d, 1.0d, -1.0d, null));
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
        getNodeFactory().getOrCreateLocation(new LocationImpl(91.3d, -104.0d, -1.0d, null));
        getNodeFactory().getOrCreateLocation(new LocationImpl(-100.3d, 104d, -1.0d, null));
        getNodeFactory().getOrCreateLocation(new LocationImpl(-10.3d, -200.0d, -1.0d, null));
        getNodeFactory().getOrCreateLocation(new LocationImpl(-20.0d, 300.0d, -1.0d, null));
    }


    @Test
    public void createAndFindEnvironment() throws NodeFactoryException {
        getNodeFactory().setEnvoLookupService(new TermLookupService() {
            @Override
            public List<Term> lookupTermByName(String name) throws TermLookupServiceException {
                ArrayList<Term> terms = new ArrayList<>();
                terms.add(new TermImpl("NS:" + name, StringUtils.replace(name, " ", "_")));
                return terms;
            }
        });
        Location location = getNodeFactory().getOrCreateLocation(new LocationImpl(0.0, 1.0, 2.0, null));
        List<Environment> first = getNodeFactory().getOrCreateEnvironments(location, "BLA:123", "this and that");
        location = getNodeFactory().getOrCreateLocation(new LocationImpl(0.0, 1.0, 2.0, null));
        List<Environment> second = getNodeFactory().getOrCreateEnvironments(location, "BLA:123", "this and that");
        assertThat(first.size(), is(second.size()));
        assertThat(((NodeBacked) first.get(0)).getNodeID(), is(not(((NodeBacked) second.get(0)).getNodeID())));

        List<Environment> environments = location.getEnvironments();
        assertThat(environments.size(), is(2));
        Environment environment = environments.get(0);
        assertThat(environment.getName(), is("this_and_that"));
        assertThat(environment.getExternalId(), is("NS:this and that"));

        Location anotherLocation = getNodeFactory().getOrCreateLocation(new LocationImpl(48.2, 123.1, null, null));
        LocationNode anotherLocationNode = (LocationNode) anotherLocation;
        assertThat(anotherLocationNode.getEnvironments().size(), is(0));
        anotherLocationNode.addEnvironment((EnvironmentNode) environment);
        assertThat(anotherLocationNode.getEnvironments().size(), is(1));

        // don't add environment that has already been associated
        anotherLocationNode.addEnvironment(environment);
        assertThat(anotherLocationNode.getEnvironments().size(), is(1));

        getNodeFactory().getOrCreateEnvironments(anotherLocation, "BLA:124", "that");
        assertThat(anotherLocationNode.getEnvironments().size(), is(2));
    }

    @Test
    public void createStudyWithExternalIdNoDOI() throws NodeFactoryException, IOException {
        StudyImpl study1 = new StudyImpl("my title", null, "some citation");
        study1.setExternalId("some:id");
        StudyNode study = getNodeFactory().getOrCreateStudy(study1);
        assertThat(study.getExternalId(), is("some:id"));
        assertThat(study.getDOI(), is(nullValue()));

    }

    @Test
    public void getOrCreateDataset() throws NodeFactoryException, IOException {
        assertDataset(DatasetConstant.CITATION);
    }

    @Test
    public void getOrCreateDatasetDWCABib() throws NodeFactoryException, IOException {
        assertDataset(PropertyAndValueDictionary.DCTERMS_BIBLIOGRAPHIC_CITATION);
    }

    protected void assertDataset(String citationKey) throws NodeFactoryException {
        DatasetImpl dataset = new DatasetWithResourceMapping("some/namespace", URI.create("some:uri"), getResourceService());
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put(DatasetConstant.SHOULD_RESOLVE_REFERENCES, false);
        objectNode.put(citationKey, "some citation");
        dataset.setConfig(objectNode);

        Dataset origDataset = getNodeFactory().getOrCreateDataset(dataset);


        assertThat(origDataset, is(notNullValue()));
        assertThat(origDataset.getArchiveURI().toString(), is("some:uri"));
        assertThat(origDataset.getOrDefault(DatasetConstant.SHOULD_RESOLVE_REFERENCES, "true"), is("false"));
        assertThat(origDataset.getOrDefault(DatasetConstant.CITATION, "no citation"), is("some citation"));
        assertThat(origDataset.getCitation(), is("some citation"));
        assertThat(origDataset.getOrDefault(DatasetConstant.LAST_SEEN_AT, "1"), is(not("1")));

        Dataset datasetAnother = getNodeFactory().getOrCreateDataset(dataset);
        assertThat(((DatasetNode) datasetAnother).getNodeID(), is(((DatasetNode) origDataset).getNodeID()));
    }

    @Test
    public void addDatasetToStudy() throws NodeFactoryException, IOException {
        StudyImpl study1 = new StudyImpl("my title", SOME_DOI, "some citation");
        DatasetImpl dataset = new DatasetWithResourceMapping("some/namespace", URI.create("some:uri"), getResourceService());
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put(DatasetConstant.SHOULD_RESOLVE_REFERENCES, false);
        dataset.setConfig(objectNode);
        study1.setOriginatingDataset(dataset);

        StudyNode study = getNodeFactory().getOrCreateStudy(study1);

        Dataset origDataset = study.getOriginatingDataset();

        assertThat(origDataset, is(notNullValue()));
        assertThat(origDataset.getArchiveURI().toString(), is("some:uri"));
        assertThat(origDataset.getOrDefault(DatasetConstant.SHOULD_RESOLVE_REFERENCES, "true"), is("false"));

        String expectedConfig = new ObjectMapper().writeValueAsString(objectNode);
        assertThat(new ObjectMapper().writeValueAsString(origDataset.getConfig()), is(expectedConfig));
        Node datasetNode = NodeUtil.getDataSetForStudy(study);
        assertThat(datasetNode.getProperty(DatasetConstant.NAMESPACE), is("some/namespace"));
        assertThat(datasetNode.getProperty("archiveURI"), is("some:uri"));
        assertThat(datasetNode.getProperty(DatasetConstant.SHOULD_RESOLVE_REFERENCES), is("false"));

        StudyImpl otherStudy = new StudyImpl("my other title", SOME_DOI, "some citation");
        otherStudy.setOriginatingDataset(dataset);
        StudyNode studySameDataset = getNodeFactory().getOrCreateStudy(otherStudy);
        Node datasetNodeOther = NodeUtil.getDataSetForStudy(studySameDataset);

        assertThat(datasetNode.getId(), is(datasetNodeOther.getId()));
    }

    @Test
    public void sameStudyDifferentDataset() throws NodeFactoryException, IOException {
        StudyImpl study1 = new StudyImpl("my title", SOME_DOI, "some citation");
        study1.setOriginatingDataset(datasetWithNamespace("some/namespace"));

        StudyNode study = getNodeFactory().getOrCreateStudy(study1);
        Node datasetNode = NodeUtil.getDataSetForStudy(study);

        study1.setOriginatingDataset(datasetWithNamespace("some/othernamespace"));
        StudyNode studyDifferentDataset = getNodeFactory().getOrCreateStudy(study1);
        Node datasetNodeOther = NodeUtil.getDataSetForStudy(studyDifferentDataset);

        assertThat(datasetNode.getId(), is(not(datasetNodeOther.getId())));

        getNodeFactory().getOrCreateStudy(study1);
    }

    @Test
    public void sameStudyNoDataset() throws NodeFactoryException, IOException {
        StudyImpl study1 = new StudyImpl("my title", SOME_DOI, "some citation");

        StudyNode studyNoDataset1 = getNodeFactory().getOrCreateStudy(study1);
        StudyNode studyNoDataset2 = getNodeFactory().getOrCreateStudy(study1);

        assertThat(studyNoDataset1.getUnderlyingNode().getId(),
                is(studyNoDataset2.getUnderlyingNode().getId()));
    }

    private DatasetImpl datasetWithNamespace(String namespace) {
        DatasetImpl dataset = new DatasetWithResourceMapping(namespace, URI.create("some:uri"), getResourceService());
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put(DatasetConstant.SHOULD_RESOLVE_REFERENCES, false);
        dataset.setConfig(objectNode);
        return dataset;
    }

    @Test
    public void addDatasetToStudyNulls() throws NodeFactoryException {
        StudyImpl study1 = new StudyImpl("my title", SOME_DOI, "some citation");
        DatasetImpl dataset = new DatasetWithResourceMapping(null, null, getResourceService());
        study1.setOriginatingDataset(dataset);
        StudyNode study = getNodeFactory().getOrCreateStudy(study1);

        assertThat(NodeUtil.getDataSetForStudy(study), is(nullValue()));
    }

    @Test
    public void addDatasetToStudyNulls2() throws NodeFactoryException {
        StudyImpl study1 = new StudyImpl("my title", SOME_DOI, "some citation");
        DatasetImpl dataset = new DatasetWithResourceMapping("some/namespace", null, getResourceService());
        study1.setOriginatingDataset(dataset);
        StudyNode study = getNodeFactory().getOrCreateStudy(study1);

        Node datasetNode = NodeUtil.getDataSetForStudy(study);
        assertThat(datasetNode.getProperty(DatasetConstant.NAMESPACE), is("some/namespace"));
        assertThat(datasetNode.hasProperty(DatasetConstant.ARCHIVE_URI), is(false));
        assertThat(datasetNode.getProperty(DatasetConstant.SHOULD_RESOLVE_REFERENCES), is("true"));
    }


    @Test
    public void createStudy() throws NodeFactoryException {
        StudyNode study = getNodeFactory().getOrCreateStudy(new StudyImpl("myTitle", new DOI("myDoi", "123"), null));
        assertThat(study.getDOI().toString(), is("10.myDoi/123"));
        assertThat(study.getExternalId(), is("https://doi.org/10.myDoi/123"));
    }

    @Test
    public void createStudyWithDifferentExternalId() throws NodeFactoryException {
        StudyImpl study1 = new StudyImpl("myTitle", new DOI("myDoi", "123"), null);
        study1.setExternalId("foo:bar");
        StudyNode study1Created = getNodeFactory().getOrCreateStudy(study1);

        StudyImpl study2 = new StudyImpl("myTitle", new DOI("myDoi", "123"), null);
        study2.setExternalId("foo:baz");

        StudyNode study2Created = getNodeFactory().getOrCreateStudy(study2);

        assertThat(study1Created.getExternalId(),is("foo:bar"));
        assertThat(study2Created.getExternalId(),is("foo:baz"));
    }

    @Test
    public void createStudyWithDifferentExternalIdInSameNamespace() throws NodeFactoryException {
        StudyImpl study1 = new StudyImpl("myTitle", new DOI("myDoi", "123"), null);
        study1.setOriginatingDataset(new DatasetWithResourceMapping("name/space", URI.create("foo:bar"), getResourceService()));
        study1.setExternalId("foo:bar");
        StudyNode study1Created = getNodeFactory().getOrCreateStudy(study1);

        StudyImpl study2 = new StudyImpl("myTitle", new DOI("myDoi", "123"), null);
        study2.setOriginatingDataset(new DatasetWithResourceMapping("name/space", URI.create("foo:bar"), getResourceService()));
        study2.setExternalId("foo:baz");

        StudyNode study2Created = getNodeFactory().getOrCreateStudy(study2);

        assertThat(study1Created.getExternalId(),is("foo:bar"));
        assertThat(study2Created.getExternalId(),is("foo:baz"));
    }

    @Test
    public void createStudyWithSameExternalIdInDifferentNamespace() throws NodeFactoryException {
        StudyImpl study1 = new StudyImpl("myTitle", new DOI("myDoi", "123"), null);
        study1.setOriginatingDataset(new DatasetWithResourceMapping("name/space", URI.create("foo:bar"), getResourceService()));
        study1.setExternalId("foo:bar");
        StudyNode study1Created = getNodeFactory().getOrCreateStudy(study1);

        StudyImpl study2 = new StudyImpl("myTitle", new DOI("myDoi", "123"), null);
        study2.setOriginatingDataset(new DatasetWithResourceMapping("name/spacz", URI.create("foo:bar"), getResourceService()));
        study2.setExternalId("foo:bar");

        StudyNode study2Created = getNodeFactory().getOrCreateStudy(study2);

        assertThat(study1Created.getExternalId(),is("foo:bar"));
        assertThat(study1Created.getOriginatingDataset().getNamespace(),is("name/space"));
        assertThat(study2Created.getExternalId(),is("foo:bar"));
        assertThat(study2Created.getOriginatingDataset().getNamespace(),is("name/spacz"));
    }

    @Test
    public void createStudyWithDifferentExternalIdInDifferentNamespace() throws NodeFactoryException {
        StudyImpl study1 = new StudyImpl("myTitle", new DOI("myDoi", "123"), null);
        study1.setOriginatingDataset(new DatasetWithResourceMapping("name/space", URI.create("foo:bar"), getResourceService()));
        study1.setExternalId("foo:bar");
        StudyNode study1Created = getNodeFactory().getOrCreateStudy(study1);

        StudyImpl study2 = new StudyImpl("myTitle", new DOI("myDoi", "123"), null);
        study2.setOriginatingDataset(new DatasetWithResourceMapping("name/spacz", URI.create("foo:bar"), getResourceService()));
        study2.setExternalId("foo:baz");

        StudyNode study2Created = getNodeFactory().getOrCreateStudy(study2);

        assertThat(study1Created.getExternalId(),is("foo:bar"));
        assertThat(study1Created.getOriginatingDataset().getNamespace(),is("name/space"));
        assertThat(study2Created.getExternalId(),is("foo:baz"));
        assertThat(study2Created.getOriginatingDataset().getNamespace(),is("name/spacz"));
    }

    @Test
    public void createStudyWithMatchingExternalIdDifferentTitle() throws NodeFactoryException {
        StudyImpl study1 = new StudyImpl("myTitle", new DOI("myDoi", "123"), null);
        study1.setExternalId("foo:bar");
        StudyNode study1Created = getNodeFactory().getOrCreateStudy(study1);

        StudyImpl study2 = new StudyImpl("myTitlez", new DOI("myDoi", "123"), null);
        study2.setExternalId("foo:bar");

        StudyNode study2Created = getNodeFactory().getOrCreateStudy(study2);

        assertThat(study1Created.getExternalId(),is("foo:bar"));
        assertThat(study1Created.getTitle(),is("myTitle"));
        assertThat(study2Created.getExternalId(),is("foo:bar"));
        assertThat(study2Created.getTitle(),is("myTitle"));
    }

    @Test
    public void specimenWithNoName() throws NodeFactoryException {
        Specimen specimen = getNodeFactory().createSpecimen(getNodeFactory().createStudy(new StudyImpl("bla", null, null)), new TaxonImpl(null, "bla:123"));
        assertThat(NodeUtil.getClassifications(specimen).iterator().hasNext(), is(false));
    }

    @Test
    public void specimenWithLifeStageInName() throws NodeFactoryException {
        initTaxonService();
        Specimen specimen = getNodeFactory().createSpecimen(getNodeFactory().createStudy(new StudyImpl("bla", null, null)), new TaxonImpl("mickey eggs scales", null));
        assertThat(specimen.getLifeStage().getName(), is("egg"));
        assertThat(specimen.getLifeStage().getId(), is("UBERON:0007379"));
        assertThat(specimen.getBodyPart().getName(), is("scale"));
        assertThat(specimen.getBodyPart().getId(), is("UBERON:0002542"));
    }

    @Test
    public void specimenWithLifeStageInName2() throws NodeFactoryException {
        initTaxonService();
        Specimen specimen = getNodeFactory().createSpecimen(getNodeFactory().createStudy(new StudyImpl("bla", null, null)), new TaxonImpl("CALANUS SPP (NAUPLII)", null));
        assertThat(specimen.getLifeStage().getName(), is("nauplius stage"));
        assertThat(specimen.getLifeStage().getId(), is("UBERON:0014406"));
    }

    @Test
    public void specimenWithBasisOfRecord() throws NodeFactoryException {
        initTaxonService();
        Specimen specimen = getNodeFactory().createSpecimen(getNodeFactory().createStudy(new StudyImpl("bla", null, null)), new TaxonImpl("mickey mouse", null));
        specimen.setBasisOfRecord(getNodeFactory().getOrCreateBasisOfRecord("something:123", "theBasis"));
        assertThat(specimen.getBasisOfRecord().getName(), is("theBasis"));
        assertThat(specimen.getBasisOfRecord().getId(), is("TEST:theBasis"));
    }

    @Test
    public void interactionWithParticipants() throws NodeFactoryException {
        initTaxonService();
        StudyNode study = getNodeFactory().createStudy(new StudyImpl("bla", null, null));
        Interaction interaction = getNodeFactory().createInteraction(study);

        assertThat(interaction.getParticipants().size(), is(0));

        getNodeFactory().createSpecimen(interaction, new TaxonImpl("mickey mouse", null));
        getNodeFactory().createSpecimen(interaction, new TaxonImpl("donald duck", null));

        assertThat(interaction.getParticipants().size(), is(2));

    }

    @Test
    public void interactionWitEventTime() throws NodeFactoryException {
        initTaxonService();
        StudyNode study = getNodeFactory().createStudy(new StudyImpl("bla", null, null));

        SpecimenNode mickeyMouse = getNodeFactory().createSpecimen(study, new TaxonImpl("mickey mouse", null));
        SpecimenNode donaldDuck = getNodeFactory().createSpecimen(study, new TaxonImpl("donald duck", null));

        mickeyMouse.ate(donaldDuck);
        getNodeFactory().setUnixEpochProperty(mickeyMouse, DateUtil.parseDateUTC("2021-01-05").toDate());

        Iterable<Relationship> collectedRel = NodeFactoryNeo4j.getCollectedRel(mickeyMouse);
        boolean checked = false;
        for (Relationship relationship : collectedRel) {
            assertThat(relationship.getProperty(SpecimenConstant.EVENT_DATE), is("2021-01-05T00:00:00Z"));
            checked = true;
        }

        assertThat(checked, is(true));

    }

    private void initTaxonService() {
        this.taxonIndex = new NonResolvingTaxonIndexNeo4j2(
                getGraphDb()
        );
    }

}
