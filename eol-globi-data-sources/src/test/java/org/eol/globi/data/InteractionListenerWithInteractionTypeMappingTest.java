package org.eol.globi.data;

import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.LogContext;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.InteractTypeMapper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class InteractionListenerWithInteractionTypeMappingTest {

    @Test
    public void withTaxonHierachy() throws StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<Map<String, String>>();
        InteractionListener listener = new InteractionListenerWithInteractionTypeMapping(links::add, new InteractTypeMapper() {
            @Override
            public boolean shouldIgnoreInteractionType(String nameOrId) {
                return false;
            }

            @Override
            public InteractType getInteractType(String nameOrId) {
                return null;
            }
        }, new ImportLogger() {
            @Override
            public void warn(LogContext ctx, String message) {

            }

            @Override
            public void info(LogContext ctx, String message) {

            }

            @Override
            public void severe(LogContext ctx, String message) {

            }
        });
        listener.newLink(new HashMap<String, String>() {
            {
                put(TaxonUtil.TARGET_TAXON_GENUS, "Donald");
                put(TaxonUtil.TARGET_TAXON_SPECIFIC_EPITHET, "duck");
            }
        });

        assertThat(links.size(), is(1));
        assertThat(links.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Donald duck"));
        assertThat(links.get(0).get(TaxonUtil.TARGET_TAXON_PATH_NAMES), is("genus | species"));
        assertThat(links.get(0).get(TaxonUtil.TARGET_TAXON_PATH), is("Donald | Donald duck"));
    }


}