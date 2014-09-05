package org.eol.globi.opentree;

import org.junit.Test;

import java.io.IOException;
import java.util.TreeSet;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;

public class OpenTreeTaxonIndexIT {

    @Test
    public void buildMap() throws IOException {
        OpenTreeTaxonIndex openTreeTaxonIndex = new OpenTreeTaxonIndex(getClass().getResource("/ott/taxonomy.tsv"));
        assertThat(openTreeTaxonIndex.findOpenTreeTaxonIdFor("ncbi:518"), is(898843L));
        assertThat(openTreeTaxonIndex.findOpenTreeTaxonIdFor("gbif:3219832"), is(898843L));
        assertThat(openTreeTaxonIndex.findOpenTreeTaxonIdFor("irmng:11068172"), is(898843L));
        assertThat(new TreeSet<String>(openTreeTaxonIndex.findUniqueExternalIdPrefixes()), hasItems("study713:", "https:", "gbif:", "if:", "silva:", "irmng:", "ncbi:", "h2007:"));
        openTreeTaxonIndex.destroy();
    }
}
