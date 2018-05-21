package org.eol.globi.service;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class DOIResolverCacheTest {

    @Test
    public void initCache() throws IOException, PropertyEnricherException {
        String bla = "doi\tcitation" +
                "\ndoi:some/doi\tsome citation" +
                "\ndoi:some/other/doi\tsome other citation";
        Reader reader = new StringReader(bla);


        DOIResolverCache doiResolverCache = new DOIResolverCache();

        doiResolverCache.init(reader);
        Map<String, String> doiForReference = doiResolverCache.resolveDoiFor(Arrays.asList("some citation", "some other citation"));
        assertThat(doiForReference.get("some other citation"), is("https://doi.org/some/other/doi"));
        assertThat(doiForReference.get("some citation"), is("https://doi.org/some/doi"));
    }

    @Test
    public void initCache2() throws IOException, PropertyEnricherException {
        String bla = "doi\tcitation\n" +
                "doi:some/A\tcitationA\n" +
                "doi:some/B\tcitationB";
        Reader reader = new StringReader(bla);


        DOIResolverCache doiResolverCache = new DOIResolverCache();

        doiResolverCache.init(reader);
        Map<String, String> doiForReference = doiResolverCache.resolveDoiFor(Arrays.asList("citationA"));
        assertThat(doiForReference.get("citationA"), is("https://doi.org/some/A"));
    }

    @Test
    public void initCache3() throws IOException, PropertyEnricherException {
        String bla = "doi\tcitation\n" +
                "doi:some/A\tcitationA\n" +
                "\tcitationX\n" +
                "\t\n" +
                "doi:some/B\tcitationB";
        Reader reader = new StringReader(bla);


        DOIResolverCache doiResolverCache = new DOIResolverCache();

        doiResolverCache.init(reader);
        Map<String, String> doiForReference = doiResolverCache.resolveDoiFor(Arrays.asList("citationA"));
        assertThat(doiForReference.get("citationA"), is("https://doi.org/some/A"));
    }

    @Test
    public void initCache4() throws IOException, PropertyEnricherException {
        DOIResolverCache doiResolverCache = new DOIResolverCache("/org/eol/globi/tool/citations.tsv.gz");

        String ref1 = "Kalka, Margareta, and Elisabeth K. V. Kalko. Gleaning Bats as Underestimated Predators of Herbivorous Insects: Diet of Micronycteris Microtis (Phyllostomidae) in Panama. Journal of Tropical Ecology 1 (2006): 1-10.";
        Map<String, String> doiForReference = doiResolverCache.resolveDoiFor(Arrays.asList(ref1));
        assertThat(doiForReference.get(ref1), is("https://doi.org/10.1017/S0266467405002920"));
    }

    @Test
    public void initCacheNoTabs() throws IOException, PropertyEnricherException {
        String bla = "doi citation\n" +
                "doi:some/A citationA\n" +
                "doi:some/B citationB";
        Reader reader = new StringReader(bla);

        DOIResolverCache doiResolverCache = new DOIResolverCache();

        doiResolverCache.init(reader);
        Map<String, String> doiForReference = doiResolverCache.resolveDoiFor(Collections.singletonList("citationA"));
        assertThat(doiForReference.get("citationA"), is(not("https://doi.org/some/A")));
    }

}