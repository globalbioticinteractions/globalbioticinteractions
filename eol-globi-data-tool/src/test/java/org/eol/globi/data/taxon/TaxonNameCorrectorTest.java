package org.eol.globi.data.taxon;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TaxonNameCorrectorTest {

    private final static CorrectionService CORRECTOR = new TaxonNameCorrector();

    @Test
    public void cleanName() {
        assertThat(CORRECTOR.correct("Blbua blas "), is("Blbua blas"));
        assertThat(CORRECTOR.correct("Blbua  blas  "), is("Blbua blas"));
        assertThat(CORRECTOR.correct("Blbua  blas (akjhaskdjhf)"), is("Blbua blas"));
        assertThat(CORRECTOR.correct("  Blbua  blas (akjhaskdjhf)"), is("Blbua blas"));
        assertThat(CORRECTOR.correct("Aegathoa oculata¬†"), is("Aegathoa oculata"));
        assertThat(CORRECTOR.correct("Aegathoa oculata*"), is("Aegathoa oculata"));
        assertThat(CORRECTOR.correct("Aegathoa oculata?"), is("Aegathoa oculata"));
        assertThat(CORRECTOR.correct("Aegathoa oculata sp."), is("Aegathoa oculata"));
        assertThat(CORRECTOR.correct("Aegathoa oculata spp"), is("Aegathoa oculata"));
        assertThat(CORRECTOR.correct("Aegathoa oculata spp."), is("Aegathoa oculata"));
        assertThat(CORRECTOR.correct("Aegathoa oculata spp. Aegathoa oculata spp. Aegathoa oculata spp."), is("Aegathoa oculata"));
        assertThat(CORRECTOR.correct("Taraxacum leptaleum"), is("Taraxacum leptaleum"));
        assertThat(CORRECTOR.correct(""), is("no name"));
        assertThat(CORRECTOR.correct("a"), is("no name"));
        assertThat(CORRECTOR.correct("NA"), is("no name"));
        assertThat(CORRECTOR.correct("NE"), is("no name"));
        assertThat(CORRECTOR.correct("'Loranthus'"), is("Loranthus"));
        assertThat(CORRECTOR.correct("Bacteriastrum spp.          Bacteriastrum spp.          Bacteriastrum spp.          Bacteriastrum spp."), is("Bacteriastrum"));
        assertThat(CORRECTOR.correct("Acrididae spp. "), is("Acrididae"));
        assertThat(CORRECTOR.correct("<quotes>Chenopodiaceae<quotes>"), is("Chenopodiaceae"));

        assertThat(CORRECTOR.correct("Mycosphaerella filipendulae-denudatae"), is("Mycosphaerella filipendulae-denudatae"));

        assertThat(CORRECTOR.correct("Puccinia dioicae var. dioicae"), is("Puccinia dioicae var. dioicae"));
        assertThat(CORRECTOR.correct("Puccinia dioicae var dioicae"), is("Puccinia dioicae var. dioicae"));
        assertThat(CORRECTOR.correct("Puccinia dioicae variety dioicae"), is("Puccinia dioicae var. dioicae"));
        assertThat(CORRECTOR.correct("Puccinia dioicae varietas dioicae"), is("Puccinia dioicae var. dioicae"));

        assertThat(CORRECTOR.correct("Aegathoa oculata ssp. fred"), is("Aegathoa oculata ssp. fred"));
        assertThat(CORRECTOR.correct("Aegathoa oculata ssp fred"), is("Aegathoa oculata ssp. fred"));
        assertThat(CORRECTOR.correct("Aegathoa oculata subsp fred"), is("Aegathoa oculata ssp. fred"));
        assertThat(CORRECTOR.correct("Aegathoa oculata subsp. fred"), is("Aegathoa oculata ssp. fred"));
        assertThat(CORRECTOR.correct("Aegathoa oculata subspecies fred"), is("Aegathoa oculata ssp. fred"));

        //assertThat(normalizer.normalize("Armeria 'Bees Ruby'"), is("Armeria 'Bees Ruby'"));

        assertThat(CORRECTOR.correct("Rubus fruticosus agg."), is("Rubus fruticosus agg."));

        assertThat(CORRECTOR.correct("Limonia (Dicranomyia) chorea"), is("Limonia chorea"));

        assertThat(CORRECTOR.correct("Archips podana/operana"), is("Archips"));

        assertThat(CORRECTOR.correct("Leptochela cf bermudensis"), is("Leptochela"));

        assertThat(CORRECTOR.correct("S enflata"), is("Sagitta enflata"));

        assertThat(CORRECTOR.correct("Aneugmenus fürstenbergensis"), is("Aneugmenus fuerstenbergensis"));
        assertThat(CORRECTOR.correct("Xanthorhoë"), is("Xanthorhoe"));

        // Malcolm Storey technique to distinguish duplicate genus names in taxonomies.
        assertThat(CORRECTOR.correct("Ammophila (Bot.)"), is("Ammophila (Bot.)"));
        assertThat(CORRECTOR.correct("Ammophila (Zool.)"), is("Ammophila (Zool.)"));
        assertThat(CORRECTOR.correct("Ammophila (zoo)"), is("Ammophila (zoo)"));
        assertThat(CORRECTOR.correct("Ammophila (bot)"), is("Ammophila (bot)"));
        assertThat(CORRECTOR.correct("Ammophila (bot.)"), is("Ammophila (bot.)"));
        assertThat(CORRECTOR.correct("Ammophila (Bot)"), is("Ammophila (Bot)"));
        assertThat(CORRECTOR.correct("Ammophila (blah)"), is("Ammophila"));
        assertThat(CORRECTOR.correct("Cal sapidus"), is("Callinectes sapidus"));

        assertThat(CORRECTOR.correct("Bivalvia Genus A"), is("Bivalvia"));
        assertThat(CORRECTOR.correct("EOL:123"), is("EOL 123"));


    }

    /**
     * In scientific names of hybrids (e.g. Erysimum decumbens x perofskianum, http://eol.org/pages/5145889),
     *  the \u00D7 or × (multiply) symbol should be used. However, most data sources simply use lower case "x",
     *  and don't support the × symbol is their name matching methods.
     */

    @Test
    public void replaceMultiplyOrXByLowerCaseX() {
        assertThat(CORRECTOR.correct("Genus species1 x species2"), is("Genus species1 x species2"));
        assertThat(CORRECTOR.correct("Genus species1 \u00D7 species2"), is("Genus species1 x species2"));
        assertThat(CORRECTOR.correct("Genus species1 × species2"), is("Genus species1 x species2"));
        assertThat(CORRECTOR.correct("Genus species1 ×hybridName"), is("Genus species1 xhybridName"));
        assertThat(CORRECTOR.correct("Genus species1 X species2"), is("Genus species1 x species2"));
        assertThat(CORRECTOR.correct("Genus species1 XhybridName"), is("Genus species1 xhybridName"));
    }

    @Test
    public void taxonNameTooShort() {
        assertThat(CORRECTOR.correct("G"), is("no name"));
        assertThat(CORRECTOR.correct("H"), is("no name"));
        assertThat(CORRECTOR.correct("HH"), is("HH"));
    }

    @Test
    public void taxonNameUSKI() {
        assertThat(CORRECTOR.correct("Scypha raphanus"), is("Sycon raphanus"));
    }

    @Test
    public void taxonNameWrast() {
        assertThat(CORRECTOR.correct("Pleocyemata spp."), is("Pleocyemata"));
        assertThat(CORRECTOR.correct("Aegathoa oculata "), is("Aegathoa oculata"));
    }

    @Test
    public void circularSuggestions() {
        assertThat(CORRECTOR.correct("Mimesa bicolor"), is("Mimesa bicolor"));
        assertThat(CORRECTOR.correct("Mimesa equestris"), is("Mimesa equestris"));
        assertThat(CORRECTOR.correct("Excalfactoria chinensis"), is("Coturnix chinensis"));
    }
}
