package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.NodeListener;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.ResourceServiceLocal;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.eol.globi.data.DatasetImporterForTSV.ASSOCIATED_TAXA;
import static org.eol.globi.data.DatasetImporterForTSV.DATASET_CITATION;
import static org.eol.globi.data.DatasetImporterForTSV.HABITAT_ID;
import static org.eol.globi.data.DatasetImporterForTSV.HABITAT_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_URL;
import static org.eol.globi.domain.PropertyAndValueDictionary.NETWORK_ID;
import static org.eol.globi.domain.PropertyAndValueDictionary.NETWORK_NAME;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertTrue;

public class DatasetImporterForJSONTest extends GraphDBNeo4jTestCase {


    @Test
    public void importFewLinesJSON() throws StudyImporterException {
        Dataset dataset = getDataset(new TreeMap<URI, String>() {{
            put(URI.create("/interactions.json"), "{\"http://www.w3.org/ns/prov#wasDerivedFrom\":\"hash://sha256/f1c123b38bdfab129d8a393b06842eddae823bf08a2c91b1100399940b582a9a\",\"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\":\"application/vnd.taxonworks+json\",\"referenceId\":\"https://sfg.taxonworks.org/api/v1/sources/49013\",\"interactionId\":\"https://sfg.taxonworks.org/api/v1/biological_associations/84748\",\"taxonRootsResolved\":2,\"referenceResolved\":true,\"referenceCitation\":\"@article{49013,\\n  author = {Abate, T.},\\n  booktitle = {Journal of Applied Entomology},\\n  journal = {Journal of Applied Entomology},\\n  month = {mar},\\n  day = {31},\\n  pages = {278-285},\\n  title = {The bean fly <i>Ophiomyia phaseoli</i> Tryon (Diptera: Agromyzidae) and its parasitoids in Ethiopia.},\\n  volume = {111(3)},\\n  year = {1991},\\n  stated_year = {1991},\\n  year_suffix = {a},\\n  issn = {0044-2240}\\n}\\n\",\"interactionTypeName\":\"interactsWith\",\"sourceTaxonName\":\"Eupelmus\",\"sourceTaxonId\":\"gid://taxon-works/TaxonName/456381\",\"sourceTaxonRank\":\"genus\",\"sourceTaxonPath\":\"Root | Animalia | Arthropoda | Insecta | Hymenoptera | Chalcidoidea | Eupelmidae | Eupelminae | Eupelmus\",\"sourceTaxonPathIds\":\"gid://taxon-works/TaxonName/455455 | gid://taxon-works/TaxonName/703648 | gid://taxon-works/TaxonName/703653 | gid://taxon-works/TaxonName/703658 | gid://taxon-works/TaxonName/455456 | gid://taxon-works/TaxonName/455458 | gid://taxon-works/TaxonName/455504 | gid://taxon-works/TaxonName/455506 | gid://taxon-works/TaxonName/456381\",\"sourceTaxonPathNames\":\"nomenclatural rank | kingdom | phylum | class | order | superfamily | family | subfamily | genus\",\"targetTaxonName\":\"Agromyzidae\",\"targetTaxonId\":\"gid://taxon-works/TaxonName/513569\",\"targetTaxonRank\":\"family\",\"targetTaxonPath\":\"Root | Animalia | Arthropoda | Insecta | Diptera | Agromyzidae\",\"targetTaxonPathIds\":\"gid://taxon-works/TaxonName/455455 | gid://taxon-works/TaxonName/703648 | gid://taxon-works/TaxonName/703653 | gid://taxon-works/TaxonName/703658 | gid://taxon-works/TaxonName/513567 | gid://taxon-works/TaxonName/513569\",\"targetTaxonPathNames\":\"nomenclatural rank | kingdom | phylum | class | order | family\"}\n" +
                    "{\"http://www.w3.org/ns/prov#wasDerivedFrom\":\"hash://sha256/84b5e8aa0dfdb83fdf500a3f759ff782ca91ff2710d017841eeb82ed6eb2016e\",\"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\":\"application/vnd.taxonworks+json\",\"referenceId\":\"https://sfg.taxonworks.org/api/v1/sources/49013\",\"interactionId\":\"https://sfg.taxonworks.org/api/v1/biological_associations/112687\",\"taxonRootsResolved\":2,\"referenceResolved\":true,\"referenceCitation\":\"@article{49013,\\n  author = {Abate, T.},\\n  booktitle = {Journal of Applied Entomology},\\n  journal = {Journal of Applied Entomology},\\n  month = {mar},\\n  day = {31},\\n  pages = {278-285},\\n  title = {The bean fly <i>Ophiomyia phaseoli</i> Tryon (Diptera: Agromyzidae) and its parasitoids in Ethiopia.},\\n  volume = {111(3)},\\n  year = {1991},\\n  stated_year = {1991},\\n  year_suffix = {a},\\n  issn = {0044-2240}\\n}\\n\",\"interactionTypeName\":\"interactsWith\",\"targetTaxonName\":\"Diptera\",\"targetTaxonId\":\"gid://taxon-works/TaxonName/513567\",\"targetTaxonRank\":\"order\",\"targetTaxonPath\":\"Root | Animalia | Arthropoda | Insecta | Diptera\",\"targetTaxonPathIds\":\"gid://taxon-works/TaxonName/455455 | gid://taxon-works/TaxonName/703648 | gid://taxon-works/TaxonName/703653 | gid://taxon-works/TaxonName/703658 | gid://taxon-works/TaxonName/513567\",\"targetTaxonPathNames\":\"nomenclatural rank | kingdom | phylum | class | order\",\"sourceTaxonName\":\"null\",\"sourceTaxonId\":\"gid://taxon-works/TaxonName/505259\",\"sourceTaxonRank\":\"\",\"sourceTaxonPath\":\"Root | Animalia | Arthropoda | Insecta | Hymenoptera | Chalcidoidea | Eulophidae | Entedoninae | null\",\"sourceTaxonPathIds\":\"gid://taxon-works/TaxonName/455455 | gid://taxon-works/TaxonName/703648 | gid://taxon-works/TaxonName/703653 | gid://taxon-works/TaxonName/703658 | gid://taxon-works/TaxonName/455456 | gid://taxon-works/TaxonName/455458 | gid://taxon-works/TaxonName/455498 | gid://taxon-works/TaxonName/455501 | gid://taxon-works/TaxonName/505259\",\"sourceTaxonPathNames\":\"nomenclatural rank | kingdom | phylum | class | order | superfamily | family | subfamily\"}\n");
        }});

        DatasetImporterForJSON importer = new DatasetImporterForJSON(null, nodeFactory);
        importer.setDataset(dataset);
        importStudy(importer);

        assertExists("Agromyzidae");
        assertExists("Diptera");

        assertStudyTitles("https://sfg.taxonworks.org/api/v1/sources/49013");
    }

    public void assertStudyTitles(String element) {
        final List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        final List<String> titles = new ArrayList<String>();
        for (Study study : allStudies) {
            titles.add(study.getTitle());
        }
        assertThat(titles, hasItem(element));
    }

    protected void assertExists(String taxonName) throws NodeFactoryException {
        Taxon taxon = taxonIndex.findTaxonByName(taxonName);
        assertThat(taxon, is(notNullValue()));
        assertThat(taxon.getName(), is(taxonName));
    }

    public DatasetImpl getDataset(TreeMap<URI, String> treeMap) {
        return new DatasetWithResourceMapping("someRepo", URI.create("http://example.com"),
                new ResourceServiceLocal(new InputStreamFactoryNoop())) {
            @Override
            public InputStream retrieve(URI resource) throws IOException {
                String input = treeMap.get(resource);
                return input == null ? null : IOUtils.toInputStream(input, StandardCharsets.UTF_8);
            }
        };
    }

}