package org.eol.globi.data;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.DatasetLocal;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.junit.Assert;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetImporterForRSSLocalTest extends GraphDBTestCase {

    @Test
    public void importLocalArctosArchive() throws StudyImporterException, IOException {
        DatasetImporter importer = new StudyImporterTestFactory(nodeFactory)
                .instantiateImporter(DatasetImporterForRSS.class);
        DatasetLocal dataset = new DatasetLocal(inStream -> inStream);


        ObjectNode configNode = new ObjectMapper().createObjectNode();
        URL resource = getClass().getResource("/org/eol/globi/data/rss/arctos_issue_461.zip");
        assertNotNull(resource);
        String rssContent = rssContent(resource.toString());
        File directory = new File("target/tmp");
        FileUtils.forceMkdir(directory);
        File rss = File.createTempFile("rss", ".xml", directory);
        FileUtils.writeStringToFile(rss, rssContent, StandardCharsets.UTF_8);
        configNode.put("url", rss.toURI().toString());
        configNode.put("hasDependencies", true);
        dataset.setConfig(configNode);
        importer.setDataset(dataset);
        importStudy(importer);

        List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), greaterThan(0));
        StudyNode study = allStudies.get(0);

        TaxonNode taxonNode = (TaxonNode) taxonIndex.findTaxonByName("Anaxyrus cognatus");

        Assert.assertNotNull(taxonNode);

        NodeUtil.handleCollectedRelationships(new NodeTypeDirection(study.getUnderlyingNode()), new NodeUtil.RelationshipListener() {
            @Override
            public void on(Relationship relationship) {
                assertThat(relationship.getType().name(), is("COLLECTED"));

                Specimen source = new SpecimenNode(relationship.getEndNode());
                Relationship singleRelationship = ((SpecimenNode) source).getUnderlyingNode().getSingleRelationship(NodeUtil.asNeo4j(InteractType.INTERACTS_WITH), Direction.OUTGOING);

                Specimen target = new SpecimenNode(singleRelationship.getEndNode());
                assertNotNull(target);
                assertNotNull(source);
                Node sourceOrigTaxon = ((SpecimenNode) source)
                        .getUnderlyingNode()
                        .getSingleRelationship(NodeUtil.asNeo4j(RelTypes.ORIGINALLY_DESCRIBED_AS), Direction.OUTGOING)
                        .getEndNode();
                Node targetOrigTaxon = ((SpecimenNode) target)
                        .getUnderlyingNode()
                        .getSingleRelationship(NodeUtil.asNeo4j(RelTypes.ORIGINALLY_DESCRIBED_AS), Direction.OUTGOING)
                        .getEndNode();

                assertThat(new TaxonNode(sourceOrigTaxon).getName(), is("Anaxyrus cognatus"));
                assertThat(new TaxonNode(targetOrigTaxon).getName(), is("Anaxyrus cognatus"));


            }
        });


    }

    private String rssContent(String resourceURL) {
        return "<rss version=\"2.0\">\n" +
                "    <channel>\n" +
                "        <item ProjUID=\"2\">\n" +
                "            <title> testing</title>\n" +
                "            <type>DWCA</type>\n" +
                "            <recordType>DWCA</recordType>\n" +
                "            <link>\n" +
                "                " + resourceURL + "\n" +
                "            </link>\n" +
                "            <pubDate>Tue, 08 Mar 2016 13:49:08</pubDate>\n" +
                "        </item>\n" +
                "    </channel>\n" +
                "</rss>";
    }

}