package org.trophic.graph.data;

import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.trophic.graph.domain.Location;
import org.trophic.graph.domain.RelTypes;
import org.trophic.graph.domain.Specimen;
import org.trophic.graph.domain.Study;
import org.trophic.graph.domain.Taxon;

import java.io.IOException;
import java.io.StringWriter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class StudyExporterTest extends GraphDBTestCase {

    @Test
    public void exportMissingLength() throws IOException, NodeFactoryException {
        createTestData(null);
        String expected = "";
        expected += "\"study\",\"predator\", \"length(mm)\",\"prey\", \"latitude\", \"longitude\", \"altitude\"";
        expected += "\n\"myStudy\",\"Homo sapiens\",,\"Canis lupus\",123.0,345.9,-60.0";
        expected += "\n\"myStudy\",\"Homo sapiens\",,\"Canis lupus\",123.0,345.9,-60.0";

        Study myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        new StudyExporterImpl().exportStudy(myStudy1, row);

        assertThat(row.getBuffer().toString(), equalTo(expected));
    }

    @Test
    public void exportToCSV() throws NodeFactoryException, IOException {

        Double length = 123.0;
        createTestData(length);

        String expected = "";
        expected += "\"study\",\"predator\", \"length(mm)\",\"prey\", \"latitude\", \"longitude\", \"altitude\"";
        expected += "\n\"myStudy\",\"Homo sapiens\",123.0,\"Canis lupus\",123.0,345.9,-60.0";
        expected += "\n\"myStudy\",\"Homo sapiens\",123.0,\"Canis lupus\",123.0,345.9,-60.0";

        Study myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        new StudyExporterImpl().exportStudy(myStudy1, row);

        assertThat(row.getBuffer().toString(), equalTo(expected));

    }

    private void createTestData(Double length) throws NodeFactoryException {
        Study myStudy = nodeFactory.createStudy("myStudy");
        Specimen specimen = nodeFactory.createSpecimen();
        myStudy.collected(specimen);
        Taxon taxon = nodeFactory.createTaxon("Homo sapiens", null);
        specimen.classifyAs(taxon);
        Specimen otherSpecimen = nodeFactory.createSpecimen();
        Taxon wolf = nodeFactory.createTaxon("Canis lupus", null);
        otherSpecimen.classifyAs(wolf);
        specimen.ate(otherSpecimen);
        specimen.ate(otherSpecimen);
        if (null != length) {
            specimen.setLengthInMm(length);
        }

        Location location = nodeFactory.createLocation(123.0, 345.9, -60.0);
        specimen.caughtIn(location);
    }


}
