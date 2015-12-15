package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.NodeFactoryImpl;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ExportTaxonNamesTest extends GraphDBTestCase {

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
        TaxonNode human = taxonIndex.getOrCreateTaxon("Homo sapiens");
        taxonIndex.getOrCreateTaxon("Canis lupus");
        NodeUtil.connectTaxa(new TaxonImpl("Alternate Homo sapiens", "alt:123"), human, getGraphDb(), RelTypes.SAME_AS);
        NodeUtil.connectTaxa(new TaxonImpl("Similar Homo sapiens", "alt:456"), human, getGraphDb(), RelTypes.SIMILAR_TO);

        StringWriter writer = new StringWriter();
        new ExportTaxonNames().exportStudy(study, writer, true);
        assertThat(writer.toString(), is("id,name,rank,commonNames,path,pathIds,pathNames" +
                "\nhomoSapiensId,Homo sapiens,,,one two three,," +
                "\nalt:123,Alternate Homo sapiens,,,,," +
                "\ncanisLupusId,Canis lupus,,,four five six,,"));
    }

}