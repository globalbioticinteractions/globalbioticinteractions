package org.globalbioticinteractions.dataset;

import com.fasterxml.jackson.databind.JsonNode;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocal;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
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

        JsonNode config = EMLUtil.datasetWithEML(origDataset, URI.create(uriString));

        DatasetProxy proxy = new DatasetProxy(origDataset);
        proxy.setConfig(config);
        assertThat(proxy.getCitation(), is("Occurrence Records for vampire-moths-and-their-fruit-piercing-relatives. 2018-09-27. South Central California Network - 2ba077c1-aa41-455e-9a84-bccb61a91230."));
        assertThat(proxy.getFormat(), is(MIME_TYPE_DWCA));
    }

    @Test
    public void customEML() throws URISyntaxException, IOException {
        Dataset origDataset = new DatasetWithResourceMapping("some/namespace", URI.create("some:uri"), new ResourceServiceLocal(new InputStreamFactoryNoop()));
        String uriString = "jar:" + getClass().getResource("dwca-seltmann.zip").toURI().toString() + "!/taxonomy-darwin-core-1ac8b1c8b7728b13a6dba9fd5b64a3aeb036f5fb/eml.xml";

        JsonNode config = EMLUtil.datasetWithEML(origDataset, URI.create(uriString));

        DatasetProxy proxy = new DatasetProxy(origDataset);
        proxy.setConfig(config);
        assertThat(proxy.getCitation(), is("University of California Santa Barbara Invertebrate Zoology Collection. 2021-07-16."));
        assertThat(proxy.getFormat(), is(MIME_TYPE_DWCA));
    }

    @Test
    public void emlToMetaTables() throws URISyntaxException, IOException {
        Dataset origDataset = new DatasetWithResourceMapping("some/namespace", URI.create("some:uri"), new ResourceServiceLocal(new InputStreamFactoryNoop()));
        URI uriString = getClass().getResource("eml-table.xml").toURI();

        JsonNode config = EMLUtil.datasetWithEML(origDataset, uriString);

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
        JsonNode config = EMLUtil.datasetWithEML(origDataset, emlURI);

        DatasetProxy proxy = new DatasetProxy(origDataset);
        proxy.setConfig(config);
        assertThat(proxy.getCitation(), is("Illinois Natural History Survey Insect Collection. 2018-02-28."));
        assertThat(proxy.getFormat(), is(MIME_TYPE_DWCA));
    }

}