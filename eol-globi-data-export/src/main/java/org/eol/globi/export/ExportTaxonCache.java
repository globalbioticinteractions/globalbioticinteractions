package org.eol.globi.export;

import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

public class ExportTaxonCache implements StudyExporter {

    @Override
    public void exportStudy(final Study study, Writer writer, boolean includeHeader) throws IOException {
        if (includeHeader) {
            doExport(study, writer);
        }
    }

    protected void doExport(Study study, Writer writer) throws IOException {
        String query = "START taxon = node:taxons('*:*') " +
                "MATCH taxon-[?:SAME_AS*0..1]->linkedTaxon " +
                "WHERE has(linkedTaxon.path) " +
                "RETURN linkedTaxon.externalId? as id" +
                ", linkedTaxon.name? as name" +
                ", linkedTaxon.rank? as rank" +
                ", linkedTaxon.commonNames? as commonNames" +
                ", linkedTaxon.path? as path" +
                ", linkedTaxon.pathIds? as pathIds" +
                ", linkedTaxon.pathNames? as pathNames" +
                ", taxon.externalUrl? as externalUrl" +
                ", taxon.thumbnailUrl? as thumbnailUrl";

        HashMap<String, Object> params = new HashMap<String, Object>() {{
        }};

        ExportUtil.writeResults(writer, ((StudyNode)study).getUnderlyingNode().getGraphDatabase(), query, params, true);
    }
}
