package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.service.DatasetLocal;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.ResourceServiceHTTP;
import org.eol.globi.util.ResourceServiceLocal;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class DatasetImporterForWebOfLifeIT extends GraphDBNeo4jTestCase {


    @Test
    public void retrieveNetworkList() throws IOException {
        String resource = DatasetImporterForWebOfLife.WEB_OF_LIFE_BASE_URL + "/networkslist.php?type=All&data=All";
        final List<URI> networkNames =
                DatasetImporterForWebOfLife
                        .getNetworkNames(new ResourceServiceHTTP(new InputStreamFactoryNoop(), getCacheDir())
                                .retrieve(URI.create(resource))
                        );

        assertThat(networkNames, hasItem(URI.create("A_HP_002")));
        assertThat(networkNames.size() > 50, is(true));
    }


}

