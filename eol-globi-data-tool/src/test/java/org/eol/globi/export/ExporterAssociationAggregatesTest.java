package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryImpl;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.taxon.CorrectionService;
import org.eol.globi.data.taxon.TaxonIndexImpl;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ExporterAssociationAggregatesTest extends GraphDBTestCase {

    @Before
    public void setEnricher() {
        final PropertyEnricher taxonEnricher = new PropertyEnricher() {

            @Override
            public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
                Taxon taxon = new TaxonImpl();
                TaxonUtil.mapToTaxon(properties, taxon);
                taxon.setExternalId(taxon.getName() + "id");
                taxon.setPath(taxon.getName() + "path");
                return TaxonUtil.taxonToMap(taxon);
            }

            @Override
            public void shutdown() {

            }
        };
        nodeFactory = new NodeFactoryImpl(getGraphDb(), new TaxonIndexImpl(taxonEnricher, new CorrectionService() {

            @Override
            public String correct(String taxonName) {
                return taxonName;
            }
        }, getGraphDb()));
    }

    @Test
    public void exportCSVNoHeader() throws IOException, NodeFactoryException, ParseException {
        String[] studyTitles = {"myStudy1", "myStudy2"};

        for (String studyTitle : studyTitles) {
            createTestData(null, studyTitle);
        }

        nodeFactory.findStudy("myStudy1").setExternalId("some:id");

        ExporterAssociationAggregates exporter = new ExporterAssociationAggregates();
        StringWriter row = new StringWriter();
        for (String studyTitle : studyTitles) {
            Study myStudy1 = nodeFactory.findStudy(studyTitle);
            exporter.exportStudy(myStudy1, row, false);
        }

        String expected = "\nglobi:assoc:1-2-ATE-5,globi:occur:source:1-2-ATE,http://purl.obolibrary.org/obo/RO_0002470,globi:occur:target:1-2-ATE-5,,,,,data source description,,,globi:ref:1" +
                        "\nglobi:assoc:9-2-ATE-5,globi:occur:source:9-2-ATE,http://purl.obolibrary.org/obo/RO_0002470,globi:occur:target:9-2-ATE-5,,,,,data source description,,,globi:ref:9";
        assertThat(row.getBuffer().toString(), equalTo(expected));
    }

    @Test
    public void exportNoMatchTaxa() throws IOException, NodeFactoryException, ParseException {
        String[] studyTitles = {"myStudy1", "myStudy2"};

        for (String studyTitle : studyTitles) {
            Study myStudy = nodeFactory.getOrCreateStudy(studyTitle, "data source description", ExternalIdUtil.toCitation("contributor", "description", "pubYear"));
            Specimen specimen = nodeFactory.createSpecimen(myStudy, PropertyAndValueDictionary.NO_MATCH);
            specimen.ate(nodeFactory.createSpecimen(myStudy, PropertyAndValueDictionary.NO_MATCH));
        }

        ExporterAssociationAggregates exporter = new ExporterAssociationAggregates();
        StringWriter row = new StringWriter();
        for (String studyTitle : studyTitles) {
            Study myStudy1 = nodeFactory.findStudy(studyTitle);
            exporter.exportStudy(myStudy1, row, false);
        }


        assertThat(row.getBuffer().toString(), equalTo(""));
    }

    private void createTestData(Double length, String studyTitle) throws NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.getOrCreateStudy(studyTitle, "data source description", ExternalIdUtil.toCitation("contributor", "description", "pubYear"));
        Specimen specimen = nodeFactory.createSpecimen(myStudy, "Homo sapiens");
        specimen.setStomachVolumeInMilliLiter(666.0);
        specimen.setLifeStage(new Term("GlOBI:JUVENILE", "JUVENILE"));
        specimen.setPhysiologicalState(new Term("GlOBI:DIGESTATE", "DIGESTATE"));
        specimen.setBodyPart(new Term("GLOBI:BONE", "BONE"));
        nodeFactory.setUnixEpochProperty(specimen, new Date(ExportTestUtil.utcTestTime()));
        Specimen otherSpecimen = nodeFactory.createSpecimen(myStudy, "Canis lupus");
        otherSpecimen.setVolumeInMilliLiter(124.0);

        specimen.ate(otherSpecimen);
        specimen.ate(otherSpecimen);
        if (null != length) {
            specimen.setLengthInMm(length);
        }

        Location location = nodeFactory.getOrCreateLocation(44.0, 120.0, -60.0);
        specimen.caughtIn(location);
    }

}
