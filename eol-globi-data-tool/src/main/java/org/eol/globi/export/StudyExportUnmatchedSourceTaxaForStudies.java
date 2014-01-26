package org.eol.globi.export;

import org.eol.globi.domain.Study;

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
