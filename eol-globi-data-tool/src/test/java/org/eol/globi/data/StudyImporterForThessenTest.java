package org.eol.globi.data;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonNode;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class StudyImporterForThessenTest extends GraphDBTestCase {

    @Test
    public void importSome() throws StudyImporterException, NodeFactoryException {
        StudyImporterForThessen importer = new StudyImporterForThessen(new ParserFactoryImpl(), nodeFactory);
        importer.setFilter(new ImportFilter() {
            @Override
            public boolean shouldImportRecord(Long recordNumber) {
                return recordNumber % 1000 == 0;
            }
        });
        Study study = importStudy(importer);
        assertThat(study.getExternalId(), containsString("github"));
        Iterable<Relationship> specimens = study.getSpecimens();
        int specimenCount = 0;
        Set<String> taxonIds = new HashSet<String>();
        while (specimens.iterator().hasNext()) {
            Node sourceSpecimen = specimens.iterator().next().getEndNode();
            specimenCount++;
            Node taxonNode = sourceSpecimen.getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING).getEndNode();
            taxonIds.add((String) taxonNode.getProperty(PropertyAndValueDictionary.EXTERNAL_ID));
        }

        assertThat(specimenCount > 10, is(true));
        assertThat(taxonIds.size() > 1, is(true));
        assertThat(study.getCitation(), is(notNullValue()));
        assertThat(study.getTitle(), is(notNullValue()));

        for (String taxonId : taxonIds) {
            TaxonNode taxon = taxonIndex.findTaxonById(taxonId);
            assertThat(taxon, is(notNullValue()));
            assertThat(taxon.getName(), is(notNullValue()));
        }

    }
}
