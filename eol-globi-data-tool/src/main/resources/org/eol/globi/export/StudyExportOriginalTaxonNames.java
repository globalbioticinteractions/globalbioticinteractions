package org.eol.globi.export;

import org.eol.globi.domain.Study;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.ResourceIterator;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class StudyExportOriginalTaxonNames implements StudyExporter {
    @Override
    public void exportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(study.getUnderlyingNode().getGraphDatabase());

        StringBuilder query = new StringBuilder();
        query.append("START taxon = node:taxons('*:*')");
        query.append(" MATCH taxon<-[:CLASSIFIED_AS]-specimen-[:ORIGINALLY_DESCRIBED_AS]->origName");
        query.append(" RETURN distinct(origName.name) as name");

        ExecutionResult results = engine.execute(query.toString());

        ResourceIterator<Map<String,Object>> iterator = results.iterator();
        boolean isFirst = true;
        while (iterator.hasNext()) {
            if (!isFirst) {
                writer.append("\n");
            }
            writer.append((String) iterator.next().get("name"));
            isFirst = false;
        }
        iterator.close();
    }
}
