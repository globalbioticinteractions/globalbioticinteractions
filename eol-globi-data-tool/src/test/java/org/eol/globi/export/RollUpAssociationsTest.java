package org.eol.globi.export;

import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Study;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class RollUpAssociationsTest extends RollUpTest {

    @Test
    public void export() throws NodeFactoryException, ParseException, IOException {
        Study myStudy = createStudy();
        StringWriter writer = new StringWriter();
        new RollUpAssociations().doExportStudy(myStudy, writer, true);
        String exported = writer.toString();
        assertThat(exported, containsString("globi:assoc:1-genus_id_1-ATE-species_id_2,globi:occur:rsource:1-genus_id_1-ATE,http://eol.org/schema/terms/eats,globi:occur:rtarget:1-genus_id_1-ATE-species_id_2,,,,,,,,globi:ref:1"));
        assertThat(exported, containsString("globi:assoc:1-species_id_1-ATE-family_id_2,globi:occur:rsource:1-species_id_1-ATE,http://eol.org/schema/terms/eats,globi:occur:rtarget:1-species_id_1-ATE-family_id_2,,,,,,,,globi:ref:1"));
        assertThat(exported, containsString("globi:assoc:1-genus_id_1-ATE-family_id_2,globi:occur:rsource:1-genus_id_1-ATE,http://eol.org/schema/terms/eats,globi:occur:rtarget:1-genus_id_1-ATE-family_id_2,,,,,,,,globi:ref:1"));
        assertThat(exported, containsString("globi:assoc:1-species_id_1-ATE-genus_id_2,globi:occur:rsource:1-species_id_1-ATE,http://eol.org/schema/terms/eats,globi:occur:rtarget:1-species_id_1-ATE-genus_id_2,,,,,,,,globi:ref:1"));
        assertThat(exported, containsString("globi:assoc:1-species_id_1-ATE-species_id_2,globi:occur:rsource:1-species_id_1-ATE,http://eol.org/schema/terms/eats,globi:occur:rtarget:1-species_id_1-ATE-species_id_2,,,,,,,,globi:ref:1"));
        assertThat(exported, containsString("globi:assoc:1-genus_id_1-ATE-genus_id_2,globi:occur:rsource:1-genus_id_1-ATE,http://eol.org/schema/terms/eats,globi:occur:rtarget:1-genus_id_1-ATE-genus_id_2,,,,,,,,globi:ref:1"));
    }

    @Test
    public void exportStudyWithExternalId() throws NodeFactoryException, ParseException, IOException {
        Study myStudy = createStudy();
        myStudy.setExternalId("ext:id");
        StringWriter writer = new StringWriter();
        new RollUpAssociations().doExportStudy(myStudy, writer, true);
        String exported = writer.toString();
        assertThat(exported, containsString("globi:assoc:1-genus_id_1-ATE-species_id_2,globi:occur:rsource:1-genus_id_1-ATE,http://eol.org/schema/terms/eats,globi:occur:rtarget:1-genus_id_1-ATE-species_id_2,,,,,,,,globi:ref:1"));
    }

    @Test
    public void exportStudyWithExternalId2() throws NodeFactoryException, ParseException, IOException {
        Study myStudy = createStudy();
        myStudy.setExternalId("http://dx.doi.org/10.1007/bf02784282");
        StringWriter writer = new StringWriter();
        new RollUpAssociations().doExportStudy(myStudy, writer, true);
        String exported = writer.toString();
        assertThat(exported, containsString("globi:assoc:1-genus_id_1-ATE-species_id_2,globi:occur:rsource:1-genus_id_1-ATE,http://eol.org/schema/terms/eats,globi:occur:rtarget:1-genus_id_1-ATE-species_id_2,,,,,,,,globi:ref:1"));
    }

}