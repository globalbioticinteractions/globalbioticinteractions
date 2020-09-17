package org.eol.globi.data;

import org.eol.globi.domain.InteractType;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.tool.NullImportLogger;
import org.eol.globi.util.InteractTypeMapper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID_VERBATIM;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME_VERBATIM;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
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
        }, new NullImportLogger());
        listener.newLink(new HashMap<String, String>() {
            {
                put(INTERACTION_TYPE_NAME, "eats");
                put(TaxonUtil.TARGET_TAXON_GENUS, "Donald");
                put(TaxonUtil.TARGET_TAXON_SPECIFIC_EPITHET, "duck");
            }
        });

        assertThat(links.size(), is(1));
        assertThat(links.get(0).get(TaxonUtil.TARGET_TAXON_NAME), is("Donald duck"));
        assertThat(links.get(0).get(TaxonUtil.TARGET_TAXON_PATH_NAMES), is("genus | species"));
        assertThat(links.get(0).get(TaxonUtil.TARGET_TAXON_PATH), is("Donald | Donald duck"));
    }

    @Test
    public void withVerbatimInteractionType() throws StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<>();
        InteractionListener listener = new InteractionListenerWithInteractionTypeMapping(links::add, new InteractTypeMapper() {
            @Override
            public boolean shouldIgnoreInteractionType(String nameOrId) {
                return false;
            }

            @Override
            public InteractType getInteractType(String nameOrId) {
                return InteractType.ATE;
            }
        }, new NullImportLogger());
        listener.newLink(new HashMap<String, String>() {
            {
                put(INTERACTION_TYPE_NAME, "Donald");
                put(INTERACTION_TYPE_ID, "duck");
            }
        });

        assertThat(links.size(), is(1));
        assertThat(links.get(0).get(INTERACTION_TYPE_ID_VERBATIM), is("duck"));
        assertThat(links.get(0).get(INTERACTION_TYPE_NAME_VERBATIM), is("Donald"));
        assertThat(links.get(0).get(INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002470"));
        assertThat(links.get(0).get(INTERACTION_TYPE_NAME), is("eats"));
    }

    @Test
    public void withBlankInteractionType() throws StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<>();
        InteractionListener listener = new InteractionListenerWithInteractionTypeMapping(links::add, new InteractTypeMapper() {
            @Override
            public boolean shouldIgnoreInteractionType(String nameOrId) {
                return false;
            }

            @Override
            public InteractType getInteractType(String nameOrId) {
                return InteractType.ATE;
            }
        }, new NullImportLogger());
        listener.newLink(new HashMap<String, String>() {
            {
                put(INTERACTION_TYPE_NAME, "");
                put(INTERACTION_TYPE_ID, "");
            }
        });

        assertThat(links.size(), is(1));
        assertThat(links.get(0).get(INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002470"));
        assertThat(links.get(0).get(INTERACTION_TYPE_NAME), is("eats"));
        assertThat(links.get(0).get(INTERACTION_TYPE_ID_VERBATIM), is(""));
        assertThat(links.get(0).get(INTERACTION_TYPE_NAME_VERBATIM), is(""));
    }


    @Test
    public void withNullInteractionType() throws StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<>();
        InteractionListener listener = new InteractionListenerWithInteractionTypeMapping(links::add, new InteractTypeMapper() {
            @Override
            public boolean shouldIgnoreInteractionType(String nameOrId) {
                return false;
            }

            @Override
            public InteractType getInteractType(String nameOrId) {
                return InteractType.ATE;
            }
        }, new NullImportLogger());
        listener.newLink(new HashMap<String, String>() {
            {
                put(INTERACTION_TYPE_NAME, null);
                put(INTERACTION_TYPE_ID, null);
            }
        });

        assertThat(links.size(), is(1));
        assertThat(links.get(0).get(INTERACTION_TYPE_ID), is(nullValue()));
        assertThat(links.get(0).get(INTERACTION_TYPE_NAME), is(nullValue()));
        assertThat(links.get(0).get(INTERACTION_TYPE_ID_VERBATIM), is(nullValue()));
        assertThat(links.get(0).get(INTERACTION_TYPE_NAME_VERBATIM), is(nullValue()));
    }


}