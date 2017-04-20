package org.eol.globi.data;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.LogContext;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.DatasetImpl;
import org.eol.globi.service.GitHubUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class StudyImporterForGoMexSI2IT extends GraphDBTestCase {

    private static final Log LOG = LogFactory.getLog(StudyImporterForGoMexSI2IT.class);
    public static final String WKT_FOOTPRINT2 = "POLYGON((-92.6729107838999 29.3941413332999,-92.5604838626999 29.2066775354,-92.7326173694 29.1150784684999,-92.9638307704999 29.1171045174,-93.3169089704999 29.3616452463,-93.4007435505999 29.5222620776999,-93.3169089704999 29.6243402981,-93.1045280342 29.6340566488,-92.6729107838999 29.3941413332999))";

    @Test
    public void createAndPopulateStudyGitHubMostRecent() throws StudyImporterException, IOException, URISyntaxException {
        importWithCommit(GitHubUtil.getBaseUrlLastCommit("gomexsi/interaction-data"));
        assertThatSomeDataIsImported(nodeFactory, taxonIndex);
    }

    @Test
    public void createAndPopulateStudyWithStatic() throws StudyImporterException, IOException, URISyntaxException {
        importWithCommit(new File(getClass().getResource("gomexsi/Prey.csv").toURI()).getParentFile().toURI().toString());
        assertThatSomeDataIsImported(nodeFactory, taxonIndex);
    }

    protected StudyImporterForGoMexSI2 importWithCommit(String baseUrlLastCommit) throws StudyImporterException, IOException {
        StudyImporterForGoMexSI2 importer = new StudyImporterForGoMexSI2(new ParserFactoryLocal(), nodeFactory);
        final List<String> msgs = new ArrayList<String>();
        importer.setLogger(new ImportLogger() {
            @Override
            public void warn(LogContext study, String message) {
                LOG.warn(message);
                msgs.add("warn: " + message);
            }

            @Override
            public void info(LogContext study, String message) {
                LOG.info(message);
                msgs.add("info: " + message);
            }

            @Override
            public void severe(LogContext study, String message) {
                LOG.error(message);
                msgs.add("severe: " + message);
            }
        });

        JsonNode config = new ObjectMapper().readTree("{ \n" +
                "  \"citation\": \"testing source citation\",\n" +
                "  \"format\": \"gomexsi\"\n" +
                "}");
        DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create(baseUrlLastCommit));
        importer.setDataset(dataset);
        dataset.setConfig(config);

        importStudy(importer);
        return importer;
    }

    private static void assertThatSomeDataIsImported(NodeFactory nodeFactory, TaxonIndex taxonIndex) throws StudyImporterException, NodeFactoryException {
        Study study = nodeFactory.findStudy("Divita et al 1983");

        assertSpecimenProperties(((NodeBacked)study).getUnderlyingNode().getGraphDatabase());

        assertNotNull(study);
        assertThat(study.getTitle(), is("Divita et al 1983"));
        assertThat(study.getExternalId(), is(ExternalIdUtil.urlForExternalId("GAME:2689")));
        assertThat(study.getCitation(), is("Regina Divita, Mischelle Creel, Peter Sheridan. 1983. Foods of coastal fishes during brown shrimp Penaeus aztecus, migration from Texas estuaries (June - July 1981)."));
        assertNotNull(nodeFactory.findStudy("Beaumariage 1973"));
        assertNotNull(nodeFactory.findStudy("Baughman, 1943"));

        assertNotNull(taxonIndex.findTaxonByName("Chloroscombrus chrysurus"));
        assertNotNull(taxonIndex.findTaxonByName("Micropogonias undulatus"));

        assertNotNull(taxonIndex.findTaxonByName("Amphipoda"));
        assertNotNull(taxonIndex.findTaxonByName("Crustacea"));

        Taxon taxon = taxonIndex.findTaxonByName("Scomberomorus cavalla");
        List<String> preyList = new ArrayList<String>();
        final List<String> titles = new ArrayList<String>();
        Iterable<Relationship> classifiedAsRels = ((NodeBacked)taxon).getUnderlyingNode().getRelationships(Direction.INCOMING, NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS));
        int count = 0;
        for (Relationship classifiedAsRel : classifiedAsRels) {
            Node predatorSpecimen = classifiedAsRel.getStartNode();
            Specimen predator = new SpecimenNode(predatorSpecimen);
            Iterable<Relationship> stomachContents = NodeUtil.getStomachContents(predator);
            for (Relationship prey : stomachContents) {
                Relationship singleRelationship = prey.getEndNode().getSingleRelationship(NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS), Direction.OUTGOING);
                preyList.add((String) singleRelationship.getEndNode().getProperty("name"));
            }
            count++;

            Relationship collectedBy = predatorSpecimen.getSingleRelationship(NodeUtil.asNeo4j(RelTypes.COLLECTED), Direction.INCOMING);
            assertThat(collectedBy, is(notNullValue()));
            String title = (String) collectedBy.getStartNode().getProperty("title");
            titles.add(title);
        }

        assertThat(count > 7, is(true));
        assertThat(preyList, hasItem("Organic matter"));
        assertThat(preyList, hasItem("Triglidae"));
        assertThat(preyList, hasItem("Sparidae"));

        assertThat(titles, hasItem("Beaumariage 1973"));
        assertThat(titles, hasItem("Blanton et al 1972"));

        assertNotNull(taxon);

        final String footprintWKT = WKT_FOOTPRINT2;
        LocationImpl expectedLocation = new LocationImpl(29.346953, -92.980614, -13.641, footprintWKT);
        expectedLocation.setLocality("Louisiana inner continental shelf");
        Location location = nodeFactory.findLocation(expectedLocation);
        assertThat(location, is(notNullValue()));
        assertThat(location.getFootprintWKT(), is(footprintWKT));
        assertThat(location.getLocality(), is("Louisiana inner continental shelf"));
        assertNotNull(location);
        List<Environment> environments = location.getEnvironments();
        assertThat(environments.size(), not(is(0)));
        assertThat(environments.get(0).getExternalId(), is("http://cmecscatalog.org/classification/aquaticSetting/13"));
        assertThat(environments.get(0).getName(), is("Marine Nearshore Subtidal"));

        assertNotNull(nodeFactory.findStudy("GoMexSI"));
    }

    private static void assertSpecimenProperties(GraphDatabaseService service) {
        Index<Node> taxa = service.index().forNodes("taxons");
        boolean detectedAtLeastOneLifeState = false;
        boolean detectedAtLeastOnePhysiologicalState = false;
        boolean detectedAtLeastOnePreyBodyPart = false;
        boolean detectedAtLeastOneBodyLength = false;
        boolean detectedAtLeastOneLocation = false;
        boolean detectedAtLeastOneFrequencyOfOccurrence = false;
        boolean detectedAtLeastOneTotalNumberConsumed = false;
        boolean detectedAtLeastOneTotalVolume = false;
        boolean detectedAtLeastOneGoMexSIProperty = false;

        assertThat(taxa, is(notNullValue()));

        for (Node taxonNode : taxa.query("name", "*")) {
            Iterable<Relationship> classifiedAs = taxonNode.getRelationships(Direction.INCOMING, NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS));
            for (Relationship classifiedA : classifiedAs) {
                Node specimenNode = classifiedA.getStartNode();
                detectedAtLeastOneLifeState |= specimenNode.hasProperty(SpecimenConstant.LIFE_STAGE_LABEL);
                detectedAtLeastOnePhysiologicalState |= specimenNode.hasProperty(SpecimenConstant.PHYSIOLOGICAL_STATE_LABEL);
                detectedAtLeastOnePreyBodyPart |= specimenNode.hasProperty(SpecimenConstant.BODY_PART_LABEL);
                detectedAtLeastOneBodyLength |= specimenNode.hasProperty(SpecimenConstant.LENGTH_IN_MM);
                detectedAtLeastOneFrequencyOfOccurrence |= specimenNode.hasProperty(SpecimenConstant.FREQUENCY_OF_OCCURRENCE);
                detectedAtLeastOneTotalNumberConsumed |= specimenNode.hasProperty(SpecimenConstant.TOTAL_COUNT);
                detectedAtLeastOneTotalVolume |= specimenNode.hasProperty(SpecimenConstant.TOTAL_VOLUME_IN_ML);
                detectedAtLeastOneGoMexSIProperty |= specimenNode.hasProperty(StudyImporterForGoMexSI2.GOMEXSI_NAMESPACE + "PRED_DATABASE_NAME");
                detectedAtLeastOneGoMexSIProperty |= specimenNode.hasProperty(StudyImporterForGoMexSI2.GOMEXSI_NAMESPACE + "PREY_DATABASE_NAME");
                if (specimenNode.hasRelationship(Direction.INCOMING, NodeUtil.asNeo4j(RelTypes.COLLECTED))) {
                    detectedAtLeastOneLocation = true;
                }
            }
        }

        assertThat(detectedAtLeastOneLifeState, is(true));
        assertThat(detectedAtLeastOnePhysiologicalState, is(true));
        assertThat(detectedAtLeastOnePreyBodyPart, is(true));
        assertThat(detectedAtLeastOneLocation, is(true));
        assertThat(detectedAtLeastOneBodyLength, is(true));
        assertThat(detectedAtLeastOneFrequencyOfOccurrence, is(true));
        assertThat(detectedAtLeastOneTotalNumberConsumed, is(true));
        assertThat(detectedAtLeastOneTotalVolume, is(true));
        assertThat(detectedAtLeastOneGoMexSIProperty, is(true));
    }


}