package org.eol.globi.util;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class NodeUtilTest {

    @Test
    public void truncatedTaxonName() {
        assertThat(NodeUtil.truncateTaxonName("one two three"), is("one two"));
        assertThat(NodeUtil.truncateTaxonName("one two three four"), is("one two"));
        assertThat(NodeUtil.truncateTaxonName("one two"), is("one two"));
        assertThat(NodeUtil.truncateTaxonName("one"), is("one"));
    }

    @Test
    public void doNotTruncateVirusName() {
        assertThat(NodeUtil.truncateTaxonName("whatever something virus"), is("whatever something virus"));
    }
}
