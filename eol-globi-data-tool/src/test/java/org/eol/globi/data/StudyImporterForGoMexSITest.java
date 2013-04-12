package org.eol.globi.data;


import org.junit.Test;

import static junit.framework.Assert.assertNotNull;

public class StudyImporterForGoMexSITest extends GraphDBTestCase {

    @Test
    public void createAndPopulateStudy() throws StudyImporterException, NodeFactoryException {
        StudyImporterForGoMexSI importer = new StudyImporterForGoMexSI(new ParserFactoryImpl(), nodeFactory);

        importer.importStudy();

        assertNotNull(nodeFactory.findTaxonOfType("Chloroscombrus chrysurus"));
        assertNotNull(nodeFactory.findTaxonOfType("Micropogonias undulatus"));

        assertNotNull(nodeFactory.findTaxonOfType("Amphipoda"));
        assertNotNull(nodeFactory.findTaxonOfType("Crustacea"));

        assertNotNull(nodeFactory.findLocation(29.346953, -92.980614, -13.641));

        assertNotNull(nodeFactory.findStudy(StudyImporterFactory.Study.GOMEXSI.toString()));
    }


}