package org.eol.globi.export;

import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;

import java.io.IOException;
import java.util.HashMap;

public class ExportTaxonCache implements StudyExporter {

    @Override
    public void exportStudy(final StudyNode study, ExportUtil.Appender writer, boolean includeHeader) throws IOException {
        if (includeHeader) {
            doExport(study, writer);
        }
    }

    protected void doExport(StudyNode study, ExportUtil.Appender appender) throws IOException {
        String query = "CYPHER 1.9 START taxon = node:taxons('*:*') " +
                "MATCH taxon-[?:SAME_AS*0..1]->linkedTaxon " +
                "WHERE has(linkedTaxon.path) " +
                "RETURN linkedTaxon.externalId? as id" +
                ", linkedTaxon.name? as name" +
                ", linkedTaxon.rank? as rank" +
                ", linkedTaxon.commonNames? as commonNames" +
                ", linkedTaxon.path? as path" +
                ", linkedTaxon.pathIds? as pathIds" +
                ", linkedTaxon.pathNames? as pathNames" +
                ", linkedTaxon.speciesName? as speciesName" +
                ", linkedTaxon.speciesId? as speciesId" +
                ", linkedTaxon.genusName? as genusName" +
                ", linkedTaxon.genusId? as genusId" +
                ", linkedTaxon.familyName? as familyName" +
                ", linkedTaxon.familyId? as familyId" +
                ", linkedTaxon.orderName? as orderName" +
                ", linkedTaxon.orderId? as orderId" +
                ", linkedTaxon.className? as className" +
                ", linkedTaxon.classId? as classId" +
                ", linkedTaxon.phylumName? as phylumName" +
                ", linkedTaxon.phylumId? as phylumId" +
                ", linkedTaxon.kingdomName? as kingdomName" +
                ", linkedTaxon.kingdomId? as kingdomId" +
                ", taxon.externalUrl? as externalUrl" +
                ", taxon.thumbnailUrl? as thumbnailUrl";

        HashMap<String, Object> params = new HashMap<String, Object>() {{
        }};

        ExportUtil.writeResults(appender,
                study.getUnderlyingNode().getGraphDatabase(),
                query,
                params,
                true
        );
    }
}
