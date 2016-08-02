package org.eol.globi.taxon;

import org.eol.globi.service.NameSuggester;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@Ignore
public class GlobalNamesCanonTest {

    protected NameSuggester getNameSuggester() {
        return new GlobalNamesCanon();
    }

    @Test
    public void subspecies() {
        assertThat(getNameSuggester().suggest("Aegathoa oculata spp"), is("Aegathoa oculata"));
        assertThat(getNameSuggester().suggest("Aegathoa oculata spp."), is("Aegathoa oculata"));
        assertThat(getNameSuggester().suggest("Aegathoa oculata spp. Aegathoa oculata spp. Aegathoa oculata spp."), is("Aegathoa oculata"));

        assertThat(getNameSuggester().suggest("Aegathoa oculata ssp. fred"), is("Aegathoa oculata ssp. fred"));
        assertThat(getNameSuggester().suggest("Aegathoa oculata ssp fred"), is("Aegathoa oculata ssp. fred"));
        assertThat(getNameSuggester().suggest("Aegathoa oculata subsp fred"), is("Aegathoa oculata ssp. fred"));
        assertThat(getNameSuggester().suggest("Aegathoa oculata subsp. fred"), is("Aegathoa oculata ssp. fred"));
        assertThat(getNameSuggester().suggest("Acrididae spp. "), is("Acrididae"));
        assertThat(getNameSuggester().suggest("Pleocyemata spp."), is("Pleocyemata"));

    }

    @Test
    public void species() {
        assertThat(getNameSuggester().suggest("Aegathoa oculata sp."), is("Aegathoa oculata"));
    }


    @Test
    public void varieties() {
        assertThat(getNameSuggester().suggest("Puccinia dioicae var. dioicae"), is("Puccinia dioicae var. dioicae"));
        assertThat(getNameSuggester().suggest("Puccinia dioicae var dioicae"), is("Puccinia dioicae var. dioicae"));
    }

    @Test
    public void removeWhitespaces() {
        assertThat(getNameSuggester().suggest("Aegathoa oculata "), is("Aegathoa oculata"));
        assertThat(getNameSuggester().suggest("Blbua blas "), is("Blbua blas"));
        assertThat(getNameSuggester().suggest("Blbua  blas  "), is("Blbua blas"));
        assertThat(getNameSuggester().suggest("Blbua  blas (akjhaskdjhf)"), is("Blbua blas"));
        assertThat(getNameSuggester().suggest("  Blbua  blas (akjhaskdjhf)"), is("Blbua blas"));
        assertThat(getNameSuggester().suggest("Taraxacum leptaleum"), is("Taraxacum leptaleum"));

        assertThat(getNameSuggester().suggest("Mycosphaerella filipendulae-denudatae"), is("Mycosphaerella filipendulae-denudatae"));


        //assertThat(normalizer.normalize("Armeria 'Bees Ruby'"), is("Armeria 'Bees Ruby'"));


        assertThat(getNameSuggester().suggest("Limonia (Dicranomyia) chorea"), is("Limonia chorea"));

        assertThat(getNameSuggester().suggest("Archips podana/operana"), is("Archips"));

        assertThat(getNameSuggester().suggest("Leptochela cf bermudensis"), is("Leptochela bermudensis"));

        assertThat(getNameSuggester().suggest("Xanthorhoë"), is("Xanthorhoe"));


    }

    @Test
    @Ignore
    public void dubiousAssertions() {
        assertThat(getNameSuggester().suggest("Rubus fruticosus agg."), is("Rubus fruticosus agg."));
        // Malcolm Storey technique to distinguish duplicate genus names in taxonomies.
        assertThat(getNameSuggester().suggest("Ammophila (Bot.)"), is("Ammophila (Bot.)"));
        assertThat(getNameSuggester().suggest("Ammophila (Zool.)"), is("Ammophila (Zool.)"));
        assertThat(getNameSuggester().suggest("Ammophila (zoo)"), is("Ammophila (zoo)"));
        assertThat(getNameSuggester().suggest("Ammophila (bot)"), is("Ammophila (bot)"));
        assertThat(getNameSuggester().suggest("Ammophila (bot.)"), is("Ammophila (bot.)"));
        assertThat(getNameSuggester().suggest("Ammophila (Bot)"), is("Ammophila (Bot)"));
        assertThat(getNameSuggester().suggest("Ammophila (blah)"), is("Ammophila"));

    }

    @Test
    public void repetition() {
        assertThat(getNameSuggester().suggest("Bacteriastrum spp.          Bacteriastrum spp.          Bacteriastrum spp.          Bacteriastrum spp."), is("Bacteriastrum"));
    }

    /**
     * In scientific names of hybrids (e.g. Erysimum decumbens x perofskianum, http://eol.org/pages/5145889),
     * the \u00D7 or × (multiply) symbol should be used. However, most data sources simply use lower case "x",
     * and don't support the × symbol is their name matching methods.
     */

    @Test
    public void replaceMultiplyOrXByLowerCaseX() {
        final String expected = "Arthopyrenia hyalospora × Hydnellum scrobiculatum";
        assertThat(getNameSuggester().suggest("Lobelia cardinalis x siphilitica"), is("Lobelia cardinalis × siphilitica"));
        assertThat(getNameSuggester().suggest("Arthopyrenia hyalospora X Hydnellum scrobiculatum"), is(expected));
        assertThat(getNameSuggester().suggest("Arthopyrenia hyalospora x Hydnellum scrobiculatum"), is(expected));
        assertThat(getNameSuggester().suggest("Arthopyrenia hyalospora \u00D7 Hydnellum scrobiculatum"), is(expected));
    }


}
