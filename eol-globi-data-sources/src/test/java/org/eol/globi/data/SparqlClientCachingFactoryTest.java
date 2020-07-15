package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.HttpUtil;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SparqlClientCachingFactoryTest {


    @Test
    public void expectCaching() throws IOException {
        final ResourceService resourceService = singleRequestResourceService();
        final SparqlClient openBiodivClient = new SparqlClientCachingFactory().create(resourceService);
        assertCitation(openBiodivClient);
        assertCitation(openBiodivClient);
    }

    public void assertCitation(SparqlClient openBiodivClient) throws IOException {
        assertThat(StudyImporterForPensoft.findCitationByDoi("10.3897/zookeys.306.5455", openBiodivClient), is("Dewi Sartiami, Laurence A. Mound. 2013. Identification of the terebrantian thrips (Insecta, Thysanoptera) associated with cultivated plants in Java, Indonesia. ZooKeys. https://doi.org/10.3897/zookeys.306.5455"));
    }

    public static ResourceService singleRequestResourceService() {
        AtomicInteger counter = new AtomicInteger(0);
        return resourceName -> {
            if (counter.getAndIncrement() == 0) {
                return IOUtils.toInputStream("article,title,doi,authorsList,pubYear,journalName\n" +
                        "http://openbiodiv.net/D37E8D1A-221B-FFA6-FFE7-4458FFA0FFC2,\"Identification of the terebrantian thrips (Insecta, Thysanoptera) associated with cultivated plants in Java, Indonesia\",10.3897/zookeys.306.5455,\"Dewi Sartiami, Laurence A. Mound\",2013,ZooKeys\n",
                        StandardCharsets.UTF_8);
            } else {
                throw new IOException("should not ask twice");
            }
        };
    }

}