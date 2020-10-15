package org.globalbioticinteractions.util;

import org.eol.globi.domain.Taxon;
import org.junit.Test;

import java.io.IOException;

import static org.eol.globi.data.TestUtil.getResourceServiceTest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class OpenBiodivUtilTest {

    @Test
    public void retrieveTaxonFamily() throws IOException {
        Taxon taxon = OpenBiodivUtil.retrieveTaxonHierarchyById("4B689A17-2541-4F5F-A896-6F0C2EEA3FB4", new OpenBiodivClient(getResourceServiceTest()));
        assertThat(taxon.getName(), is("Acanthaceae"));
        assertThat(taxon.getRank(), is("family"));
        assertThat(taxon.getExternalId(), is("http://openbiodiv.net/4B689A17-2541-4F5F-A896-6F0C2EEA3FB4"));
        assertThat(taxon.getPath(), is("Plantae | Tracheophyta | Magnoliopsida | Lamiales | Acanthaceae"));
        assertThat(taxon.getPathNames(), is("kingdom | phylum | class | order | family"));
    }

    @Test
    public void retrieveTaxonSpecies() throws IOException {
        Taxon taxon = OpenBiodivUtil.retrieveTaxonHierarchyById("6A54156A-BE5C-44D7-A9E3-3902DA4CCFAC", new OpenBiodivClient(getResourceServiceTest()));
        assertThat(taxon.getName(), is("Copidothrips octarticulatus"));
        assertThat(taxon.getRank(), is(nullValue()));
        assertThat(taxon.getExternalId(), is("http://openbiodiv.net/6A54156A-BE5C-44D7-A9E3-3902DA4CCFAC"));
        assertThat(taxon.getPath(), is("Copidothrips octarticulatus"));
        assertThat(taxon.getPathNames(), is(""));
    }

    @Test
    public void retrieveTaxonSpecies2() throws IOException {
        Taxon taxon = OpenBiodivUtil.retrieveTaxonHierarchyById("22A7F215-829B-458A-AEBB-39FFEA6D4A91", new OpenBiodivClient(getResourceServiceTest()));
        assertThat(taxon.getName(), is("Bolacothrips striatopennatus"));
        assertThat(taxon.getRank(), is("species"));
        assertThat(taxon.getExternalId(), is("http://openbiodiv.net/22A7F215-829B-458A-AEBB-39FFEA6D4A91"));
        assertThat(taxon.getPath(), is("Animalia | Arthropoda | Insecta | Thysanoptera | Thripidae | Bolacothrips | Bolacothrips striatopennatus"));
        assertThat(taxon.getPathNames(), is("kingdom | phylum | class | order | family | genus | species"));
    }


}