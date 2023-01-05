package org.eol.globi.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.domain.LogContext;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.tool.NullImportLogger;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetImporterForPlanqueIT extends GraphDBNeo4jTestCase {

    @Test
    public void importAll() throws StudyImporterException, IOException {
        final List<String> errorMessages = new ArrayList<String>();

        BaseDatasetImporter importer = new DatasetImporterForPlanque(
                new ParserFactoryLocal(getClass()), nodeFactory
        );
        JsonNode config = new ObjectMapper().readTree("{ \"citation\": \"Benjamin Planque, Raul Primicerio, Kathrine Michalsen, Michaela Aschan, Grégoire Certain, Padmini Dalpadado, Harald Gjøsæater, Cecilie Hansen, Edda Johannesen, Lis Lindal Jørgensen, Ina Kolsum, Susanne Kortsch, Lise-Marie Leclerc, Lena Omli, Mette Skern-Mauritzen, and Magnus Wiedmann 2014. Who eats whom in the Barents Sea: a food web topology from plankton to whales. Ecology 95:1430–1430. https://doi.org/10.1890/13-1062.1\",\n" +
                "  \"doi\": \"https://doi.org/10.1890/13-1062.1\",\n" +
                "  \"format\": \"planque\",\n" +
                "  \"resources\": {\n" +
                "    \"links\": \"http://www.esapubs.org/archive/ecol/E095/124/revised/PairwiseList.txt\",\n" +
                "    \"references\": \"http://www.esapubs.org/archive/ecol/E095/124/revised/References.txt\",\n" +
                "    \"referencesForLinks\": \"http://www.esapubs.org/archive/ecol/E095/124/revised/PairWise2References.txt\"\n" +
                "  }\n" +
                "}");
        DatasetImpl dataset = new DatasetWithResourceMapping("some/namespace", URI.create("http://example.com"), new ResourceServiceLocalAndRemote(inStream -> inStream));
        dataset.setConfig(config);
        importer.setDataset(dataset);

        importer.setLogger(new NullImportLogger() {
            @Override
            public void warn(LogContext ctx, String message) {
                errorMessages.add(message);
            }

        });
        importStudy(importer);

        int interactionCount = 0;
        List<StudyNode> studies = NodeUtil.findAllStudies(getGraphDb());
        for (StudyNode study : studies) {
            interactionCount += getSpecimenCount(study);
        }
        assertThat(interactionCount, is(4900));

        int uniqueReference = 236;

        // note that the +1 is for all links that had no reference associated to it
        assertThat(studies.size(), is(uniqueReference + 1));
        assertThat(taxonIndex.findTaxonByName("Trisopterus esmarkii"), is(notNullValue()));
        assertThat(errorMessages.size(), is(0));
    }


}
