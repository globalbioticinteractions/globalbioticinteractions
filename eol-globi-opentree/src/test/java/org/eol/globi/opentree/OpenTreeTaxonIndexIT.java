package org.eol.globi.opentree;

import org.junit.Test;

import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
public class OpenTreeTaxonIndexIT {

    @Test
    public void buildMap() {
        OpenTreeTaxonIndex openTreeTaxonIndex = new OpenTreeTaxonIndex(getClass().getResource("/ott/taxonomy.tsv"));
        assertThat(openTreeTaxonIndex.findOpenTreeTaxonIdFor("ncbi:518"), is(898843L));
        assertThat(openTreeTaxonIndex.findOpenTreeTaxonIdFor("gbif:3219832"), is(898843L));
        assertThat(openTreeTaxonIndex.findOpenTreeTaxonIdFor("irmng:11068172"), is(898843L));
        assertThat(new TreeSet<>(openTreeTaxonIndex.findUniqueExternalIdPrefixes()), hasItems("study713:", "https:", "gbif:", "if:", "silva:", "irmng:", "ncbi:", "h2007:"));
        openTreeTaxonIndex.destroy();
    }
}
