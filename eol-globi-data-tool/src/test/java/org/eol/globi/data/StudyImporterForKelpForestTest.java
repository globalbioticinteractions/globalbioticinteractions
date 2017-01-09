package org.eol.globi.data;

import org.eol.globi.domain.Taxon;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;

public class StudyImporterForKelpForestTest extends GraphDBTestCase {

    @Ignore("kelp forest not available on 1 Nov 2015: Caused by: org.apache.http.conn.HttpHostConnectException: Connect to kelpforest.ucsc.edu:80 [kelpforest.ucsc.edu/128.114.235.111] failed: Operation timed out")
    @Test
    public void importAll() throws StudyImporterException, NodeFactoryException {
        StudyImporterForKelpForest importer = new StudyImporterForKelpForest(new ParserFactoryLocal(), nodeFactory);
        importStudy(importer);
        assertSeaOtter();
    }

    protected void assertSeaOtter() throws NodeFactoryException {
        Taxon taxon = taxonIndex.findTaxonByName("sea otter");
        assertThat(taxon, is(notNullValue()));
        assertThat(taxon.getExternalId(), is("ITIS:180547"));
    }


}