package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ExportTaxonMapsTest extends GraphDBTestCase {

    @Test
    public void exportOnePredatorTwoPrey() throws NodeFactoryException, IOException {
        final PropertyEnricher taxonEnricher = new PropertyEnricher() {
            @Override
            public Map<String, String> enrich(Map<String, String> properties) {
                Taxon taxon = new TaxonImpl();
                TaxonUtil.mapToTaxon(properties, taxon);
                if ("Homo sapiens".equals(taxon.getName())) {
                    taxon.setExternalId("homoSapiensId");
                    taxon.setPath("one two three");
                } else if ("Canis lupus".equals(taxon.getName())) {
                    taxon.setExternalId("canisLupusId");
                    taxon.setPath("four five six");
                }
                return TaxonUtil.taxonToMap(taxon);
            }

            @Override
            public void shutdown() {

            }
        };
        taxonIndex = ExportTestUtil.taxonIndexWithEnricher(taxonEnricher, getGraphDb());
        Study study = nodeFactory.getOrCreateStudy("title", "source", "citation");
        Taxon taxon = new TaxonImpl("Homo sapiens");
        taxon.setExternalUrl("http://some/thing");
        taxon.setThumbnailUrl("http://thing/some");
        nodeFactory.createSpecimen(study, taxon);
        TaxonNode human = taxonIndex.getOrCreateTaxon(taxon);
        nodeFactory.createSpecimen(study, new TaxonImpl("Canis lupus"));
        NodeUtil.connectTaxa(new TaxonImpl("Alternate Homo sapiens", "alt:123"), human, getGraphDb(), RelTypes.SAME_AS);
        NodeUtil.connectTaxa(new TaxonImpl("Similar Homo sapiens", "alt:456"), human, getGraphDb(), RelTypes.SIMILAR_TO);
        resolveNames();

        StringWriter writer = new StringWriter();
        new ExportTaxonMaps().exportStudy(study, writer, true);
        assertThat(writer.toString(), is("providedTaxonId,providedTaxonName,resolvedId,resolvedTaxonName" +
                "\n\"\",Homo sapiens,homoSapiensId,Homo sapiens" +
                "\n\"\",Homo sapiens,alt:123,Alternate Homo sapiens" +
                "\n\"\",Canis lupus,canisLupusId,Canis lupus"));
    }

}