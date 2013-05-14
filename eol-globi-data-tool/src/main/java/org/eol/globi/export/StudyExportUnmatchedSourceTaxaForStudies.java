package org.eol.globi.export;

import org.eol.globi.domain.Study;
import org.eol.globi.service.NoMatchService;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class StudyExportUnmatchedSourceTaxaForStudies extends StudyExportUnmatchedTaxaForStudies {

    public StudyExportUnmatchedSourceTaxaForStudies(GraphDatabaseService graphDatabaseService) {
        super(graphDatabaseService);
    }

    protected String getQueryString(Study study) {
        return "START study = node:studies(title=\"" + study.getTitle() + "\") " +
                    "MATCH study-[:COLLECTED]->specimen-[:CLASSIFIED_AS]->taxon, specimen-[:ORIGINALLY_DESCRIBED_AS]->description " +
                    "WHERE taxon.externalId = \"" + NoMatchService.NO_MATCH + "\" " +
                    "RETURN distinct description.name, taxon.name, study.title";
    }

    @Override
    protected String getTaxonLabel() {
        return "source";
    }

}
