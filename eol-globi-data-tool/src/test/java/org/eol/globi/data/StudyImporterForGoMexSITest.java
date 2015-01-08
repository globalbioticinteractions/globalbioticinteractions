package org.eol.globi.data;


import org.eol.globi.domain.Environment;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.GitHubUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class StudyImporterForGoMexSITest extends GraphDBTestCase {

    @Test
    public void createAndPopulateStudy() throws StudyImporterException, NodeFactoryException, IOException, URISyntaxException {
        StudyImporterForGoMexSI importer = new StudyImporterForGoMexSI(new ParserFactoryImpl(), nodeFactory);
        assertThatSomeDataIsImported(importer, nodeFactory);
    }

    @Test
    public void createAndPopulateStudyGitHub() throws StudyImporterException, NodeFactoryException, IOException, URISyntaxException {
        StudyImporterForGoMexSI importer = new StudyImporterForGoMexSI(new ParserFactoryImpl(), nodeFactory);
        importer.setBaseUrl(GitHubUtil.getBaseUrlLastCommit("gomexsi/interaction-data"));
        importer.setSourceCitation("testing source citation");
        assertThatSomeDataIsImported(importer, nodeFactory);
    }

    private static void assertThatSomeDataIsImported(StudyImporterForGoMexSI importer, NodeFactory nodeFactory) throws StudyImporterException, NodeFactoryException {
        importer.importStudy();

        Study study = nodeFactory.findStudy("Divita et al 1983");

        assertSpecimenProperties(study.getUnderlyingNode().getGraphDatabase());

        assertNotNull(study);
        assertThat(study.getTitle(), is("Divita et al 1983"));
        assertThat(study.getExternalId(), is(ExternalIdUtil.infoURLForExternalId("GAME:2689")));
        assertThat(study.getContributor(), is("Regina Divita, Mischelle Creel, Peter Sheridan"));
        assertThat(study.getPublicationYear(), is("1983"));
        assertThat(study.getInstitution(), is(""));
        assertThat(study.getDescription(), is("Foods of coastal fishes during brown shrimp Penaeus aztecus, migration from Texas estuaries (June - July 1981). "));
        assertNotNull(nodeFactory.findStudy("Beaumariage 1973"));

        assertNotNull(nodeFactory.findTaxonByName("Chloroscombrus chrysurus"));
        assertNotNull(nodeFactory.findTaxonByName("Micropogonias undulatus"));

        assertNotNull(nodeFactory.findTaxonByName("Amphipoda"));
        assertNotNull(nodeFactory.findTaxonByName("Crustacea"));

        TaxonNode taxon = nodeFactory.findTaxonByName("Scomberomorus cavalla");
        List<String> preyList = new ArrayList<String>();
        final List<String> titles = new ArrayList<String>();
        Iterable<Relationship> classifiedAsRels = taxon.getUnderlyingNode().getRelationships(Direction.INCOMING, RelTypes.CLASSIFIED_AS);
        int count = 0;
        for (Relationship classifiedAsRel : classifiedAsRels) {
            Node predatorSpecimen = classifiedAsRel.getStartNode();
            Specimen predator = new Specimen(predatorSpecimen);
            Iterable<Relationship> stomachContents = predator.getStomachContents();
            for (Relationship prey : stomachContents) {
                Relationship singleRelationship = prey.getEndNode().getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING);
                preyList.add((String) singleRelationship.getEndNode().getProperty("name"));
            }
            count++;

            Relationship collectedBy = predatorSpecimen.getSingleRelationship(RelTypes.COLLECTED, Direction.INCOMING);
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

        Location location = nodeFactory.findLocation(29.346953, -92.980614, -13.641);
        assertNotNull(location);
        List<Environment> environments = location.getEnvironments();
        assertThat(environments.size(), not(is(0)));
        assertThat(environments.get(0).getExternalId(), is("CMECS:AQUATIC_SETTING:13"));
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

        assertThat(taxa, is(notNullValue()));

        for (Node taxonNode : taxa.query("name", "*")) {
            Iterable<Relationship> classifiedAs = taxonNode.getRelationships(Direction.INCOMING, RelTypes.CLASSIFIED_AS);
            for (Relationship classifiedA : classifiedAs) {
                Node specimenNode = classifiedA.getStartNode();
                detectedAtLeastOneLifeState |= specimenNode.hasProperty(Specimen.LIFE_STAGE_LABEL);
                detectedAtLeastOnePhysiologicalState |= specimenNode.hasProperty(Specimen.PHYSIOLOGICAL_STATE_LABEL);
                detectedAtLeastOnePreyBodyPart |= specimenNode.hasProperty(Specimen.BODY_PART_LABEL);
                detectedAtLeastOneBodyLength |= specimenNode.hasProperty(Specimen.LENGTH_IN_MM);
                detectedAtLeastOneFrequencyOfOccurrence |= specimenNode.hasProperty(Specimen.FREQUENCY_OF_OCCURRENCE);
                detectedAtLeastOneTotalNumberConsumed |= specimenNode.hasProperty(Specimen.TOTAL_COUNT);
                detectedAtLeastOneTotalVolume |= specimenNode.hasProperty(Specimen.TOTAL_VOLUME_IN_ML);
                if (specimenNode.hasRelationship(Direction.INCOMING, RelTypes.COLLECTED)) {
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
    }


}