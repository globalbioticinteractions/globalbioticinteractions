package org.eol.globi.export;

import org.eol.globi.data.LifeStage;
import org.eol.globi.domain.BodyPart;
import org.eol.globi.domain.PhysiologicalState;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.EnvoTerm;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ExporterAssociationsTest extends GraphDBTestCase {

    @Test
    public void exportWithHeader() throws IOException, NodeFactoryException, ParseException {
        createTestData(null);

        String expected = "\nglobi:assoc:5,globi:occur:source:3,/eats,globi:occur:target:6,data source description,globi:ref:1" +
                "\nglobi:assoc:6,globi:occur:source:3,/eats,globi:occur:target:6,data source description,globi:ref:1";


        Study myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        new ExporterAssociations().exportStudy(myStudy1, row, false);

        assertThat(row.getBuffer().toString(), equalTo(expected));
    }

    private void createTestData(Double length) throws NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.getOrCreateStudy("myStudy", "contributor", "institute", "period", "description", "pubYear", "data source description");
        Specimen specimen = nodeFactory.createSpecimen("Homo sapiens", "EOL:123");
        specimen.setStomachVolumeInMilliLiter(666.0);
        specimen.setLifeStage(new EnvoTerm("GLOBI:JUVENILE", "JUVENILE"));
        specimen.setPhysiologicalState(new EnvoTerm("GLOBI:DIGESTATE", "DIGESTATE"));
        specimen.setBodyPart(new EnvoTerm("GLOBI:BONE", "BONE"));
        Relationship collected = myStudy.collected(specimen);
        Transaction transaction = myStudy.getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            collected.setProperty(Specimen.DATE_IN_UNIX_EPOCH, ExportTestUtil.utcTestTime());
            transaction.success();
        } finally {
            transaction.finish();
        }
        eats(specimen, "Canis lupus", "EOL:456");
        eats(specimen, "Felis whateverus", PropertyAndValueDictionary.NO_MATCH);
        if (null != length) {
            specimen.setLengthInMm(length);
        }

        Location location = nodeFactory.getOrCreateLocation(123.0, 345.9, -60.0);
        specimen.caughtIn(location);
    }

    private void eats(Specimen specimen, String scientificName, String taxonExternalId) throws NodeFactoryException {
        Specimen otherSpecimen = nodeFactory.createSpecimen(scientificName, taxonExternalId);
        otherSpecimen.setVolumeInMilliLiter(124.0);

        specimen.ate(otherSpecimen);
        specimen.ate(otherSpecimen);
    }


    @Test
    public void darwinCoreMetaTable() throws IOException {
        ExporterAssociations exporter = new ExporterAssociations();
        StringWriter writer = new StringWriter();
        exporter.exportDarwinCoreMetaTable(writer, "testtest.csv");

        assertThat(writer.toString(), is(exporter.getMetaTablePrefix() + "testtest.csv" + exporter.getMetaTableSuffix()));
    }

}
