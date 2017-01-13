package org.eol.globi.service;

import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class DOIResolverCacheTest {

    @Test
    public void initCache() throws IOException, PropertyEnricherException {
        String bla = "citation\tdoi" +
                "\nsome citation\tdoi:some/doi" +
                "\nsome other citation\tdoi:some/other/doi";
        Reader reader = new StringReader(bla);


        DOIResolverCache doiResolverCache = new DOIResolverCache();

        doiResolverCache.init(reader);
        Map<String, String> doiForReference = doiResolverCache.resolveDoiFor(Arrays.asList("some citation", "some other citation"));
        assertThat(doiForReference.get("some other citation"), is("doi:some/other/doi"));
        assertThat(doiForReference.get("some citation"), is("doi:some/doi"));
    }

    @Test
    public void initCache2() throws IOException, PropertyEnricherException {
        String bla = "citation\tdoi\n" +
                "citationA\tdoi:some/A\n" +
                "citationB\tdoi:some/B";
        Reader reader = new StringReader(bla);


        DOIResolverCache doiResolverCache = new DOIResolverCache();

        doiResolverCache.init(reader);
        Map<String, String> doiForReference = doiResolverCache.resolveDoiFor(Arrays.asList("citationA"));
        assertThat(doiForReference.get("citationA"), is("doi:some/A"));
    }

    @Test
    public void initCacheNoTabs() throws IOException, PropertyEnricherException {
        String bla = "citation doi\n" +
                "citationA doi:some/A\n" +
                "citationB doi:some/B";
        Reader reader = new StringReader(bla);

        DOIResolverCache doiResolverCache = new DOIResolverCache();

        doiResolverCache.init(reader);
        Map<String, String> doiForReference = doiResolverCache.resolveDoiFor(Collections.singletonList("citationA"));
        assertThat(doiForReference.get("citationA"), is(not("doi:some/A")));
    }

    @Test
    public void initCacheResource() throws IOException, PropertyEnricherException {
        String doiCacheResource = "doi-cache-test.tsv";
        assertThat(getClass().getResourceAsStream(doiCacheResource), is(notNullValue()));
        DOIResolverCache doiResolverCache = new DOIResolverCache(doiCacheResource);

        Map<String, String> doiForReference = doiResolverCache.resolveDoiFor(Arrays.asList("citationA", "citationB"));
        assertThat(doiForReference.get("citationA"), is("doi:some/A"));
        assertThat(doiForReference.get("citationB"), is("doi:some/B"));
    }

}