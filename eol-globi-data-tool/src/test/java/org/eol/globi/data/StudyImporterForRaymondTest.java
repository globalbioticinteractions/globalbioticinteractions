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

public class StudyImporterForRaymondTest extends GraphDBTestCase{

    @Test
    public void importStudy() throws StudyImporterException {
        StudyImporter importer = new StudyImporterForRaymond(new ParserFactoryImpl(), nodeFactory);
        Study study = importer.importStudy();

        assertThat(study, is(notNullValue()));
    }
}
