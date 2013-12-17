package org.eol.globi.data.taxon;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TaxonNameCorrectorTest {

    @Test
    public void cleanName() {
        CorrectionService normalizer = new TaxonNameCorrector();
        assertThat(normalizer.correct("Blbua blas "), is("Blbua blas"));
        assertThat(normalizer.correct("Blbua  blas  "), is("Blbua blas"));
        assertThat(normalizer.correct("Blbua  blas (akjhaskdjhf)"), is("Blbua blas"));
        assertThat(normalizer.correct("  Blbua  blas (akjhaskdjhf)"), is("Blbua blas"));
        assertThat(normalizer.correct("Aegathoa oculata¬†"), is("Aegathoa oculata"));
        assertThat(normalizer.correct("Aegathoa oculata*"), is("Aegathoa oculata"));
        assertThat(normalizer.correct("Aegathoa oculata?"), is("Aegathoa oculata"));
        assertThat(normalizer.correct("Aegathoa oculata sp."), is("Aegathoa oculata"));
        assertThat(normalizer.correct("Aegathoa oculata spp"), is("Aegathoa oculata"));
        assertThat(normalizer.correct("Aegathoa oculata spp."), is("Aegathoa oculata"));
        assertThat(normalizer.correct("Aegathoa oculata spp. Aegathoa oculata spp. Aegathoa oculata spp."), is("Aegathoa oculata"));
        assertThat(normalizer.correct("Taraxacum leptaleum"), is("Taraxacum leptaleum"));
        assertThat(normalizer.correct(""), is("no name"));
        assertThat(normalizer.correct("a"), is("no name"));
        assertThat(normalizer.correct("NA"), is("no name"));
        assertThat(normalizer.correct("NE"), is("no name"));
        assertThat(normalizer.correct("'Loranthus'"), is("Loranthus"));
        assertThat(normalizer.correct("Bacteriastrum spp.          Bacteriastrum spp.          Bacteriastrum spp.          Bacteriastrum spp."), is("Bacteriastrum"));
        assertThat(normalizer.correct("Acrididae spp. "), is("Acrididae"));
        assertThat(normalizer.correct("<quotes>Chenopodiaceae<quotes>"), is("Chenopodiaceae"));

        assertThat(normalizer.correct("Mycosphaerella filipendulae-denudatae"), is("Mycosphaerella filipendulae-denudatae"));

        assertThat(normalizer.correct("Puccinia dioicae var. dioicae"), is("Puccinia dioicae var. dioicae"));
        assertThat(normalizer.correct("Puccinia dioicae var dioicae"), is("Puccinia dioicae var. dioicae"));
        assertThat(normalizer.correct("Puccinia dioicae variety dioicae"), is("Puccinia dioicae var. dioicae"));
        assertThat(normalizer.correct("Puccinia dioicae varietas dioicae"), is("Puccinia dioicae var. dioicae"));

        assertThat(normalizer.correct("Aegathoa oculata ssp. fred"), is("Aegathoa oculata ssp. fred"));
        assertThat(normalizer.correct("Aegathoa oculata ssp fred"), is("Aegathoa oculata ssp. fred"));
        assertThat(normalizer.correct("Aegathoa oculata subsp fred"), is("Aegathoa oculata ssp. fred"));
        assertThat(normalizer.correct("Aegathoa oculata subsp. fred"), is("Aegathoa oculata ssp. fred"));
        assertThat(normalizer.correct("Aegathoa oculata subspecies fred"), is("Aegathoa oculata ssp. fred"));

        //assertThat(normalizer.normalize("Armeria 'Bees Ruby'"), is("Armeria 'Bees Ruby'"));

        assertThat(normalizer.correct("Rubus fruticosus agg."), is("Rubus fruticosus agg."));

        assertThat(normalizer.correct("Limonia (Dicranomyia) chorea"), is("Limonia chorea"));

        assertThat(normalizer.correct("Archips podana/operana"), is("Archips"));

        assertThat(normalizer.correct("Leptochela cf bermudensis"), is("Leptochela"));

        assertThat(normalizer.correct("S enflata"), is("Sagitta enflata"));

        assertThat(normalizer.correct("Aneugmenus fürstenbergensis"), is("Aneugmenus fuerstenbergensis"));
        assertThat(normalizer.correct("Xanthorhoë"), is("Xanthorhoe"));

        // Malcolm Storey technique to distinguish duplicate genus names in taxonomies.
        assertThat(normalizer.correct("Ammophila (Bot.)"), is("Ammophila (Bot.)"));
        assertThat(normalizer.correct("Ammophila (Zool.)"), is("Ammophila (Zool.)"));
        assertThat(normalizer.correct("Ammophila (zoo)"), is("Ammophila (zoo)"));
        assertThat(normalizer.correct("Ammophila (bot)"), is("Ammophila (bot)"));
        assertThat(normalizer.correct("Ammophila (bot.)"), is("Ammophila (bot.)"));
        assertThat(normalizer.correct("Ammophila (Bot)"), is("Ammophila (Bot)"));
        assertThat(normalizer.correct("Ammophila (blah)"), is("Ammophila"));
        assertThat(normalizer.correct("Cal sapidus"), is("Callinectes sapidus"));

        assertThat(normalizer.correct("Bivalvia Genus A"), is("Bivalvia"));


    }

    /**
     * In scientific names of hybrids (e.g. Erysimum decumbens x perofskianum, http://eol.org/pages/5145889),
     *  the \u00D7 or × (multiply) symbol should be used. However, most data sources simply use lower case "x",
     *  and don't support the × symbol is their name matching methods.
     */

    @Test
    public void replaceMultiplyOrXByLowerCaseX() {
        CorrectionService normalizer = new TaxonNameCorrector();
        assertThat(normalizer.correct("Genus species1 x species2"), is("Genus species1 x species2"));
        assertThat(normalizer.correct("Genus species1 \u00D7 species2"), is("Genus species1 x species2"));
        assertThat(normalizer.correct("Genus species1 × species2"), is("Genus species1 x species2"));
        assertThat(normalizer.correct("Genus species1 ×hybridName"), is("Genus species1 xhybridName"));
        assertThat(normalizer.correct("Genus species1 X species2"), is("Genus species1 x species2"));
        assertThat(normalizer.correct("Genus species1 XhybridName"), is("Genus species1 xhybridName"));
    }

    @Test
    public void taxonNameTooShort() {
        CorrectionService normalizer = new TaxonNameCorrector();
        assertThat(normalizer.correct("G"), is("no name"));
        assertThat(normalizer.correct("H"), is("no name"));
        assertThat(normalizer.correct("HH"), is("HH"));
    }

    @Test
    public void taxonNameUSKI() {
        CorrectionService normalizer = new TaxonNameCorrector();
        assertThat(normalizer.correct("Scypha raphanus"), is("Sycon raphanus"));
    }
}
