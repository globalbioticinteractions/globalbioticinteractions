package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
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

public class ExportTaxonCacheTest extends GraphDBTestCase {

    @Test
    public void exportOnePredatorTwoPrey() throws NodeFactoryException, IOException {
        taxonIndex = ExportTestUtil.taxonIndexWithEnricher(null, getGraphDb());
        Study study = nodeFactory.getOrCreateStudy(new StudyImpl("title", "source", null, "citation"));
        Taxon taxon = new TaxonImpl("Homo sapiens");
        taxon.setExternalId("homoSapiensId");
        taxon.setPath("one\ttwo three");
        taxon.setExternalUrl("http://some/thing");
        taxon.setCommonNames("man @en | \"mens @nl");
        taxon.setThumbnailUrl("http://thing/some");
        Taxon human = taxonIndex.getOrCreateTaxon(taxon);
        TaxonImpl taxon1 = new TaxonImpl("Canis lupus", "canisLupusId");
        taxon1.setPath("four five six");
        taxonIndex.getOrCreateTaxon(taxon1);
        NodeUtil.connectTaxa(new TaxonImpl("Alternate Homo sapiens no path", "alt:123"), (TaxonNode)human, getGraphDb(), RelTypes.SAME_AS);
        final TaxonImpl altTaxonWithPath = new TaxonImpl("Alternate Homo sapiens", "alt:123");
        altTaxonWithPath.setPath("some path here");
        NodeUtil.connectTaxa(altTaxonWithPath, (TaxonNode)human, getGraphDb(), RelTypes.SAME_AS);
        NodeUtil.connectTaxa(new TaxonImpl("Similar Homo sapiens", "alt:456"), (TaxonNode)human, getGraphDb(), RelTypes.SIMILAR_TO);

        StringWriter writer = new StringWriter();
        new ExportTaxonCache().exportStudy(study, writer, true);
        assertThat(writer.toString(), is("id\tname\trank\tcommonNames\tpath\tpathIds\tpathNames\texternalUrl\tthumbnailUrl" +
                "\nhomoSapiensId\tHomo sapiens\t\tman @en | \"mens @nl\tone two three\t\t\thttp://some/thing\thttp://thing/some" +
                "\nalt:123\tAlternate Homo sapiens\t\t\tsome path here\t\t\thttp://some/thing\thttp://thing/some" +
                "\ncanisLupusId\tCanis lupus\t\t\tfour five six\t\t\t\t"));
    }



}