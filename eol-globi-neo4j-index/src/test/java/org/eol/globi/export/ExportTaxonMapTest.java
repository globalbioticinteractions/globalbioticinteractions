package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherSingle;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
public class ExportTaxonMapTest extends GraphDBTestCase {

    @Test
    public void exportOnePredatorTwoPrey() throws NodeFactoryException, IOException {
        final PropertyEnricher taxonEnricher = new PropertyEnricherSingle() {
            @Override
            public Map<String, String> enrichFirstMatch(Map<String, String> properties) {
                Taxon taxon = new TaxonImpl();
                TaxonUtil.mapToTaxon(properties, taxon);
                if ("Homo sapiens".equals(taxon.getName())) {
                    taxon.setExternalId("homoSapiensId");
                    taxon.setPath("one two three");
                } else if ("Canis lupus".equals(taxon.getName())) {
                    taxon.setExternalId("canisLupusId");
                    taxon.setPath("four\tfive six");
                }
                return TaxonUtil.taxonToMap(taxon);
            }

            @Override
            public void shutdown() {

            }
        };
        taxonIndex = ExportTestUtil.taxonIndexWithEnricher(taxonEnricher, getGraphDb());
        StudyNode study = (StudyNode) nodeFactory.getOrCreateStudy(new StudyImpl("title", null, "citation"));
        Taxon taxon = new TaxonImpl("Homo sapiens");
        taxon.setExternalId("homoSapiensId");
        taxon.setPath("one two three");
        taxon.setExternalUrl("http://some/thing");
        taxon.setThumbnailUrl("http://thing/some");
        nodeFactory.createSpecimen(study, taxon);
        Taxon human = taxonIndex.getOrCreateTaxon(taxon);
        TaxonImpl dog = new TaxonImpl("Canis lupus");
        dog.setExternalId("canisLupusId");
        dog.setPath("four\tfive six");

        nodeFactory.createSpecimen(study, dog);
        final TaxonImpl altTaxonWithPath = new TaxonImpl("Alternate Homo sapiens", "alt:123");
        altTaxonWithPath.setPath("some path here");
        NodeUtil.connectTaxa(altTaxonWithPath, (TaxonNode)human, getGraphDb(), RelTypes.SAME_AS);
        NodeUtil.connectTaxa(new TaxonImpl("Alternate Homo sapiens no path", "alt:123"), (TaxonNode)human, getGraphDb(), RelTypes.SAME_AS);
        NodeUtil.connectTaxa(new TaxonImpl("Similar Homo sapiens", "alt:456"), (TaxonNode)human, getGraphDb(), RelTypes.SIMILAR_TO);
        resolveNames();

        StringWriter writer = new StringWriter();
        new ExportTaxonMap().exportStudy(study, ExportUtil.AppenderWriter.of(writer), true);
        String actual = writer.toString();
        assertThat(actual, startsWith("providedTaxonId\tprovidedTaxonName\tresolvedTaxonId\tresolvedTaxonName"));
        assertThat(actual, containsString("\nhomoSapiensId\tHomo sapiens\thomoSapiensId\tHomo sapiens"));
        assertThat(actual, containsString("\nhomoSapiensId\tHomo sapiens\talt:123\tAlternate Homo sapiens"));
        assertThat(actual, containsString("\ncanisLupusId\tCanis lupus\tcanisLupusId\tCanis lupus\n"));
    }

}