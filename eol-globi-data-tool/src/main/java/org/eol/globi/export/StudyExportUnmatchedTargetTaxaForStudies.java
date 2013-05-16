package org.eol.globi.export;

import org.eol.globi.util.InteractUtil;
import org.eol.globi.domain.Study;
import org.eol.globi.service.NoMatchService;
import org.neo4j.graphdb.GraphDatabaseService;

public class StudyExportUnmatchedTargetTaxaForStudies extends StudyExportUnmatchedTaxaForStudies {

    public StudyExportUnmatchedTargetTaxaForStudies(GraphDatabaseService graphDatabaseService) {
        super(graphDatabaseService);
    }

    protected String getQueryString(Study study) {
        String interactionsClause = InteractUtil.allInteractionsCypherClause();
        return "MATCH study-[:COLLECTED]->sourceSpecimen-[:" + interactionsClause + "]->targetSpecimen-[:CLASSIFIED_AS]->taxon, " +
                    "targetSpecimen-[:ORIGINALLY_DESCRIBED_AS]->description ";
    }

    @Override
    protected String getTaxonLabel() {
        return "target";
    }

}
