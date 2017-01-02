package org.eol.globi.export;

import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

public class ExportUnmatchedTaxonNames implements StudyExporter {

    @Override
    public void exportStudy(final Study study, Writer writer, boolean includeHeader) throws IOException {

        String query = "START study = node:studies(title={study_title}) " +
                "MATCH study-[:COLLECTED]->specimen-[:CLASSIFIED_AS]->taxon " +
                "WITH distinct(taxon) as dtaxon, study " +
                "MATCH dtaxon-[sameAs?:SAME_AS]->otherTaxon " +
                "WHERE not(has(dtaxon.path)) AND otherTaxon = null " +
                "WITH dtaxon, otherTaxon, study " +
                "MATCH study-[:COLLECTED]->specimen-[:CLASSIFIED_AS]->dtaxon, specimen-[:ORIGINALLY_DESCRIBED_AS]->origTaxon, dtaxon-[?:SIMILAR_TO]->ftaxon\n" +
                "RETURN distinct(origTaxon.name) as `unmatched taxon name`" +
                ", origTaxon.externalId? as `unmatched taxon id`" +
                ", dtaxon.statusLabel? as `name status`" +
                ", ftaxon.name? as `similar to taxon name`" +
                ", ftaxon.path? as `similar to taxon path`" +
                ", ftaxon.externalId? as `similar to taxon id`" +
                ", study.citation? as `study`" +
                ", study.source as `source`";

        HashMap<String, Object> params = new HashMap<String, Object>() {{
            put("study_title", study.getTitle());
        }};

        ExportUtil.writeResults(writer, ((StudyNode)study).getUnderlyingNode().getGraphDatabase(), query, params, includeHeader);
    }

}
