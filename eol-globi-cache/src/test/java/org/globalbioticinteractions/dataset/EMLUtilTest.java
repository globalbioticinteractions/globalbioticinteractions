package org.globalbioticinteractions.dataset;

import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetImpl;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class EMLUtilTest {

    @Test
    public void metaToMetaTables() throws URISyntaxException, IOException {
        Dataset origDataset = new DatasetImpl("some/namespace", URI.create("some:uri"));
        String uriString = "jar:" + getClass().getResource("dwca.zip").toURI().toString() + "!/vampire-moth-dwca-master/eml.xml";

        Dataset proxy = EMLUtil.datasetWithEML(origDataset, URI.create(uriString));

        assertThat(proxy.getCitation(), is ("Occurrence Records for vampire-moths-and-their-fruit-piercing-relatives. 2018-09-18. South Central California Network - 5f573b1a-0e9a-43cf-95d7-299207f98522."));
        assertThat(proxy.getFormat(), is ("application/dwca"));
        assertThat(proxy.getConfigURI(), is (notNullValue()));
    }

}