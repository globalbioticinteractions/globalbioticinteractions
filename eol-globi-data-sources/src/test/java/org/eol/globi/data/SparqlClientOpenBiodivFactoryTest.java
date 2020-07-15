package org.eol.globi.data;

import org.eol.globi.service.ResourceService;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SparqlClientOpenBiodivFactoryTest {

    @Test(expected = IOException.class)
    public void expectNonCaching() throws IOException {
        final ResourceService resourceService = SparqlClientCachingFactoryTest.singleRequestResourceService();
        final SparqlClient openBiodivClient = new OpenBiodivClient(resourceService);
        StudyImporterForPensoft.findCitationByDoi("10.3897/zookeys.306.5455", openBiodivClient);
        try {
            StudyImporterForPensoft.findCitationByDoi("10.3897/zookeys.306.5455", openBiodivClient);
        } catch (IOException ex) {
            assertThat(ex.getCause().getMessage(), is("should not ask twice"));
            throw ex;
        }

    }


}