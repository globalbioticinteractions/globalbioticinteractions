package org.eol.globi.data;

import org.eol.globi.service.TaxonUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class AttackRemarksParserTest {

    @Test
    // see https://github.com/globalbioticinteractions/scan/issues/6
    public void occurrenceRemarkAttack() throws IOException {
        String occurrenceRemarks = "attacking maple";

        Map<String, String> properties = new AttackRemarksParser().parse(occurrenceRemarks);

        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is("maple"));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is("attacks"));
    }

    @Test
    // see https://github.com/globalbioticinteractions/scan/issues/6
    public void occurrenceRemarkAttackingMoth() throws IOException {
        String occurrenceRemarks = "attacking moth on spreading board";

        Map<String, String> properties = new AttackRemarksParser().parse(occurrenceRemarks);
        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is("moth on spreading board"));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is("attacks"));
    }

    @Test
    // see https://github.com/globalbioticinteractions/scan/issues/6
    public void attackedByCat() throws IOException {
        String occurrenceRemarks = "attacked by cat";

        Map<String, String> properties = new AttackRemarksParser().parse(occurrenceRemarks);

        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is("cat"));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is("attacked by"));
    }

    @Test
    // see https://github.com/globalbioticinteractions/scan/issues/6
    public void occurrenceRemarkAttack2() throws IOException {
        String occurrenceRemarks = "FIELDNOTES: BEING ATTACKED BY TWO KITTNES IN GRASY AREA";

        Map<String, String> properties = new AttackRemarksParser().parse(occurrenceRemarks);

        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is("TWO KITTNES IN GRASY AREA"));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is("attacked by"));
    }

    @Test
    // see https://github.com/globalbioticinteractions/scan/issues/6
    public void occurrenceRemarkDogAttack() throws IOException {
        String occurrenceRemarks = "Dog attacked, fractures to the body";

        Map<String, String> properties = new AttackRemarksParser().parse(occurrenceRemarks);

        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is("dog"));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is("attacked by"));
    }

    @Test
    // see https://github.com/globalbioticinteractions/scan/issues/6
    public void occurrenceRemarkUnknownAnimalAttack() throws IOException {
        String occurrenceRemarks = "attacked by unknown animal";

        Map<String, String> properties = new AttackRemarksParser().parse(occurrenceRemarks);
        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is("unknown animal"));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is("attacked by"));
    }

    @Test
    // see https://github.com/globalbioticinteractions/scan/issues/6
    public void occurrenceRemarkAttackByOther() throws IOException {
        String occurrenceRemarks = "Attacked by Other";

        Map<String, String> properties = new AttackRemarksParser().parse(occurrenceRemarks);

        assertThat(properties.get(TaxonUtil.TARGET_TAXON_NAME), is("Other"));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is("attacked by"));
    }



}