package org.eol.globi.taxon;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TaxonNameCorrectorTest {

    private final static CorrectionService CORRECTOR = new TaxonNameCorrector();

    @Test
    public void cleanName() {
        assertThat(CORRECTOR.correct(""), is("no name"));
        assertThat(CORRECTOR.correct("a"), is("no name"));
        assertThat(CORRECTOR.correct("NA"), is("no name"));
        assertThat(CORRECTOR.correct("NE"), is("no name"));

        assertThat(CORRECTOR.correct("Aneugmenus fürstenbergensis"), is("Aneugmenus fuerstenbergensis"));
        assertThat(CORRECTOR.correct("Xanthorhoë"), is("Xanthorhoe"));

        assertThat(CORRECTOR.correct("Cal sapidus"), is("Callinectes sapidus"));

        assertThat(CORRECTOR.correct("Bivalvia Genus A"), is("Bivalvia"));
        assertThat(CORRECTOR.correct("EOL:123"), is("EOL 123"));
        assertThat(CORRECTOR.correct("Pegoscapus cf. obscurus (Kirby, 1890)"), is("Pegoscapus obscurus"));


    }

    @Test
    public void taxonNameNoMatch() {
        assertThat(CORRECTOR.correct(PropertyAndValueDictionary.NO_MATCH), is(PropertyAndValueDictionary.NO_MATCH));
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
    public void taxonManualCorrectionInAdditionToPreferredNameSelection() {
        assertThat(CORRECTOR.correct("S enflata"), is("Flaccisagitta enflata"));
    }

    @Test
    public void taxonNameWrast() {
        assertThat(CORRECTOR.correct("Pleocyemata spp."), is("Pleocyemata"));
        assertThat(CORRECTOR.correct("Aegathoa oculata "), is("Aegathoa oculata"));
    }

    @Test
    public void longSpineSwimmingCrab() {
        assertThat(CORRECTOR.correct("Acheloüs spinicarpus"), is("Achelous spinicarpus"));
    }

    @Test
    public void circularSuggestions() {
        assertThat(CORRECTOR.correct("Mimesa bicolor"), is("Mimesa bicolor"));
        assertThat(CORRECTOR.correct("Mimesa equestris"), is("Mimesa equestris"));
        assertThat(CORRECTOR.correct("Excalfactoria chinensis"), is("Coturnix chinensis"));
    }
}
