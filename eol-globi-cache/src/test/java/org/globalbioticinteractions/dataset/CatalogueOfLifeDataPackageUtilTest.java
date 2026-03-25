package org.globalbioticinteractions.dataset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.ResourceService;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class CatalogueOfLifeDataPackageUtilTest {

    @Test
    public void dataPackage() throws IOException, URISyntaxException {
        JsonNode jsonNode = CatalogueOfLifeDataPackageUtil.datasetFor(
                new ResourceService() {
                    @Override
                    public InputStream retrieve(URI resourceName) throws IOException {
                        InputStream is = getClass().getResourceAsStream(resourceName.toString());
                        assertNotNull("cannot find [" + resourceName + "]", is);
                        return is;
                    }
                },
                URI.create("coldp/metadata.yaml")
        );

        assertNotNull(jsonNode);

        JsonNode expectedConfig = new ObjectMapper().readTree(
                getClass().getResourceAsStream("coldp/globi-expected.json")
        );


        JsonNode tablesNode = jsonNode.at("/tables");
        assertThat(tablesNode.size(), Is.is(3));
        for (JsonNode table : tablesNode) {
            assertThat(table.at("/dcterms:bibliographicCitation").asText(),
                    Is.is("@misc{ChecklistBankDataset2017, publisher = {Belgian Biodiversity Platform, Belspo}, address = {Brussels, Belgium}, version = {2026-03-01}, url = {https://www.afromoths.net/}, title = {Afromoths, online database of Afrotropical moth species (Lepidoptera)}, author = {{De Prins}, {Jurate} and {De Prins}, {Willy}}, year = 2026, month = 3}")
            );
        }

    }

}