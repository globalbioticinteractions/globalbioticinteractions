package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterForThessenIT extends GraphDBTestCase {

    @Test
    public void importSome() throws StudyImporterException {
        StudyImporterForThessen importer = new StudyImporterForThessen(new ParserFactoryImpl(), nodeFactory);
        importer.setFilter(new ImportFilter() {
            @Override
            public boolean shouldImportRecord(Long recordNumber) {
                return recordNumber % 100 == 0;
            }
        });
        Study study = importer.importStudy();
        Iterable<Relationship> specimens = study.getSpecimens();
        int count = 0;
        while(specimens.iterator().hasNext()) {
            specimens.iterator().next();
            count++;
        }

        assertThat(count > 10, is(true));
        assertThat(study.getCitation(), is(notNullValue()));
        assertThat(study.getTitle(), is(notNullValue()));
    }
}
