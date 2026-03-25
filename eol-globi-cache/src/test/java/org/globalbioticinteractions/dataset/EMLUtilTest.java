package org.globalbioticinteractions.dataset;

import com.fasterxml.jackson.databind.JsonNode;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocal;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.eol.globi.domain.PropertyAndValueDictionary.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

public class EMLUtilTest {

    @Test
    public void metaToMetaTables() throws URISyntaxException, IOException {
        Dataset origDataset = new DatasetWithResourceMapping("some/namespace", URI.create("some:uri"), new ResourceServiceLocal(new InputStreamFactoryNoop()));
        String uriString = "jar:" + getClass().getResource("dwca.zip").toURI().toString() + "!/vampire-moth-dwca-c4549a1690b84595c88946f477057b9ab76e5360/eml.xml";

        JsonNode config = EMLUtil.datasetFor(origDataset, URI.create(uriString));

        DatasetProxy proxy = new DatasetProxy(origDataset);
        proxy.setConfig(config);
        assertThat(proxy.getCitation(), is("Occurrence Records for vampire-moths-and-their-fruit-piercing-relatives. 2018-09-27. South Central California Network - 2ba077c1-aa41-455e-9a84-bccb61a91230."));
        assertThat(proxy.getFormat(), is(MIME_TYPE_DWCA));
    }

    @Test
    public void customEML() throws URISyntaxException, IOException {
        Dataset origDataset = new DatasetWithResourceMapping("some/namespace", URI.create("some:uri"), new ResourceServiceLocal(new InputStreamFactoryNoop()));
        String uriString = "jar:" + getClass().getResource("dwca-seltmann.zip").toURI().toString() + "!/taxonomy-darwin-core-1ac8b1c8b7728b13a6dba9fd5b64a3aeb036f5fb/eml.xml";

        JsonNode config = EMLUtil.datasetFor(origDataset, URI.create(uriString));

        DatasetProxy proxy = new DatasetProxy(origDataset);
        proxy.setConfig(config);
        assertThat(proxy.getCitation(), is("University of California Santa Barbara Invertebrate Zoology Collection. 2021-07-16."));
        assertThat(proxy.getFormat(), is(MIME_TYPE_DWCA));
    }

    @Test
    public void emlToMetaTables() throws URISyntaxException, IOException {
        Dataset origDataset = new DatasetWithResourceMapping("some/namespace", URI.create("some:uri"), new ResourceServiceLocal(new InputStreamFactoryNoop()));
        URI uriString = getClass().getResource("eml-table.xml").toURI();

        JsonNode config = EMLUtil.datasetFor(origDataset, uriString);

        DatasetProxy proxy = new DatasetProxy(origDataset);
        proxy.setConfig(config);
        assertThat(proxy.getCitation(), is("WorldFAIR pilot data from: VisitationData_Luisa_Carvalheiro."));
        assertThat(proxy.getFormat(), not(is(MIME_TYPE_DWCA)));
        assertThat(proxy.getFormat(), is("globi"));

        JsonNode context = proxy.getConfig().get("@context");
        assertThat(context, is(notNullValue()));
        assertThat(context.toString(), is("[\"http://www.w3.org/ns/csvw\",{\"@language\":\"en\"}]"));

        JsonNode tablesNode = proxy.getConfig().get("tables");
        assertThat(tablesNode.size(), is(1));
        JsonNode tableNode = tablesNode.get(0);
        assertThat(tableNode.get("delimiter").textValue(), is("\t"));
        assertThat(tableNode.get("headerRowCount").textValue(), is("6"));
        assertThat(tableNode.get("url").textValue(), is("https://docs.google.com/spreadsheets/u/1/d/1cJ0qX9ppqHoSyqFykwYJef-DFOzoutthBXjwKRY81T8/export?format=tsv&id=1cJ0qX9ppqHoSyqFykwYJef-DFOzoutthBXjwKRY81T8&gid=776329546"));
        JsonNode jsonNode = tableNode.get("tableSchema").get("columns");
        assertThat(jsonNode.size(), is(198));
        assertThat(jsonNode.get(0).get("name").textValue(), is("sourceCatalogNumber"));
        assertThat(jsonNode.get(jsonNode.size() - 1).get("name").textValue(), is("Caste"));

    }

