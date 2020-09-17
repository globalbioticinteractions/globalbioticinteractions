package org.eol.globi.data;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Term;
import org.globalbioticinteractions.dataset.Dataset;
import org.eol.globi.service.DatasetLocal;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class DatasetImporterForSeltmannTest extends GraphDBTestCase {

    @Test
    public void importSome() throws StudyImporterException, IOException {
        DatasetImporterForSeltmann importer = new DatasetImporterForSeltmann(null, nodeFactory);
        Dataset dataset = new DatasetLocal(inStream -> inStream);
        JsonNode config = new ObjectMapper().readTree("{\"citation\": \"some citation\", \"resources\": {\"archive\": \"seltmann/testArchive.zip\"}}");
        dataset.setConfig(config);
        importer.setDataset(dataset);
        importStudy(importer);

        List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        for (StudyNode allStudy : allStudies) {
            assertThat(allStudy.getSource(), startsWith("Digital Bee Collections Network, 2014 (and updates). Version: 2015-03-18. National Science Foundation grant DBI 0956388"));
            assertThat(allStudy.getCitation(), is("Digital Bee Collections Network, 2014 (and updates). Version: 2015-03-18. National Science Foundation grant DBI 0956388"));

            AtomicBoolean success = new AtomicBoolean(false);
            NodeUtil.handleCollectedRelationships(new NodeTypeDirection(allStudy.getUnderlyingNode()), new NodeUtil.RelationshipListener() {
                @Override
                public void on(Relationship relationship) {
                    SpecimenNode spec = new SpecimenNode(relationship.getEndNode());
                    final String recordId = (String) spec.getUnderlyingNode().getProperty("idigbio:recordID");
                    assertThat(recordId, is(notNullValue()));
                    assertThat(spec.getExternalId(), is(recordId));
                    Term basisOfRecord = spec.getBasisOfRecord();
                    assertThat(basisOfRecord.getId(), either(is("TEST:PreservedSpecimen")).or(is("TEST:LabelObservation")));
                    assertThat(basisOfRecord.getName(), either(is("PreservedSpecimen")).or(is("LabelObservation")));
                    success.set(true);
                }
            });

            assertTrue(success.get());
        }

        assertThat(taxonIndex.findTaxonByName("Megandrena mentzeliae"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Mentzelia tricuspis"), is(notNullValue()));

    }

    @Test
    public void extractAssociatedNameGenusAndSpecificEpithet() {
        Map<String, String> assocMap = new TreeMap<String, String>() {{
            put(DatasetImporterForSeltmann.FIELD_ASSOCIATED_GENUS, "Donald");
            put(DatasetImporterForSeltmann.FIELD_ASSOCIATED_SPECIFIC_EPITHET, "duckus");
            put(DatasetImporterForSeltmann.FIELD_ASSOCIATED_SCIENTIFIC_NAME, "Donaldduckus");
        }
        };
        final String targetName = DatasetImporterForSeltmann.getTargetNameFromAssocMap(assocMap);
        assertThat(targetName, is("Donald duckus"));
    }

    @Test
    public void extractAssociatedNameScientificNameOnly() {
        Map<String, String> assocMap = new TreeMap<String, String>() {{
            put(DatasetImporterForSeltmann.FIELD_ASSOCIATED_SCIENTIFIC_NAME, "Donaldidae");
        }
        };
        final String targetName = DatasetImporterForSeltmann.getTargetNameFromAssocMap(assocMap);
        assertThat(targetName, is("Donaldidae"));
    }
}
