package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.eol.globi.util.ResourceUtil;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DatasetImporterForZOVERTest {


    @Test
    public void retrieveTicks() throws IOException {
        assertSame(getContent("tick"), "zover/tick.json");
    }

    @Test
    public void retrieveBats() throws IOException {
        assertSame(getContent("chiroptera"), "zover/chiroptera.json");
    }

    @Test
    public void retrieveRodent() throws IOException {
        assertSame(getContent("rodent"), "zover/rodent.json");
    }

    @Test
    public void retrieveMosquito() throws IOException {
        assertSame(getContent("mosquito"), "zover/mosquito.json");
    }

    @Test
    public void retrieveMosquitoVirusData() throws IOException {
        String s = getVirusData("mosquito", 35237L);
        assertSame(s, "zover/mosquito_viruses_35237.json");
    }

    @Test
    public void retrieveBatVirusData() throws IOException {
        String s = getVirusData("chiroptera", 35237L);
        assertSame(s, "zover/chiroptera_viruses_35237.json");
    }

    @Test
    public void retrieveTickVirusData() throws IOException {
        String s = getVirusData("tick", 35237L);
        assertSame(s, "zover/tick_viruses_35237.json");
    }

    @Test
    public void retrieveRodentVirusData() throws IOException {
        String s = getVirusData("rodent", 35237L);
        assertSame(s, "zover/rodent_viruses_35237.json");
    }

    public String getVirusData(String host, Long virusId) throws IOException {
        return getData(host, "/ZOVER/json/", "_viruses_" + virusId + ".json");
    }

    private void assertSame(String content, String resourceName) throws IOException {
        InputStream resource = getClass().getResourceAsStream(resourceName);

        String expectedContent = IOUtils.toString(resource, StandardCharsets.UTF_8);

        assertThat(content, Is.is(expectedContent));
    }

    public String getContent(String host) throws IOException {
        String pathPrefix = "/cgi-bin/ZOVER/lineage2json1.pl?type=viruses&host=";
        String pathSuffix = "";
        String s = getData(host, pathPrefix, pathSuffix);

        return s;
    }

    public String getData(String host, String pathPrefix, String pathSuffix) throws IOException {
        String str = "http://www.mgc.ac.cn" + pathPrefix + host + pathSuffix;
        InputStream inputStream = ResourceUtil.asInputStream(
                URI.create(str), is -> is);
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }


}