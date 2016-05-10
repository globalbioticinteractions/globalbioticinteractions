package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ExportNCBIResourceFileTest extends GraphDBTestCase {

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

        taxon = new TaxonImpl("Homo sapiens", "foo:123");
        taxon.setPath("some path");
        taxonIndex.getOrCreateTaxon(taxon);

        taxon = new TaxonImpl("Enhydra lutris", "NCBI:34882");
        taxon.setPath("some path");
        taxonIndex.getOrCreateTaxon(taxon);

        StringWriter writer = new StringWriter();
        new ExportNCBIResourceFile().exportStudy(study, writer, true);
        assertThat(writer.toString(), is("<?xml version=\"1.0\"?>\n" +
                "<!DOCTYPE LinkSet PUBLIC \"-//NLM//DTD LinkOut 1.0//EN\"\n" +
                "\"http://www.ncbi.nlm.nih.gov/projects/linkout/doc/LinkOut.dtd\"\n" +
                "[<!ENTITY base.url \"http://www.globalbioticinteractions.org?\">]>\n" +
                "<LinkSet>\n" +
                " <Link>\n" +
                "   <LinkId>NCBI:9606</LinkId>\n" +
                "   <ProviderId>7777</ProviderId>\n" +
                "   <ObjectSelector>\n" +
                "     <Database>Taxonomy</Database>\n" +
                "     <ObjectList>\n" +
                "         <ObjId>9606</ObjId>\n" +
                "      </ObjectList>\n" +
                "   </ObjectSelector>\n" +
                "   <ObjectUrl>\n" +
                "      <Base>&base.url;</Base>\n" +
                "      <Rule>sourceTaxon=NCBI:&lo.id;</Rule>\n" +
                "   </ObjectUrl>\n" +
                " </Link>\n" +
                " <Link>\n" +
                "   <LinkId>NCBI:34882</LinkId>\n" +
                "   <ProviderId>7777</ProviderId>\n" +
                "   <ObjectSelector>\n" +
                "     <Database>Taxonomy</Database>\n" +
                "     <ObjectList>\n" +
                "         <ObjId>34882</ObjId>\n" +
                "      </ObjectList>\n" +
                "   </ObjectSelector>\n" +
                "   <ObjectUrl>\n" +
                "      <Base>&base.url;</Base>\n" +
                "      <Rule>sourceTaxon=NCBI:&lo.id;</Rule>\n" +
                "   </ObjectUrl>\n" +
                " </Link>\n" +
                "</LinkSet>"));
    }

}