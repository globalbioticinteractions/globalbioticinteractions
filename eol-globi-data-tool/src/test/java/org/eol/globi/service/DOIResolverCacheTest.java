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
        assertThat(doiForReference.get("some other citation"), is("doi:some/other/doi"));
        assertThat(doiForReference.get("some citation"), is("doi:some/doi"));
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
        assertThat(doiForReference.get("citationA"), is("doi:some/A"));
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
        assertThat(doiForReference.get("citationA"), is(not("doi:some/A")));
    }

}