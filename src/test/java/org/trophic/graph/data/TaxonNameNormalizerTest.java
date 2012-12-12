package org.trophic.graph.data;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TaxonNameNormalizerTest {

    @Test
    public void cleanName() {
        TaxonNameNormalizer normalizer = new TaxonNameNormalizer();
        assertThat(normalizer.normalize("Blbua blas "), is("Blbua blas"));
        assertThat(normalizer.normalize("Blbua  blas  "), is("Blbua blas"));
        assertThat(normalizer.normalize("Blbua  blas (akjhaskdjhf)"), is("Blbua blas"));
        assertThat(normalizer.normalize("  Blbua  blas (akjhaskdjhf)"), is("Blbua blas"));
        assertThat(normalizer.normalize("Aegathoa oculata¬†"), is("Aegathoa oculata"));
        assertThat(normalizer.normalize("Aegathoa oculata*"), is("Aegathoa oculata"));
        assertThat(normalizer.normalize("Aegathoa oculata?"), is("Aegathoa oculata"));
        assertThat(normalizer.normalize("Aegathoa oculata sp."), is("Aegathoa oculata sp."));
        assertThat(normalizer.normalize("Taraxacum leptaleum"), is("Taraxacum leptaleum"));
    }
}
