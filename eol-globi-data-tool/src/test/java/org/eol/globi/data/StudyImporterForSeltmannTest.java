package org.eol.globi.data;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Term;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetImpl;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class StudyImporterForSeltmannTest extends GraphDBTestCase {

    @Test
    public void importSome() throws StudyImporterException, IOException {
        StudyImporterForSeltmann importer = new StudyImporterForSeltmann(null, nodeFactory);
        Dataset dataset = new DatasetImpl("some/namespace", URI.create("http://example.com"));
        JsonNode config = new ObjectMapper().readTree("{\"citation\": \"some citation\", \"resources\": {\"archive\": \"classpath:/org/eol/globi/data/seltmann/testArchive.zip\"}}");
        dataset.setConfig(config);
        importer.setDataset(dataset);
        importStudy(importer);

        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        for (Study allStudy : allStudies) {
            assertThat(allStudy.getSource(), startsWith("Digital Bee Collections Network, 2014 (and updates). Version: 2015-03-18. National Science Foundation grant DBI 0956388"));
            assertThat(allStudy.getCitation(), is("citation:doi:Digital Bee Collections Network, 2014 (and updates). Version: 2015-03-18. National Science Foundation grant DBI 0956388"));
            Iterable<Relationship> specimens = allStudy.getSpecimens();
            for (Relationship specimen : specimens) {
                Specimen spec = new Specimen(specimen.getEndNode());
                final String recordId = (String) spec.getUnderlyingNode().getProperty("idigbio:recordID");
                assertThat(recordId, is(notNullValue()));
                assertThat(spec.getExternalId(), is(recordId));
                Term basisOfRecord = spec.getBasisOfRecord();
                assertThat(basisOfRecord.getId(), either(is("TEST:PreservedSpecimen")).or(is("TEST:LabelObservation")));
                assertThat(basisOfRecord.getName(), either(is("PreservedSpecimen")).or(is("LabelObservation")));
            }
        }

        assertThat(taxonIndex.findTaxonByName("Megandrena mentzeliae"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Mentzelia tricuspis"), is(notNullValue()));

    }

    @Test
    public void extractAssociatedNameGenusAndSpecificEpithet() {
        Map<String, String> assocMap = new TreeMap<String, String>() {{
            put(StudyImporterForSeltmann.FIELD_ASSOCIATED_GENUS, "Donald");
            put(StudyImporterForSeltmann.FIELD_ASSOCIATED_SPECIFIC_EPITHET, "duckus");
            put(StudyImporterForSeltmann.FIELD_ASSOCIATED_SCIENTIFIC_NAME, "Donaldduckus");
        }
        };
        final String targetName = StudyImporterForSeltmann.getTargetNameFromAssocMap(assocMap);
        assertThat(targetName, is("Donald duckus"));
    }

    @Test
    public void extractAssociatedNameScientificNameOnly() {
        Map<String, String> assocMap = new TreeMap<String, String>() {{
            put(StudyImporterForSeltmann.FIELD_ASSOCIATED_SCIENTIFIC_NAME, "Donaldidae");
        }
        };
        final String targetName = StudyImporterForSeltmann.getTargetNameFromAssocMap(assocMap);
        assertThat(targetName, is("Donaldidae"));
    }
}
