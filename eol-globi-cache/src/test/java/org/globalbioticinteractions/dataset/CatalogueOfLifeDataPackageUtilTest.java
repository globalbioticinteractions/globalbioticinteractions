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
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class CatalogueOfLifeDataPackageUtilTest {

    @Test
    public void dataPackage() throws IOException, URISyntaxException {
        JsonNode jsonNode = CatalogueOfLifeDataPackageUtil.datasetFor(
                new ResourceService() {
                    @Override
                    public InputStream retrieve(URI resourceName) throws IOException {
                        InputStream is = getClass().getResourceAsStream("coldp/" + resourceName.toString());
                        if (is == null) {
                            throw new IOException("cannot find [" + resourceName + "]");
                        }
                        return is;
                    }
                },
                URI.create("metadata.yaml")
        );

        assertNotNull(jsonNode);

        JsonNode tablesNode = jsonNode.at("/tables");
        assertThat(tablesNode.size(), Is.is(3));
        for (JsonNode table : tablesNode) {
            assertThat(table.at("/dcterms:bibliographicCitation").asText(),
                    Is.is("@misc{ChecklistBankDataset2017, publisher = {Belgian Biodiversity Platform, Belspo}, address = {Brussels, Belgium}, version = {2026-03-01}, url = {https://www.afromoths.net/}, title = {Afromoths, online database of Afrotropical moth species (Lepidoptera)}, author = {{De Prins}, {Jurate} and {De Prins}, {Willy}}, year = 2026, month = 3}")
            );
        }

    }

    @Test
    public void dataPackageTaxonWorks() throws IOException, URISyntaxException {
        JsonNode jsonNode = CatalogueOfLifeDataPackageUtil.datasetFor(
                new ResourceService() {
                    @Override
                    public InputStream retrieve(URI resourceName) throws IOException {
                        InputStream is = getClass().getResourceAsStream("coldp-non-name-usage-taxonworks/" + resourceName.toString());
                        if (is == null) {
                            throw new IOException("cannot find [" + resourceName + "]");
                        }
                        return is;
                    }
                },
                URI.create("metadata.yaml")
        );

        assertNotNull(jsonNode);

        Map<String, Integer> tableColumns = new TreeMap<>();
        JsonNode tablesNode = jsonNode.at("/tables");
        assertThat(tablesNode.size(), Is.is(4));
        for (JsonNode table : tablesNode) {
            tableColumns.put(table.at("/url").asText(), table.at("/tableSchema/columns").size());
            assertThat(table.at("/dcterms:bibliographicCitation").asText(),
                    Is.is("@misc{ChecklistBankDataset2317, version = {Mar 2026}, url = {https://hoppers.speciesfile.org/}, title = {3i World Auchenorrhyncha Database}, author = {{Dmitriev}, {D.A.}}, year = 2026, month = 3}")
            );
        }

        assertThat(tableColumns.get("Taxon.tsv"), Is.is(18));
        assertThat(tableColumns.get("Name.tsv"), Is.is(21));
        assertThat(tableColumns.get("References.tsv"), Is.is(5));
        assertThat(tableColumns.get("SpeciesInteraction.tsv"), Is.is(8));
    }

    @Test
    public void dataPackageHobern() throws IOException, URISyntaxException {
        JsonNode jsonNode = CatalogueOfLifeDataPackageUtil.datasetFor(
                new ResourceService() {
                    @Override
                    public InputStream retrieve(URI resourceName) throws IOException {
                        InputStream is = getClass().getResourceAsStream("coldp-non-name-usage-hobern/" + resourceName.toString());
                        if (is == null) {
                            throw new IOException("cannot find [" + resourceName + "]");
                        }
                        return is;
                    }
                },
                URI.create("metadata.yaml")
        );

        assertNotNull(jsonNode);

        Map<String, Integer> tableColumns = new TreeMap<>();
        JsonNode tablesNode = jsonNode.at("/tables");
        assertThat(tablesNode.size(), Is.is(4));
        for (JsonNode table : tablesNode) {
            tableColumns.put(table.at("/url").asText(), table.at("/tableSchema/columns").size());
            assertThat(table.at("/dcterms:bibliographicCitation").asText(),
                    Is.is("@misc{ChecklistBankDataset2362, version = {1.1.26.046}, title = {Catalogue of World Gelechiidae}, author = {{Hobern}, {Donald} and {Sattler}, {Klaus}}, year = 2026, month = 2}")
            );
        }

        assertThat(tableColumns.get("data/taxon.csv"), Is.is(22));
        assertThat(tableColumns.get("data/name.csv"), Is.is(17));
        assertThat(tableColumns.get("data/reference.csv"), Is.is(10));
        assertThat(tableColumns.get("data/speciesinteraction.csv"), Is.is(11));


    }

}