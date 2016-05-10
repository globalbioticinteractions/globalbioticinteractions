package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricher;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ExportNCBIIdentityFileTest extends GraphDBTestCase {

    @Test
    public void exportOnePredatorTwoPrey() throws NodeFactoryException, IOException {
        final PropertyEnricher taxonEnricher = new PropertyEnricher() {
            @Override
            public Map<String, String> enrich(Map<String, String> properties) {
                return properties;
            }

            @Override
            public void shutdown() {

            }
        };
        taxonIndex = ExportTestUtil.taxonIndexWithEnricher(taxonEnricher, getGraphDb());
        Study study = nodeFactory.getOrCreateStudy("title", "source", "citation");
        Taxon taxon = new TaxonImpl("Homo sapiens", TaxonomyProvider.NCBI.getIdPrefix() + "9606");
        taxon.setPath("some path");
        taxonIndex.getOrCreateTaxon(taxon);

        StringWriter writer = new StringWriter();
        new ExportNCBIIdentityFile().exportStudy(study, writer, true);
        assertThat(writer.toString(), is("<?xml version=\"1.0\"?>\n" +
                "<!DOCTYPE Provider PUBLIC \"-//NLM//DTD LinkOut 1.0//EN\"\n" +
                "\"http://www.ncbi.nlm.nih.gov/projects/linkout/doc/LinkOut.dtd\">\n" +
                "<Provider>\n" +
                "    <ProviderId>7777</ProviderId>\n" +
                "    <Name>Global Biotic Interactions</Name>\n" +
                "    <NameAbbr>GloBI</NameAbbr>\n" +
                "    <SubjectType>taxonomy/phylogenetic</SubjectType>\n" +
                "    <Url>http://www.globalbioticinteractions.org</Url>\n" +
                "    <Brief>helps access existing species interaction datasets</Brief>\n" +
                "</Provider>\n"));
    }

}