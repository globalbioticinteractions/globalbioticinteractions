package org.eol.globi.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private static final Log LOG = LogFactory.getLog(StudyImporterForHechingerTest.class);

    @Test
    public void importStudy() throws StudyImporterException {
        StudyImporter importer = new StudyImporterForHechinger(new ParserFactoryImpl(), nodeFactory);
        importer.setLogger(new ImportLogger() {
            @Override
            public void warn(Study study, String message) {
                LOG.warn(message);
            }

            @Override
            public void info(Study study, String message) {
                LOG.info(message);
            }

            @Override
            public void severe(Study study, String message) {
                LOG.error(message);
            }
        });
        Study study = importer.importStudy();

        assertThat(study, is(notNullValue()));

        Iterable<Relationship> specimens = study.getSpecimens();
        int count = 0;
        for (Relationship specimen : specimens) {
            count++;
        }

        ExecutionEngine engine = new ExecutionEngine(getGraphDb());
        String query = "START resourceTaxon = node:taxons(name='Suaeda spp.')" +
                " MATCH taxon<-[:CLASSIFIED_AS]-specimen-[r]->resourceSpecimen-[:CLASSIFIED_AS]-resourceTaxon, specimen-[:COLLECTED_AT]->location" +
                " RETURN taxon.name, specimen.lifeStage?, type(r), resourceTaxon.name, resourceSpecimen.lifeStage?, location.latitude as lat, location.longitude as lng";
        ExecutionResult result = engine.execute(query);

        assertThat(result.dumpToString(), containsString("Branta bernicla"));
        assertThat(result.dumpToString(), containsString("Athya affinis"));
        assertThat(result.dumpToString(), containsString("Anas acuta"));
        assertThat(result.dumpToString(), containsString("30.378207 | -115.938835 |"));

        assertThat(count, is(13966));

    }
}
