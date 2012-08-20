package org.trophic.graph.data;

import org.junit.Test;
import org.trophic.graph.domain.Taxon;
import org.trophic.graph.obo.OboParser;
import org.trophic.graph.obo.OboTerm;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class OboImporterTest extends GraphDBTestCase {

    @Test
    public void stringFormat() {
        OboImporter oboImporter = new OboImporter(nodeFactory);
        oboImporter.setCounter(123);
        String s = oboImporter.formatProgressString(12.2);

        assertThat(s, is("123 (0.0%), 12.2 terms/s"));

        oboImporter.setCounter(OboParser.MAX_TERMS);
        s = oboImporter.formatProgressString(12.2);
        assertThat(s, is("798595 (100.0%), 12.2 terms/s"));
    }

    @Test(expected = StudyImporterException.class)
    public void importSpeciesTaxonMissingExternalId() throws IOException, NodeFactoryException, StudyImporterException {

        OboTerm term = new OboTerm();
        term.setName("Blabus blalba");
        term.setRank("species");

        new OboImporter(nodeFactory).importOboTerm(term);

        Taxon species = nodeFactory.findTaxonOfType("Blabus blalba", "species");
        assertThat(species.getName(), is("Blabus blalba"));
        assertThat(species.getType(), is("species"));
    }

    @Test
    public void importSpeciesTaxon() throws IOException, NodeFactoryException, StudyImporterException {

        OboTerm term = new OboTerm();
        term.setName("Blabus blalba");
        term.setRank("species");
        term.setId("id123");

        new OboImporter(nodeFactory).importOboTerm(term);

        Taxon species = nodeFactory.findTaxonOfType("Blabus blalba", "species");
        assertThat(species.getName(), is("Blabus blalba"));
        assertThat(species.getType(), is("species"));
    }
}
