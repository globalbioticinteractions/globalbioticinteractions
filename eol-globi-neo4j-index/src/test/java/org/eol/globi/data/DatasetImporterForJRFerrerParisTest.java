package org.eol.globi.data;

import org.hamcrest.core.Is;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class DatasetImporterForJRFerrerParisTest extends GraphDBNeo4jTestCase {

    @Ignore(value = "too slow for regular use")
    @Test
    public void testFullImport() throws StudyImporterException {
        DatasetImporterForJRFerrerParis studyImporterForJRFerrerParis = new DatasetImporterForJRFerrerParis(
                new ParserFactoryLocal(getClass()), nodeFactory
        );
        studyImporterForJRFerrerParis.importStudy();

        assertTrue(getSpecimenCount(getStudySingleton(getGraphDb())) > 0);
    }

    @Test
    public void testSomeLine() throws StudyImporterException, NodeFactoryException {
        String csvContent = "\"\",\"Lepidoptera Family\",\"Lepidoptera Name\",\"Hostplant Family\",\"Hostplant Name\",\"Country\",\"reference\"\n" +
                "\"27385\",\"Pieridae\",\"Hesperocharis anguitia\",\"Santalales\",\"'Loranthus'\",\"Brazil\",\"Braby & Nishida 2007\"\n" +
                "\"27386\",\"Pieridae\",\"Mathania carrizoi\",\"Santalales\",\"'Loranthus'\",\"Argentina\",\"Braby & Nishida 2007\"\n" +
                "\"27387\",\"Pieridae\",\"Mylothris agathina\",\"Santalales\",\"?Loranthus? spp.\",\"Kenya, Tanzania, South Africa\",\"Braby 2005\"\n" +
                "\"27388\",\"Pieridae\",\"Mylothris chloris\",\"Santalales\",\"?Loranthus? spp.\",\"Kenya\",\"Braby 2005\"";

        DatasetImporterForJRFerrerParis importer = new DatasetImporterForJRFerrerParis(new TestParserFactory(csvContent), nodeFactory);

        importStudy(importer);

        assertNotNull(taxonIndex.findTaxonByName("Hesperocharis anguitia"));

        assertThat(getSpecimenCount(getStudySingleton(getGraphDb())), Is.is(8));
    }
}
