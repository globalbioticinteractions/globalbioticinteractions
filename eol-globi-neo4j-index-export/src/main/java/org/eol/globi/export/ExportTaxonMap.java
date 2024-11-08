package org.eol.globi.export;

import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;

import java.io.IOException;
import java.util.HashMap;

public class ExportTaxonMap implements StudyExporter {

    @Override
    public void exportStudy(final StudyNode study, ExportUtil.Appender writer, boolean includeHeader) throws IOException {
        if (includeHeader) {
            doExport(study, writer);
        }
    }

    protected void doExport(StudyNode study, ExportUtil.Appender writer) throws IOException {
        String query = "CYPHER 2.3 START study = node:studies('*:*')\n" +
                "MATCH study-[:COLLECTED|REFUTES|SUPPORTS]->specimen-[:ORIGINALLY_DESCRIBED_AS]->origTaxon, specimen-[:CLASSIFIED_AS]->taxon " +
                "WITH distinct(origTaxon.name) as origName, origTaxon.externalId as origId, taxon " +
                "MATCH taxon-[:SAME_AS*0..1]->linkedTaxon " +
                "WHERE has(linkedTaxon.path) " +
                "RETURN origId as providedTaxonId" +
                ", origName as providedTaxonName" +
                ", linkedTaxon.externalId as resolvedTaxonId" +
                ", linkedTaxon.name as resolvedTaxonName";

        ExportUtil.writeResults(writer,
                study.getUnderlyingNode().getGraphDatabase(),
                query,
                new HashMap<>(),
                true
        );
    }
}
