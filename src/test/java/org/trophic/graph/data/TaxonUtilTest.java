package org.trophic.graph.data;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TaxonUtilTest {

    @Test
    public void cleanName() {
        assertThat(TaxonUtil.clean("Blbua blas "), is("Blbua blas"));
        assertThat(TaxonUtil.clean("Blbua  blas  "), is("Blbua blas"));
        assertThat(TaxonUtil.clean("Blbua  blas (akjhaskdjhf)"), is("Blbua blas"));
        assertThat(TaxonUtil.clean("  Blbua  blas (akjhaskdjhf)"), is("Blbua blas"));
    }
}
