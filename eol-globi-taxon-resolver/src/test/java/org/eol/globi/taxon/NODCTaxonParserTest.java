package org.eol.globi.taxon;

import org.junit.Test;
import org.mapdb.Fun;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class NODCTaxonParserTest {

    @Test
    public void readAllLines() throws IOException {
        final List<Fun.Tuple2<String, String>> terms = new ArrayList<Fun.Tuple2<String, String>>();

        NODCTaxonParser taxonParser = getTestParser();
        while (taxonParser.hasNext()) {
            terms.add(taxonParser.next());
        }

        assertThat(terms.size(), is(6));
        assertThat(terms.get(0).a, is("NODC:01"));
        assertThat(terms.get(0).b, is("ITIS:202419"));
        assertThat(terms.get(1).a, is("NODC:0101"));
        assertThat(terms.get(1).b, is("ITIS:1"));
        assertThat(terms.get(5).a, is("NODC:9227040101"));
        assertThat(terms.get(5).b, is("ITIS:180725"));
    }

    static NODCTaxonParser getTestParser() throws IOException {
        final String testResource = "/org/eol/globi/taxon/nodc/test-taxbrief.dat";
        final BufferedReader reader = new BufferedReader(new InputStreamReader(NODCTaxonParserTest.class.getResourceAsStream(testResource)));
        return new NODCTaxonParser(reader);
    }


}