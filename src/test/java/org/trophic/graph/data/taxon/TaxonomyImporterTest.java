package org.trophic.graph.data.taxon;

import org.junit.Test;
import org.trophic.graph.data.GraphDBTestCase;
import org.trophic.graph.data.NodeFactoryException;
import org.trophic.graph.data.StudyImporterException;
import org.trophic.graph.domain.Taxon;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TaxonomyImporterTest extends GraphDBTestCase {

    @Test
    public void stringFormat() {
        TaxonomyImporter taxonomyImporter = new TaxonomyImporter(nodeFactory);
        taxonomyImporter.setCounter(123);
        String s = taxonomyImporter.formatProgressString(12.2);

        assertThat(s, is("123 (0.0%), 12.2 terms/s"));

        taxonomyImporter.setCounter(taxonomyImporter.getParser().getExpectedMaxTerms());
        s = taxonomyImporter.formatProgressString(12.2);
        assertThat(s, is("798595 (100.0%), 12.2 terms/s"));
    }

    @Test(expected = StudyImporterException.class)
    public void importSpeciesTaxonMissingExternalId() throws IOException, NodeFactoryException, StudyImporterException {

        TaxonTerm term = new TaxonTerm();
        term.setName("Blabus blalba");
        term.setRank("species");

        new TaxonomyImporter(nodeFactory).importTaxonTerm(term);

        Taxon species = nodeFactory.findTaxonOfType("Blabus blalba");
        assertThat(species.getName(), is("Blabus blalba"));
    }

    @Test
    public void importSpeciesTaxon() throws IOException, NodeFactoryException, StudyImporterException {

        TaxonTerm term = new TaxonTerm();
        term.setName("Blabus blalba");
        term.setRank("species");
        term.setId("id123");

        new TaxonomyImporter(nodeFactory).importTaxonTerm(term);

        Taxon species = nodeFactory.findTaxonOfType("Blabus blalba");
        assertThat(species.getName(), is("Blabus blalba"));
    }
}
