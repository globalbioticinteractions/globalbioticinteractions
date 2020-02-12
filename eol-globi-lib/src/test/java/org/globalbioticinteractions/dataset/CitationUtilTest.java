package org.globalbioticinteractions.dataset;

import org.codehaus.jackson.map.ObjectMapper;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class CitationUtilTest {

    @Test
    public void citationDefault() throws IOException {

        Dataset dataset = Mockito.mock(Dataset.class);

        when(dataset.getConfig()).thenReturn(new ObjectMapper().readTree("{\n" +
                "  \"@context\": [\"http://www.w3.org/ns/csvw\", {\"@language\": \"en\"}],\n" +
                "  \"rdfs:comment\": [\"inspired by https://www.w3.org/TR/2015/REC-tabular-data-model-20151217/\"],\n" +
                "  \"tables\": [\n" +
                "    { \"url\": \"interactions.tsv\",\n" +
                "      \"dcterms:bibliographicCitation\": \"Gandhi, K. J. K., & Herms, D. A. (2009). North American arthropods at risk due to widespread Fraxinus mortality caused by the Alien Emerald ash borer. Biological Invasions, 12(6), 1839–1846. doi:10.1007/s10530-009-9594-1.\",\n" +
                "      \"doi\": \"10.1007/s10530-009-9594-1\",\n" +
                "      \"tableSchema\": \"interactions.tsv-schema.json\",\n" +
                "      \"headerRowCount\": 1,\n" +
                "      \"delimiter\": \"\\t\",\n" +
                "      \"interactionTypeId\": \"http://purl.obolibrary.org/obo/RO_0002437\",\n" +
                "      \"interactionTypeName\": \"interactsWith\"\n" +
                "    }\n" +
                "  ]\n" +
                "}"));

        String citation = CitationUtil.citationOrDefaultFor(dataset, "foo");
        assertThat(citation, Is.is("Gandhi, K. J. K., & Herms, D. A. (2009). North American arthropods at risk due to widespread Fraxinus mortality caused by the Alien Emerald ash borer. Biological Invasions, 12(6), 1839–1846. doi:10.1007/s10530-009-9594-1."));
    }

}