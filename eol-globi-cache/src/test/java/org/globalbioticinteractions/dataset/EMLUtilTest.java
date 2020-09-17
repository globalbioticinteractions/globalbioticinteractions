package org.globalbioticinteractions.dataset;

import org.codehaus.jackson.JsonNode;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.eol.globi.domain.PropertyAndValueDictionary.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class EMLUtilTest {

    @Test
    public void metaToMetaTables() throws URISyntaxException, IOException {
        Dataset origDataset = new DatasetImpl("some/namespace", URI.create("some:uri"), inStream -> inStream);
        String uriString = "jar:" + getClass().getResource("dwca.zip").toURI().toString() + "!/vampire-moth-dwca-c4549a1690b84595c88946f477057b9ab76e5360/eml.xml";

        JsonNode config = EMLUtil.datasetWithEML(origDataset, URI.create(uriString));

        DatasetProxy proxy = new DatasetProxy(origDataset);
        proxy.setConfig(config);
        assertThat(proxy.getCitation(), is ("Occurrence Records for vampire-moths-and-their-fruit-piercing-relatives. 2018-09-27. South Central California Network - 2ba077c1-aa41-455e-9a84-bccb61a91230."));
        assertThat(proxy.getFormat(), is (MIME_TYPE_DWCA));
    }

    @Test
    public void withEMLofINHS() throws URISyntaxException, IOException {
        Dataset origDataset = new DatasetImpl("some/namespace", URI.create("some:uri"), inStream -> inStream);


        URI emlURI = getClass().getResource("inhs-eml.xml").toURI();
        JsonNode config = EMLUtil.datasetWithEML(origDataset, emlURI);

        DatasetProxy proxy = new DatasetProxy(origDataset);
        proxy.setConfig(config);
        assertThat(proxy.getFormat(), is (MIME_TYPE_DWCA));
        assertThat(proxy.getCitation(), is ("Illinois Natural History Survey Insect Collection. 2018-02-28."));
    }

}