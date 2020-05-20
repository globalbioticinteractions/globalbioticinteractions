package org.eol.globi.service;

import org.apache.commons.io.FileUtils;
import org.globalbioticinteractions.doi.DOI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class DOIResolverCacheTest {

    private DOIResolverCache doiResolverCache;
    private File cacheDir;

    @Before
    public void init() {
        doiResolverCache = new DOIResolverCache();
        cacheDir = new File("target/doiResolverTest" + UUID.randomUUID());
        cacheDir.deleteOnExit();
        doiResolverCache.setCacheDir(cacheDir);
    }

    @After
    public void cleanup() {
        FileUtils.deleteQuietly(cacheDir);
    }

    @Test
    public void initCache() throws IOException, PropertyEnricherException {
        String bla = "doi\tcitation" +
                "\ndoi:10.some/doi\tsome citation" +
                "\ndoi:10.some/other/doi\tsome other citation";
        Reader reader = new StringReader(bla);

        doiResolverCache.init(reader);
        Map<String, DOI> doiForReference = doiResolverCache.resolveDoiFor(Arrays.asList("some citation", "some other citation"));
        assertThat(doiForReference.get("some other citation").toString(), is("10.some/other/doi"));
        assertThat(doiForReference.get("some citation").toString(), is("10.some/doi"));
    }

    @Test
    public void initCache2() throws IOException, PropertyEnricherException {
        String bla = "doi\tcitation\n" +
                "10.some/A\tcitationA\n" +
                "10.some/B\tcitationB";
        Reader reader = new StringReader(bla);

        doiResolverCache.init(reader);
        Map<String, DOI> doiForReference = doiResolverCache.resolveDoiFor(Collections.singletonList("citationA"));
        assertThat(doiForReference.get("citationA").toString(), is("10.some/A"));
    }

    @Test
    public void initCache3() throws IOException, PropertyEnricherException {
        String bla = "doi\tcitation\n" +
                "10.some/A\tcitationA\n" +
                "\tcitationX\n" +
                "\t\n" +
                "10.some/B\tcitationB";
        Reader reader = new StringReader(bla);


        doiResolverCache.init(reader);
        Map<String, DOI> doiForReference = doiResolverCache.resolveDoiFor(Collections.singletonList("citationA"));
        assertThat(doiForReference.get("citationA"), is(new DOI("some", "A")));
    }

    @Test
    public void initCache4() throws IOException {
        DOIResolverCache doiResolverCache = new DOIResolverCache("/org/eol/globi/tool/citations.tsv.gz");
        doiResolverCache.setCacheDir(cacheDir);

        String ref1 = "Kalka, Margareta, and Elisabeth K. V. Kalko. Gleaning Bats as Underestimated Predators of Herbivorous Insects: Diet of Micronycteris Microtis (Phyllostomidae) in Panama. Journal of Tropical Ecology 1 (2006): 1-10.";
        Map<String, DOI> doiForReference = doiResolverCache.resolveDoiFor(Collections.singletonList(ref1));
        assertThat(doiForReference.get(ref1), is(new DOI("1017", "S0266467405002920")));
    }

    @Test
    public void initCacheNoTabs() throws IOException, PropertyEnricherException {
        String bla = "doi citation\n" +
                "10.some/A citationA\n" +
                "10.some/B citationB";
        Reader reader = new StringReader(bla);

        doiResolverCache.init(reader);
        Map<String, DOI> doiForReference = doiResolverCache.resolveDoiFor(Collections.singletonList("citationA"));
        assertThat(doiForReference.get("citationA"), is(not(new DOI("some", "A"))));
    }

}