package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceHTTP;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;

public class DatasetImporterForZOVERIT {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void retrieveTicks() throws IOException {
        assertSame(DatasetImporterForZOVER.getContent(
                DatasetImporterForZOVER.ZOVER_TICK,
                getResourceService(),
                "http://www.mgc.ac.cn"),
                "zover/tick.json"
        );
    }

    @Test
    public void retrieveBats() throws IOException {
        assertSame(DatasetImporterForZOVER.getContent(
                DatasetImporterForZOVER.ZOVER_CHIROPTERA,
                getResourceService(),
                "http://www.mgc.ac.cn"),
                "zover/chiroptera.json");
    }

    @Test
    public void retrieveRodent() throws IOException {
        assertSame(DatasetImporterForZOVER.getContent(
                DatasetImporterForZOVER.ZOVER_RODENT,
                getResourceService(),
                "http://www.mgc.ac.cn")
                , "zover/rodent.json");
    }

    @Test
    public void retrieveMosquito() throws IOException {
        assertSame(DatasetImporterForZOVER.getContent(
                DatasetImporterForZOVER.ZOVER_MOSQUITO,
                getResourceService(),
                "http://www.mgc.ac.cn"),
                "zover/mosquito.json");
    }

    @Test
    public void retrieveMosquitoVirusData() throws IOException {
        String s = DatasetImporterForZOVER.getVirusData(
                DatasetImporterForZOVER.ZOVER_MOSQUITO, 35237L,
                getResourceService(),
                "http://www.mgc.ac.cn"
        );
        assertSame(s, "zover/mosquito_viruses_35237.json");
    }

    private ResourceServiceHTTP getResourceService() throws IOException {
        return new ResourceServiceHTTP(new InputStreamFactoryNoop(), folder.newFolder());
    }

    @Test
    public void retrieveMosquitoVirusData35222() throws IOException {
        String s = DatasetImporterForZOVER.getVirusData(
                DatasetImporterForZOVER.ZOVER_MOSQUITO, 35222L,
                getResourceService(),
                "http://www.mgc.ac.cn"
        );
        assertSame(s, "zover/mosquito_viruses_35222.json");
    }

    @Test
    public void retrieveBatVirusData() throws IOException {
        String s = DatasetImporterForZOVER.getVirusData(
                DatasetImporterForZOVER.ZOVER_CHIROPTERA,
                35237L,
                getResourceService(),
                "http://www.mgc.ac.cn"
        );
        assertSame(s, "zover/chiroptera_viruses_35237.json");
    }

    @Test
    public void retrieveTickVirusData() throws IOException {
        String s = DatasetImporterForZOVER.getVirusData(
                DatasetImporterForZOVER.ZOVER_TICK,
                35237L,
                getResourceService(),
                "http://www.mgc.ac.cn"
        );
        assertSame(s, "zover/tick_viruses_35237.json");
    }

    @Test
    public void retrieveRodentVirusData() throws IOException {
        String s = DatasetImporterForZOVER.getVirusData(
                DatasetImporterForZOVER.ZOVER_RODENT,
                35237L,
                getResourceService(),
                "http://www.mgc.ac.cn"
        );
        assertSame(s, "zover/rodent_viruses_35237.json");
    }

    private void assertSame(String content, String resourceName) throws IOException {
        InputStream resource = getClass().getResourceAsStream(resourceName);
        String expectedContent = IOUtils.toString(resource, StandardCharsets.UTF_8);
        assertThat(content, Is.is(expectedContent));
    }

}