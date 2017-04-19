package org.eol.globi.tool;

import org.apache.commons.io.IOUtils;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.NodeFactoryWithDatasetContext;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.domain.DatasetNode;
import org.eol.globi.domain.Interaction;
import org.eol.globi.domain.InteractionNode;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.DatasetImpl;
import org.junit.Test;
import org.openrdf.model.Model;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.rio.trig.TriGParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class LinkerTrustyNanoPubsTest extends GraphDBTestCase {

    @Test
    public void linking() throws NodeFactoryException, RDFHandlerException, IOException, RDFParseException {

        TaxonIndex taxonIndex = getOrCreateTaxonIndex();
        // see https://github.com/jhpoelen/eol-globi-data/wiki/Nanopubs
        StudyImpl study = new StudyImpl("some study", "some source", "http://doi.org/123.23/222", "some study citation");
        DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create("http://example.com/dataset"));
        NodeFactoryWithDatasetContext factory = new NodeFactoryWithDatasetContext(nodeFactory, dataset);
        Interaction interaction = factory.createInteraction(factory.createStudy(study));
        TaxonImpl donaldTaxon = new TaxonImpl("donald duck", "NCBI:1234");
        Specimen donald = factory.createSpecimen(interaction, donaldTaxon);
        donald.classifyAs(taxonIndex.getOrCreateTaxon(donaldTaxon));
        TaxonImpl mickeyTaxon = new TaxonImpl("mickey mouse", "NCBI:4444");
        Specimen mickey = factory.createSpecimen(interaction, mickeyTaxon);
        mickey.classifyAs(taxonIndex.getOrCreateTaxon(mickeyTaxon));

        donald.ate(mickey);

        LinkerTrustyNanoPubs linker = new LinkerTrustyNanoPubs();
        linker.link(getGraphDb());

        String nanoPubText = linker.writeNanoPub((DatasetNode)factory.getOrCreateDataset(dataset), new InteractionNode(((InteractionNode)interaction).getUnderlyingNode()));
        InputStream rdfIn = IOUtils.toInputStream(nanoPubText);

        String rdfActual = toTrigString(rdfIn);
        String rdfExpected = toTrigString(getClass().getResourceAsStream("nanopub.trig"));

        assertThat(rdfActual, is(rdfExpected));
    }

    private String toTrigString(InputStream rdfIn) throws IOException, RDFParseException, RDFHandlerException {
        TriGParser rdfParser = new TriGParser();
        Model model = new LinkedHashModel();
        rdfParser.setRDFHandler(new StatementCollector(model));
        rdfParser.parse(rdfIn, "http://example.com");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Rio.write(model, out, RDFFormat.TRIG);
        return new String(out.toByteArray(), "UTF-8");
    }

}
