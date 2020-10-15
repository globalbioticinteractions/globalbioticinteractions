package org.globalbioticinteractions.util;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.HttpUtil;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.eol.globi.data.TestUtil.getResourceServiceTest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class OpenBiodivUtilTest {

    @Test
    public void retrieveTaxonFamily() throws IOException {
        Taxon taxon = OpenBiodivUtil.retrieveTaxonHierarchyById("4B689A17-2541-4F5F-A896-6F0C2EEA3FB4",
                getSparqlClient());
        assertThat(taxon.getName(), is("Acanthaceae"));
        assertThat(taxon.getRank(), is("family"));
        assertThat(taxon.getExternalId(), is("http://openbiodiv.net/4B689A17-2541-4F5F-A896-6F0C2EEA3FB4"));
        assertThat(taxon.getPath(), is("Plantae | Tracheophyta | Magnoliopsida | Lamiales | Acanthaceae"));
        assertThat(taxon.getPathNames(), is("kingdom | phylum | class | order | family"));
    }

    @Test
    public void retrieveTaxonSpecies() throws IOException {
        Taxon taxon = OpenBiodivUtil
                .retrieveTaxonHierarchyById("6A54156A-BE5C-44D7-A9E3-3902DA4CCFAC",
                getSparqlClient());
        assertThat(taxon.getName(), is("Copidothrips octarticulatus"));
        assertThat(taxon.getRank(), is(nullValue()));
        assertThat(taxon.getExternalId(), is("http://openbiodiv.net/6A54156A-BE5C-44D7-A9E3-3902DA4CCFAC"));
        assertThat(taxon.getPath(), is("Copidothrips octarticulatus"));
        assertThat(taxon.getPathNames(), is(""));
    }

    @Test
    public void retrieveTaxonSpecies2() throws IOException {
        Taxon taxon = OpenBiodivUtil.retrieveTaxonHierarchyById("22A7F215-829B-458A-AEBB-39FFEA6D4A91",
                getSparqlClient());
        assertThat(taxon.getName(), is("Bolacothrips striatopennatus"));
        assertThat(taxon.getRank(), is("species"));
        assertThat(taxon.getExternalId(), is("http://openbiodiv.net/22A7F215-829B-458A-AEBB-39FFEA6D4A91"));
        assertThat(taxon.getPath(), is("Animalia | Arthropoda | Insecta | Thysanoptera | Thripidae | Bolacothrips | Bolacothrips striatopennatus"));
        assertThat(taxon.getPathNames(), is("kingdom | phylum | class | order | family | genus | species"));
    }

    public SparqlClientImpl getSparqlClient() {
        return new SparqlClientImpl(getResourceServiceTest(), PropertyAndValueDictionary.SPARQL_ENDPOINT_OPEN_BIODIV);
    }

    public static ResourceService getResourceServiceTest() {
        return resourceName -> {
            HttpGet req = new HttpGet(resourceName);
            String csvString = HttpUtil.executeAndRelease(req, HttpUtil.getFailFastHttpClient());
            return IOUtils.toInputStream(csvString, StandardCharsets.UTF_8);
        };
    }



}