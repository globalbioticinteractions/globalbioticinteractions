package org.eol.globi.data;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.LogContext;
import org.eol.globi.domain.Study;
import org.eol.globi.service.DatasetImpl;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterForPlanqueIT extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException, NodeFactoryException, IOException {
        final List<String> errorMessages = new ArrayList<String>();


        BaseStudyImporter importer = new StudyImporterForPlanque(new ParserFactoryLocal(), nodeFactory);
        JsonNode config = new ObjectMapper().readTree("{ \"citation\": \"Benjamin Planque, Raul Primicerio, Kathrine Michalsen, Michaela Aschan, Grégoire Certain, Padmini Dalpadado, Harald Gjøsæater, Cecilie Hansen, Edda Johannesen, Lis Lindal Jørgensen, Ina Kolsum, Susanne Kortsch, Lise-Marie Leclerc, Lena Omli, Mette Skern-Mauritzen, and Magnus Wiedmann 2014. Who eats whom in the Barents Sea: a food web topology from plankton to whales. Ecology 95:1430–1430. https://doi.org/10.1890/13-1062.1\",\n" +
                "  \"doi\": \"https://doi.org/10.1890/13-1062.1\",\n" +
                "  \"format\": \"planque\",\n" +
                "  \"resources\": {\n" +
                "    \"links\": \"http://www.esapubs.org/archive/ecol/E095/124/revised/PairwiseList.txt\",\n" +
                "    \"references\": \"http://www.esapubs.org/archive/ecol/E095/124/revised/References.txt\",\n" +
                "    \"referencesForLinks\": \"http://www.esapubs.org/archive/ecol/E095/124/revised/PairWise2References.txt\"\n" +
                "  }\n" +
                "}");
        DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create("http://example.com"));
        dataset.setConfig(config);
        importer.setDataset(dataset);

        importer.setLogger(new ImportLogger() {
            @Override
            public void warn(LogContext study, String message) {
                errorMessages.add(message);
            }

            @Override
            public void info(LogContext study, String message) {

            }

            @Override
            public void severe(LogContext study, String message) {

            }
        });
        importStudy(importer);

        int interactionCount = 0;
        List<Study> studies = NodeUtil.findAllStudies(getGraphDb());
        for (Study study : studies) {
            Iterable<Relationship> specimenRels = NodeUtil.getSpecimens(study);
            for (Relationship specimenRel : specimenRels) {
                interactionCount++;
            }
        }
        assertThat(interactionCount, is(4900));

        int uniqueReference = 236;

        // note that the +1 is for all links that had no reference associated to it
        assertThat(studies.size(), is(uniqueReference + 1));
        assertThat(taxonIndex.findTaxonByName("Trisopterus esmarkii"), is(notNullValue()));
        assertThat(errorMessages.size(), is(0));
    }

}
