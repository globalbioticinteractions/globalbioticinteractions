package org.eol.globi.data.taxon;

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
        assertThat(normalizer.normalize("Aegathoa oculata sp."), is("Aegathoa oculata"));
        assertThat(normalizer.normalize("Aegathoa oculata spp"), is("Aegathoa oculata"));
        assertThat(normalizer.normalize("Aegathoa oculata spp."), is("Aegathoa oculata"));
        assertThat(normalizer.normalize("Aegathoa oculata spp. Aegathoa oculata spp. Aegathoa oculata spp."), is("Aegathoa oculata"));
        assertThat(normalizer.normalize("Taraxacum leptaleum"), is("Taraxacum leptaleum"));
        assertThat(normalizer.normalize("NA"), is("NomenNescio"));
        assertThat(normalizer.normalize("NE"), is("NomenNescio"));
        assertThat(normalizer.normalize("'Loranthus'"), is("Loranthus"));
        assertThat(normalizer.normalize("Bacteriastrum spp.          Bacteriastrum spp.          Bacteriastrum spp.          Bacteriastrum spp."), is("Bacteriastrum"));
        assertThat(normalizer.normalize("Acrididae spp. "), is("Acrididae"));
        assertThat(normalizer.normalize("<quotes>Chenopodiaceae<quotes>"), is("Chenopodiaceae"));

        assertThat(normalizer.normalize("Mycosphaerella filipendulae-denudatae"), is("Mycosphaerella filipendulae-denudatae"));

        assertThat(normalizer.normalize("Puccinia dioicae var. dioicae"), is("Puccinia dioicae var. dioicae"));
        assertThat(normalizer.normalize("Puccinia dioicae var dioicae"), is("Puccinia dioicae var. dioicae"));
        assertThat(normalizer.normalize("Puccinia dioicae variety dioicae"), is("Puccinia dioicae var. dioicae"));
        assertThat(normalizer.normalize("Puccinia dioicae varietas dioicae"), is("Puccinia dioicae var. dioicae"));

        assertThat(normalizer.normalize("Archips podana/operana"), is("Archips"));

        assertThat(normalizer.normalize("Leptochela cf bermudensis"), is("Leptochela"));

        assertThat(normalizer.normalize("S enflata"), is("Sagitta enflata"));

        assertThat(normalizer.normalize("Aneugmenus fürstenbergensis"), is("Aneugmenus fuerstenbergensis"));
        assertThat(normalizer.normalize("Xanthorhoë"), is("Xanthorhoe"));

        // Malcolm Storey technique to distinguish duplicate genus names in taxonomies.
        assertThat(normalizer.normalize("Ammophila (Bot.)"), is("Ammophila (Bot.)"));
        assertThat(normalizer.normalize("Ammophila (Zool.)"), is("Ammophila (Zool.)"));
        assertThat(normalizer.normalize("Ammophila (zoo)"), is("Ammophila (zoo)"));
        assertThat(normalizer.normalize("Ammophila (bot)"), is("Ammophila (bot)"));
        assertThat(normalizer.normalize("Ammophila (bot.)"), is("Ammophila (bot.)"));
        assertThat(normalizer.normalize("Ammophila (Bot)"), is("Ammophila (Bot)"));
        assertThat(normalizer.normalize("Ammophila (blah)"), is("Ammophila"));

    }

    @Test
    public void taxonNameTooShort() {
        TaxonNameNormalizer normalizer = new TaxonNameNormalizer();
        assertThat(normalizer.normalize("G"), is("NomenNescio"));
        assertThat(normalizer.normalize("H"), is("NomenNescio"));
        assertThat(normalizer.normalize("HH"), is("HH"));
    }
}
