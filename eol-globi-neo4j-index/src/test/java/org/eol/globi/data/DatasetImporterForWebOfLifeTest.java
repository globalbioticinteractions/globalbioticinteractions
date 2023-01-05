package org.eol.globi.data;

import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class DatasetImporterForWebOfLifeTest extends GraphDBNeo4jTestCase {

    @Test
    public void generateArchiveURL() {
        final List<URI> networkNames = Arrays.asList(URI.create("A_HP_002"), URI.create("A_HP_003"));
        URI generatedArchiveURL = DatasetImporterForWebOfLife.generateArchiveURL(networkNames);

        String expectedArchiveURL = "http://www.web-of-life.es/map_download_fast2.php?format=csv&networks=" + "A_HP_002,A_HP_003" + "&species=yes&type=All&data=All&speciesrange=&interactionsrange=&searchbox=&checked=false";

        assertThat(generatedArchiveURL, is(URI.create(expectedArchiveURL)));
    }


}

