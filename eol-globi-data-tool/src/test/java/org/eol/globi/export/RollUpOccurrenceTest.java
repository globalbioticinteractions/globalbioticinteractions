package org.eol.globi.export;

import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class RollUpOccurrenceTest extends RollUpTest {

    @Test
    public void export() throws NodeFactoryException, ParseException, IOException {
        Study myStudy = createStudy();
        StringWriter writer = new StringWriter();
        new RollUpOccurrence().doExportStudy(myStudy, writer, true);
        String exported = writer.toString();
        assertThat(exported, containsString("globi:occur:rtarget:1-species_id_1-ATE-genus_id_2,genus id 2,,,,,,,,,,,,,,,,,,,,,,,,,,"));
        assertThat(exported, containsString("globi:occur:rtarget:1-genus_id_1-ATE-family_id_2,family id 2,,,,,,,,,,,,,,,,,,,,,,,,,,"));
        assertThat(exported, containsString("globi:occur:rtarget:1-genus_id_1-ATE-genus_id_2,genus id 2,,,,,,,,,,,,,,,,,,,,,,,,,,"));
        assertThat(exported, containsString("globi:occur:rsource:1-species_id_1-ATE,species id 1,,,,,,,,,,,,,,,,,,,,,,,,,,"));
        assertThat(exported, containsString("globi:occur:rtarget:1-species_id_1-ATE-family_id_2,family id 2,,,,,,,,,,,,,,,,,,,,,,,,,,"));
        assertThat(exported, containsString("globi:occur:rtarget:1-species_id_1-ATE-species_id_2,species id 2,,,,,,,,,,,,,,,,,,,,,,,,,,"));
        assertThat(exported, containsString("globi:occur:rtarget:1-genus_id_1-ATE-species_id_2,species id 2,,,,,,,,,,,,,,,,,,,,,,,,,,"));
        assertThat(exported, containsString("globi:occur:rsource:1-genus_id_1-ATE,genus id 1,,,,,,,,,,,,,,,,,,,,,,,,,,"));
    }

}