    @Test
    public void withEMLofINHS() throws URISyntaxException, IOException {
        Dataset origDataset = new DatasetWithResourceMapping("some/namespace", URI.create("some:uri"), new ResourceServiceLocal(new InputStreamFactoryNoop()));


        URI emlURI = getClass().getResource("inhs-eml.xml").toURI();
        JsonNode config = EMLUtil.datasetFor(origDataset, emlURI);

        DatasetProxy proxy = new DatasetProxy(origDataset);
        proxy.setConfig(config);
        assertThat(proxy.getCitation(), is("Illinois Natural History Survey Insect Collection. 2018-02-28."));
        assertThat(proxy.getFormat(), is(MIME_TYPE_DWCA));
    }

    @Test
    public void withBibtexUsageCitation() throws URISyntaxException, IOException {
        Dataset origDataset = new DatasetWithResourceMapping("some/namespace", URI.create("some:uri"), new ResourceServiceLocal(new InputStreamFactoryNoop()));


        URI emlURI = getClass().getResource("eml-bibtex.xml").toURI();
        JsonNode config = EMLUtil.datasetFor(origDataset, emlURI);

        DatasetProxy proxy = new DatasetProxy(origDataset);
        proxy.setConfig(config);
        assertThat(proxy.getCitation(), is("@article{Barberis_Bitonto_Costantino_Bianco_Birtele_Bonifacino_Cangelmi_Capò_Chroni_d’Agostino_et al._2025, title={Insect-flower interactions in the Mediterranean area: a Citizen Science dataset collated within the LIFE 4 Pollinators project}, volume={39}, url={https://www.pollinationecology.org/index.php/jpe/article/view/872}, DOI={10.26786/1920-7603(2025)872}, abstractNote={&amp;lt;p&amp;gt;Pollinators play a vital role in most terrestrial ecosystems, supporting wild plant communities and enhancing agricultural yields. However, despite their ecological and economic importance, they have been experiencing an alarming decline over the past decades. The Mediterranean region, known for harboring highly diverse communities of plants and pollinators, is particularly vulnerable due to intense anthropogenic pressures. Furthermore, the ecological roles of many floral visitors remain poorly understood, hindering conservation efforts. In response, in recent years, growing attention has been directed toward the contribution that citizens can give in support of pollinator research. An increasing number of projects have adopted a Citizen Science approach to enable large-scale data collection. The LIFE 4 Pollinators project (LIFE18/GIE/IT/000755) “Involving people to protect wild bees and other pollinators in the Mediterranean” aims to promote the conservation of pollinating insects and entomophilous plants across the Mediterranean region by fostering progressive changes in human practices that threaten wild pollinators. In addition to the implementation of several actions to raise awareness, the project launched a web platform to collect photographic records of flower–insect interaction from the public. The platform is expected to remain active for at least ten years, during which we encourage continuing record submissions by interested bodies. With this data paper we are making the current dataset freely accessible to anyone, committing to periodic online updates.&amp;lt;/p&amp;gt;}, journal={Journal of Pollination Ecology}, author={Barberis, Marta and Bitonto, Fortunato Fulvio and Costantino, Roberto and Bianco, Lorenzo and Birtele, Daniele and Bonifacino, Marco and Cangelmi, Giacomo and Capò, Miquel and Chroni, Athanasia and d’Agostino, Marco and et al.}, year={2025}, month={Nov.}, pages={306–315} }"));
        assertThat(proxy.getFormat(), is(MIME_TYPE_DWCA));
    }

}