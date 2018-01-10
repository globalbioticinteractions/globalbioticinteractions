package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricher;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class ExportNCBIResourceFileTest extends GraphDBTestCase {

    @Test
    public void exportTwoLinksTwoFiles() throws StudyImporterException, IOException {
        final Map<Integer, ByteArrayOutputStream> osMap = new HashMap<Integer, ByteArrayOutputStream>();
        final ExportNCBIResourceFile exportNCBIResourceFile = new ExportNCBIResourceFile();
        exportNCBIResourceFile.setLinksPerResourceFile(1);
        exportNCBIResourceFile.export(getGraphDb(), new ExportNCBIResourceFile.OutputStreamFactory() {

            @Override
            public OutputStream create(int i) throws IOException {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                osMap.put(i, baos);
                return baos;
            }
        });

        assertThat(osMap.keySet(), hasItem(0));
        assertThat(osMap.keySet(), hasItem(1));

        assertThat(osMap.get(0).toString(), is("<?xml version=\"1.0\"?>\n" +
                "<!DOCTYPE LinkSet PUBLIC \"-//NLM//DTD LinkOut 1.0//EN\"\n" +
                "\"http://www.ncbi.nlm.nih.gov/projects/linkout/doc/LinkOut.dtd\"\n" +
                "[<!ENTITY base.url \"http://www.globalbioticinteractions.org?\">]>\n" +
                "<LinkSet>\n" +
                " <Link>\n" +
                "   <LinkId>0</LinkId>\n" +
                "   <ProviderId>9426</ProviderId>\n" +
                "   <ObjectSelector>\n" +
                "     <Database>Taxonomy</Database>\n" +
                "     <ObjectList>\n" +
                "         <ObjId>9606</ObjId>\n" +
                "      </ObjectList>\n" +
                "   </ObjectSelector>\n" +
                "   <ObjectUrl>\n" +
                "      <Base>&base.url;</Base>\n" +
                "      <Rule>sourceTaxon=NCBI:&lo.id;</Rule>\n" +
                "      <UrlName>Show Biotic Interactions</UrlName>\n" +
                "   </ObjectUrl>\n" +
                " </Link>\n" +
                "</LinkSet>"));

        assertThat(osMap.get(1).toString(), is("<?xml version=\"1.0\"?>\n" +
                "<!DOCTYPE LinkSet PUBLIC \"-//NLM//DTD LinkOut 1.0//EN\"\n" +
                "\"http://www.ncbi.nlm.nih.gov/projects/linkout/doc/LinkOut.dtd\"\n" +
                "[<!ENTITY base.url \"http://www.globalbioticinteractions.org?\">]>\n" +
                "<LinkSet>\n" +
                " <Link>\n" +
                "   <LinkId>1</LinkId>\n" +
                "   <ProviderId>9426</ProviderId>\n" +
                "   <ObjectSelector>\n" +
                "     <Database>Taxonomy</Database>\n" +
                "     <ObjectList>\n" +
                "         <ObjId>34882</ObjId>\n" +
                "      </ObjectList>\n" +
                "   </ObjectSelector>\n" +
                "   <ObjectUrl>\n" +
                "      <Base>&base.url;</Base>\n" +
                "      <Rule>sourceTaxon=NCBI:&lo.id;</Rule>\n" +
                "      <UrlName>Show Biotic Interactions</UrlName>\n" +
                "   </ObjectUrl>\n" +
                " </Link>\n" +
                "</LinkSet>"));
    }

    @Test
    public void exportTwoLinksOneFiles() throws StudyImporterException, IOException {
        final Map<Integer, ByteArrayOutputStream> osMap = new HashMap<Integer, ByteArrayOutputStream>();
        final ExportNCBIResourceFile exportNCBIResourceFile = new ExportNCBIResourceFile();
        exportNCBIResourceFile.setLinksPerResourceFile(10);
        exportNCBIResourceFile.export(getGraphDb(), new ExportNCBIResourceFile.OutputStreamFactory() {

            @Override
            public OutputStream create(int i) throws IOException {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                osMap.put(i, baos);
                return baos;
            }
        });

        assertThat(osMap.keySet(), hasItem(0));
        assertThat(osMap.keySet(), not(hasItem(1)));

        assertThat(osMap.get(0).toString(), is("<?xml version=\"1.0\"?>\n" +
                "<!DOCTYPE LinkSet PUBLIC \"-//NLM//DTD LinkOut 1.0//EN\"\n" +
                "\"http://www.ncbi.nlm.nih.gov/projects/linkout/doc/LinkOut.dtd\"\n" +
                "[<!ENTITY base.url \"http://www.globalbioticinteractions.org?\">]>\n" +
                "<LinkSet>\n" +
                " <Link>\n" +
                "   <LinkId>0</LinkId>\n" +
                "   <ProviderId>9426</ProviderId>\n" +
                "   <ObjectSelector>\n" +
                "     <Database>Taxonomy</Database>\n" +
                "     <ObjectList>\n" +
                "         <ObjId>9606</ObjId>\n" +
                "         <ObjId>34882</ObjId>\n" +
                "      </ObjectList>\n" +
                "   </ObjectSelector>\n" +
                "   <ObjectUrl>\n" +
                "      <Base>&base.url;</Base>\n" +
                "      <Rule>sourceTaxon=NCBI:&lo.id;</Rule>\n" +
                "      <UrlName>Show Biotic Interactions</UrlName>\n" +
                "   </ObjectUrl>\n" +
                " </Link>\n" +
                "</LinkSet>"));
    }

    @Before
    public void init() throws NodeFactoryException {
        taxonIndex = ExportTestUtil.taxonIndexWithEnricher(null, getGraphDb());
        nodeFactory.getOrCreateStudy(new StudyImpl("title", "source", null, "citation"));
        Taxon taxon = new TaxonImpl("Homo sapiens", TaxonomyProvider.NCBI.getIdPrefix() + "9606");
        taxon.setPath("some path");
        taxonIndex.getOrCreateTaxon(taxon);

        taxon = new TaxonImpl("Homo sapiens", "foo:123");
        taxon.setPath("some path");
        taxonIndex.getOrCreateTaxon(taxon);

        taxon = new TaxonImpl("Enhydra lutris", "NCBI:34882");
        taxon.setPath("some path");
        taxonIndex.getOrCreateTaxon(taxon);
    }

}