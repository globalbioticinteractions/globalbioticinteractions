package org.eol.globi.export;

import org.eol.globi.domain.Study;
import org.eol.globi.util.InteractUtil;

public class StudyExportUnmatchedTargetTaxaForStudies extends StudyExportUnmatchedTaxaForStudies {

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
