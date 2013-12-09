package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Relationship;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class StudyImporterForHechingerTest extends GraphDBTestCase{

    @Test
    public void importStudy() throws StudyImporterException {
        StudyImporter importer = new StudyImporterForHechinger(new ParserFactoryImpl(), nodeFactory);
        Study study = importer.importStudy();

        assertThat(study, is(notNullValue()));

        Iterable<Relationship> specimens = study.getSpecimens();
        int count = 0;
        for (Relationship specimen : specimens) {
            count++;
        }

        ExecutionEngine engine = new ExecutionEngine(getGraphDb());
        ExecutionResult result = engine.execute("START resourceTaxon = node:taxons(name='Suaeda')" +
                " MATCH taxon<-[:CLASSIFIED_AS]-specimen-[r]->resourceSpecimen-[:CLASSIFIED_AS]-resourceTaxon" +
                " RETURN taxon.name, specimen.lifeStage?, type(r), resourceTaxon.name, resourceSpecimen.lifeStage?");

        assertThat(result.dumpToString(), containsString("Anas acuta"));
        assertThat(result.dumpToString(), containsString("Aythya affinis"));

        assertThat(count, is(13966));

    }
}
