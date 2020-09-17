package org.eol.globi.data;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Term;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class DatasetImporterForSeltmannIT extends GraphDBTestCase {

    private final static String ARCHIVE_URI_PREFIX = "http://amnh.begoniasociety.org/dwc/";

    @Test
    public void importBees() throws StudyImporterException, IOException {
        importArchive("AEC-DBCNet_DwC-A20151028.zip");
    }

    @Test
    public void importPlantBugs() throws StudyImporterException, IOException {
        importArchive("AEC-NA_PlantBugPBI_DwC-A20160308.zip");
    }

    @Test
    public void importTriTrophic() throws StudyImporterException, IOException {
        importArchive("AEC-TTD-TCN_DwC-A20160308.zip");
    }

    private void importArchive(String archiveName) throws StudyImporterException {
        DatasetImporterForSeltmann importer = new DatasetImporterForSeltmann(null, nodeFactory);
        URI archiveURI = URI.create(ARCHIVE_URI_PREFIX + archiveName);
        DatasetImpl dataset = new DatasetImpl("some/namespace", archiveURI, inStream -> inStream);
        createAndSetConfig(archiveURI, dataset);
        importer.setDataset(dataset);

        importStudy(importer);

        List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        for (StudyNode allStudy : allStudies) {
            assertThat(allStudy.getSource(), startsWith("Digital Bee Collections Network, 2014 (and updates). Version: 2015-03-18. National Science Foundation grant DBI 0956388"));
            assertThat(allStudy.getCitation(), is("Digital Bee Collections Network, 2014 (and updates). Version: 2015-03-18. National Science Foundation grant DBI 0956388"));

            NodeUtil.handleCollectedRelationships(new NodeTypeDirection(allStudy.getUnderlyingNode()), new NodeUtil.RelationshipListener() {
                @Override
                public void on(Relationship relationship) {
                    Specimen spec = new SpecimenNode(relationship.getEndNode());
                    Term basisOfRecord = spec.getBasisOfRecord();
                    assertThat(basisOfRecord.getId(), either(is("TEST:PreservedSpecimen")).or(is("TEST:LabelObservation")));
                    assertThat(basisOfRecord.getName(), either(is("PreservedSpecimen")).or(is("LabelObservation")));
                }
            });

        }

        assertThat(taxonIndex.findTaxonByName("Megandrena mentzeliae"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Mentzelia tricuspis"), is(notNullValue()));
    }

    private void createAndSetConfig(URI archiveURI, DatasetImpl dataset) {
        ObjectMapper objMapper = new ObjectMapper();
        ObjectNode objectNode = objMapper.createObjectNode();
        ObjectNode objectNode1 = objMapper.createObjectNode();
        objectNode1.put("archive", archiveURI.toString());
        objectNode.put("resources", objectNode1);
        dataset.setConfig(objectNode);
    }
}
