package org.eol.globi.export;

import org.eol.globi.domain.Study;
import org.eol.globi.service.NoMatchService;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class StudyExportUnmatchedSourceTaxaForStudies extends StudyExportUnmatchedTaxaForStudies {

    protected String getQueryString(Study study) {
        return "MATCH study-[:COLLECTED]->specimen-[:CLASSIFIED_AS]->taxon, " +
                "specimen-[:ORIGINALLY_DESCRIBED_AS]->description ";
    }

    @Override
    protected String getTaxonLabel() {
        return "source";
    }

}
