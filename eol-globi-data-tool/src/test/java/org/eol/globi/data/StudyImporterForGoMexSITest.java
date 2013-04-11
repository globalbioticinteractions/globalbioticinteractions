package org.eol.globi.data;


import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.UTMRef;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

public class StudyImporterForGoMexSITest extends GraphDBTestCase {

    @Test
    public void createAndPopulateStudy() throws StudyImporterException, NodeFactoryException {
        StudyImporterForGoMexSI importer = new StudyImporterForGoMexSI(new ParserFactoryImpl(), nodeFactory);

        importer.importStudy();

        assertNotNull(nodeFactory.findTaxonOfType("Chloroscombrus chrysurus"));
        assertNotNull(nodeFactory.findTaxonOfType("Micropogonias undulatus"));

        assertNotNull(nodeFactory.findTaxonOfType("Amphipoda"));
        assertNotNull(nodeFactory.findTaxonOfType("Crustacea"));

        assertNotNull(nodeFactory.findLocation(29.346953,-92.980614,-13.641));

        assertNotNull(nodeFactory.findStudy(StudyLibrary.Study.GOMEXSI.toString()));
    }


}