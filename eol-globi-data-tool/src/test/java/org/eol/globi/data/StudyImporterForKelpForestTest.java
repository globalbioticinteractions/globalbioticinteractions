package org.eol.globi.data;

import org.eol.globi.domain.TaxonNode;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;

public class StudyImporterForKelpForestTest extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException, NodeFactoryException {
        StudyImporterForKelpForest importer = new StudyImporterForKelpForest(new ParserFactoryImpl(), nodeFactory);
        importer.importStudy();
        assertSeaOtter();
    }

    protected void assertSeaOtter() throws NodeFactoryException {
        TaxonNode taxon = nodeFactory.findTaxonByName("sea otter");
        assertThat(taxon, is(notNullValue()));
        assertThat(taxon.getExternalId(), is("ITIS:180547"));
    }


}