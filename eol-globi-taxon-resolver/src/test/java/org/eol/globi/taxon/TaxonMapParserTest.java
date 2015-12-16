package org.eol.globi.taxon;

import org.eol.globi.domain.Taxon;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class TaxonMapParserTest {

    @Test
    public void readThreeLine() throws IOException {
        BufferedReader someLines = new BufferedReader(new StringReader(
                "providedTaxonId,providedTaxonName,resolvedTaxonId,resolvedTaxonName\n" +
                        "EOL:2888546,Toddalia asiatica,EOL:2888546,Toddalia asiatica\n" +
                        "EOL:2888546,Toddalia asiatica,OTT:232693,Toddalia asiatica\n" +
                        "EOL:1049789,Turtur tympanistria,EOL:1049789,Turtur tympanistria"));

        assertThat(someLines, is(notNullValue()));


        TaxonMapParser taxonParser = new TaxonMapParser();
        final List<String> taxa = new ArrayList<String>();
        taxonParser.parse(someLines, new TaxonMapListener() {
            @Override
            public void addMapping(Taxon srcTaxon, Taxon targetTaxon) {
                taxa.add(srcTaxon.getExternalId() + srcTaxon.getName() + "-->" + targetTaxon.getExternalId() + targetTaxon.getName());
            }

            @Override
            public void start() {

            }

            @Override
            public void finish() {

            }
        });

        assertThat(taxa.size(), is(3));

        assertThat(taxa.get(0), is("EOL:2888546Toddalia asiatica-->EOL:2888546Toddalia asiatica"));
        assertThat(taxa.get(1), is("EOL:2888546Toddalia asiatica-->OTT:232693Toddalia asiatica"));
        assertThat(taxa.get(2), is("EOL:1049789Turtur tympanistria-->EOL:1049789Turtur tympanistria"));

    }

}