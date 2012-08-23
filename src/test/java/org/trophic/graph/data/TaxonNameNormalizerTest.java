package org.trophic.graph.data;

import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TaxonNameNormalizerTest {

    @Test
    public void checkNotSame() {
        Map<String,String> predatorMap = TaxonNameNormalizer.SAME_AS_MAP;
        for (Map.Entry<String, String> entry : predatorMap.entrySet()) {
            if (!entry.getKey().equals(entry.getValue())) {
                System.out.println("put(\"" + entry.getKey() + "\", \"" + entry.getValue() + "\");");
            }
            assertThat(entry.getValue(), is(not(entry.getKey())));
        }
    }

    @Test
    public void cleanName() {
        TaxonNameNormalizer normalizer = new TaxonNameNormalizer();
        assertThat(normalizer.normalize("Blbua blas "), is("Blbua blas"));
        assertThat(normalizer.normalize("Blbua  blas  "), is("Blbua blas"));
        assertThat(normalizer.normalize("Blbua  blas (akjhaskdjhf)"), is("Blbua blas"));
        assertThat(normalizer.normalize("  Blbua  blas (akjhaskdjhf)"), is("Blbua blas"));
        assertThat(normalizer.normalize("Aegathoa oculata¬†"), is("Aegathoa oculata"));
        assertThat(normalizer.normalize("Aegathoa oculata*"), is("Aegathoa oculata"));
    }
}
