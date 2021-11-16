package org.eol.globi.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.ResourceUtil;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.LOCALITY_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_DOI;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_URL;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_PATH;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_PATH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;

public class DatasetImporterForZOVERIT {

    @Test
    public void retrieveTicks() throws IOException {
        assertSame(DatasetImporterForZOVER.getContent(
                DatasetImporterForZOVER.ZOVER_TICK,
                new TestResourceService(), "http://www.mgc.ac.cn"), "zover/tick.json"
        );
    }

    @Test
    public void retrieveBats() throws IOException {
        assertSame(DatasetImporterForZOVER.getContent(
                DatasetImporterForZOVER.ZOVER_CHIROPTERA,
                new TestResourceService(), "http://www.mgc.ac.cn"), "zover/chiroptera.json");
    }

    @Test
    public void retrieveRodent() throws IOException {
        assertSame(DatasetImporterForZOVER.getContent(
                DatasetImporterForZOVER.ZOVER_RODENT,
                new TestResourceService(), "http://www.mgc.ac.cn")
                , "zover/rodent.json");
    }

    @Test
    public void retrieveMosquito() throws IOException {
        assertSame(DatasetImporterForZOVER.getContent(
                DatasetImporterForZOVER.ZOVER_MOSQUITO,
                new TestResourceService(), "http://www.mgc.ac.cn"),
                "zover/mosquito.json");
    }

    @Test
    public void retrieveMosquitoVirusData() throws IOException {
        String s = DatasetImporterForZOVER.getVirusData(
                DatasetImporterForZOVER.ZOVER_MOSQUITO, 35237L,
                new TestResourceService(), "http://www.mgc.ac.cn"
        );
        assertSame(s, "zover/mosquito_viruses_35237.json");
    }

    @Test
    public void retrieveMosquitoVirusData35222() throws IOException {
        String s = DatasetImporterForZOVER.getVirusData(
                DatasetImporterForZOVER.ZOVER_MOSQUITO, 35222L,
                new TestResourceService(), "http://www.mgc.ac.cn"
        );
        assertSame(s, "zover/mosquito_viruses_35222.json");
    }

    @Test
    public void retrieveBatVirusData() throws IOException {
        String s = DatasetImporterForZOVER.getVirusData(
                DatasetImporterForZOVER.ZOVER_CHIROPTERA,
                35237L,
                new TestResourceService(), "http://www.mgc.ac.cn"
        );
        assertSame(s, "zover/chiroptera_viruses_35237.json");
    }

    @Test
    public void retrieveTickVirusData() throws IOException {
        String s = DatasetImporterForZOVER.getVirusData(
                DatasetImporterForZOVER.ZOVER_TICK,
                35237L,
                new TestResourceService(), "http://www.mgc.ac.cn"
        );
        assertSame(s, "zover/tick_viruses_35237.json");
    }

    @Test
    public void retrieveRodentVirusData() throws IOException {
        String s = DatasetImporterForZOVER.getVirusData(
                DatasetImporterForZOVER.ZOVER_RODENT,
                35237L,
                new TestResourceService(), "http://www.mgc.ac.cn"
        );
        assertSame(s, "zover/rodent_viruses_35237.json");
    }

    private void assertSame(String content, String resourceName) throws IOException {
        InputStream resource = getClass().getResourceAsStream(resourceName);
        String expectedContent = IOUtils.toString(resource, StandardCharsets.UTF_8);
        assertThat(content, Is.is(expectedContent));
    }


    private static class TestResourceService implements ResourceService {
        @Override
        public InputStream retrieve(URI resourceName) throws IOException {
            return ResourceUtil.asInputStream(resourceName, in -> in);
        }
    }
}